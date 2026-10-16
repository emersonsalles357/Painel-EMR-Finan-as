param(
    [Parameter(Mandatory=$true)][string]$JavaHome,
    [Parameter(Mandatory=$true)][ValidatePattern('^[A-Fa-f0-9]{64}$')][string]$JarSha256
)
$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $appRoot 'BackEnd\Back End\backend\target\app.jar'
$java = Join-Path $JavaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) { throw 'Java executable unavailable' }
if ((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ne $JarSha256) { throw 'JAR hash mismatch' }
$credential = $null
$process = $null
$start = $null
$started = $false
$exitCode = 1
try {
    $credential = Read-Host 'Existing password of emr_financas_app (hidden; no credential changes)' -AsSecureString
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $java
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.Arguments = '-Dloader.main=br.com.emr.emrfinancas.database.DatabaseRuntimePreflight -cp "' + $jar + '" org.springframework.boot.loader.launch.PropertiesLauncher'
    # No inherited Spring/Flyway/JVM options or alternate JDBC credentials.
    $start.EnvironmentVariables.Clear()
    foreach ($key in @('SystemRoot','WINDIR','TEMP','TMP','USERPROFILE')) {
        $value = [Environment]::GetEnvironmentVariable($key, 'Process')
        if ($value) { $start.EnvironmentVariables[$key] = $value }
    }
    $approvedHost = 'ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech'
    $start.EnvironmentVariables['DB_TARGET_ID'] = 'neon-original'
    $start.EnvironmentVariables['DB_EXPECTED_HOST'] = $approvedHost
    $start.EnvironmentVariables['DB_URL'] = 'jdbc:postgresql://' + $approvedHost + '/neondb'
    $start.EnvironmentVariables['DB_USERNAME'] = 'emr_financas_app'
    $start.EnvironmentVariables['DB_PASSWORD'] = [Net.NetworkCredential]::new('', $credential).Password
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $start
    [void]$process.Start()
    $started = $true
    $start.EnvironmentVariables.Remove('DB_PASSWORD')
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    if (-not $process.WaitForExit(90000)) {
        $process.Kill()
        throw 'Preflight timed out; owned Java process stopped'
    }
    $exitCode = $process.ExitCode
    $output = $stdout.GetAwaiter().GetResult() + "`n" + $stderr.GetAwaiter().GetResult()
    # Only controlled output; withhold dependency messages and connection details.
    foreach ($line in ($output -split "`r?`n")) {
        if ($line -match '^preflight=(PASS authenticated=true tls=verify-full identity=approved privileges=restricted readOnly=true|FAIL type=[A-Za-z0-9]+( SQLState=[A-Z0-9]{5})?)$') {
            Write-Output $line
        }
    }
    if ($exitCode -ne 0) { Write-Output 'Validation did not pass; no migration attempted' }
} finally {
    if ($credential) { $credential.Dispose() }
    if ($start) { $start.EnvironmentVariables.Remove('DB_PASSWORD') }
    if ($process) {
        if ($started -and -not $process.HasExited) { $process.Kill() }
        $process.Dispose()
    }
}
exit $exitCode
