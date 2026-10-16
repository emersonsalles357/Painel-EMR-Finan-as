param([string]$JavaHome = $env:EMR_JAVA_HOME)
$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $PSScriptRoot
if (-not $JavaHome) { $JavaHome = 'C:\Users\Desktop\AppData\Local\Temp\emr-performance-java17\jdk-17.0.20.1+1' }
$java = Join-Path $JavaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) { throw 'Set EMR_JAVA_HOME to the validated Java 17 installation' }
$jar = Join-Path $appRoot 'BackEnd\Back End\backend\target\app.jar'
if ((Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ne '9E89AB5C8EEA7610E042EE739A850721566CD43E6671DEE810EDFB80AF13A7C4') {
    throw 'Validated JAR hash mismatch'
}
$approvedHost = Read-Host 'Direct host confirmed in project emr-task5-20261016 / branch production / database emr_task5'
if ($approvedHost -notmatch '^[a-z0-9.-]+\.neon\.tech$' -or $approvedHost.Contains('-pooler.')) { throw 'Direct Neon hostname required' }
$evidence = Join-Path $appRoot '.superpowers\sdd\task5-neon-preflight'
New-Item -ItemType Directory -Path $evidence -Force | Out-Null
$driver = Join-Path $evidence 'postgresql-42.7.13.jar'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $entry = $zip.GetEntry('BOOT-INF/lib/postgresql-42.7.13.jar')
    [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $driver, $true)
} finally { $zip.Dispose() }
$keys = @('DB_URL','DB_EXPECTED_HOST','DB_USERNAME','DB_PASSWORD','DB_SSL_MODE','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS')
$previous = @{}
foreach ($key in $keys) { $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process') }
$exitCode = 1
try {
    $credential = Read-Host 'Existing password for emr_task5_app (hidden; no reset)' -AsSecureString
    $env:DB_URL = 'jdbc:postgresql://' + $approvedHost + '/emr_task5'
    $env:DB_EXPECTED_HOST = $approvedHost
    $env:DB_USERNAME = 'emr_task5_app'
    $env:DB_PASSWORD = [Net.NetworkCredential]::new('', $credential).Password
    $env:DB_SSL_MODE = 'verify-full'
    foreach ($key in @('JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS')) { [Environment]::SetEnvironmentVariable($key, $null, 'Process') }
    $log = Join-Path $evidence ('preflight-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
    & $java -cp $driver (Join-Path $PSScriptRoot 'NeonPreflight.java') 2>&1 | Tee-Object -FilePath $log
    $exitCode = $LASTEXITCODE
} finally {
    foreach ($key in $keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
    if ($credential) { $credential.Dispose() }
}
exit $exitCode
