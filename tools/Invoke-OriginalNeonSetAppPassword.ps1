param(
    [string]$PsqlPath = 'C:\Program Files\PostgreSQL\17\bin\psql.exe',
    [string]$SslRootCert = 'C:\Program Files\Git\mingw64\etc\ssl\certs\ca-bundle.crt',
    [Security.SecureString]$OwnerCredential = $null,
    [Security.SecureString]$AppCredential = $null
)
$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $PSScriptRoot

if (-not (Test-Path -LiteralPath $PsqlPath)) { throw 'psql executable unavailable' }
if (-not (Test-Path -LiteralPath $SslRootCert)) { throw 'CA root bundle unavailable for verify-full' }

$logDir = Join-Path $appRoot '.superpowers\sdd\task5-local-integration'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$logFile = Join-Path $logDir 'neon-original-set-password.log'

function ConvertFrom-SecureToPlain {
    param([Security.SecureString]$SecureValue)
    if ($null -eq $SecureValue) { return '' }
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    } finally {
        if ($bstr -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
        }
    }
}

$ownsOwnerCredential = ($null -eq $OwnerCredential)
$ownsAppCredential = ($null -eq $AppCredential)
$plainOwnerPassword = $null
$plainAppPassword = $null
$escapedAppPassword = $null
$sqlStdin = $null
$proc = $null
$start = $null

try {
    if ($ownsOwnerCredential) {
        $OwnerCredential = Read-Host 'Existing password of neondb_owner (hidden; used only in memory)' -AsSecureString
    }
    if ($ownsAppCredential) {
        $AppCredential = Read-Host 'First password for emr_financas_app (hidden; sent only via stdin over TLS verify-full)' -AsSecureString
    }

    $plainOwnerPassword = ConvertFrom-SecureToPlain -SecureValue $OwnerCredential
    $plainAppPassword = ConvertFrom-SecureToPlain -SecureValue $AppCredential

    if ([string]::IsNullOrWhiteSpace($plainOwnerPassword)) {
        throw 'neondb_owner password must not be empty'
    }
    if ([string]::IsNullOrWhiteSpace($plainAppPassword) -or $plainAppPassword.Contains([char]0)) {
        throw 'emr_financas_app password must not be empty or contain NUL characters'
    }

    $escapedAppPassword = $plainAppPassword.Replace("'", "''")
    $hostName = 'ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech'

    $sqlStdin = @"
\set ON_ERROR_STOP on
\set VERBOSITY terse
\conninfo
BEGIN READ ONLY;
SET LOCAL lock_timeout='3s';
SET LOCAL statement_timeout='15s';
DO `$`$
BEGIN
  IF current_database() IS DISTINCT FROM 'neondb'
     OR current_user IS DISTINCT FROM 'neondb_owner'
     OR session_user IS DISTINCT FROM current_user
     OR current_setting('server_version_num')::int NOT BETWEEN 160000 AND 169999
     OR NOT EXISTS (SELECT 1 FROM pg_stat_ssl WHERE pid=pg_backend_pid()) THEN
    RAISE EXCEPTION 'Target/identity/version rejected before ALTER ROLE';
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM pg_roles
    WHERE rolname='emr_financas_app' AND rolcanlogin AND NOT rolinherit
      AND NOT (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
      AND rolconnlimit=3
  ) THEN
    RAISE EXCEPTION 'emr_financas_app missing or unexpected role attributes';
  END IF;
  IF NOT (has_database_privilege('emr_financas_app','neondb','CONNECT')
      AND NOT has_database_privilege('emr_financas_app','neondb','CREATE')
      AND NOT has_database_privilege('emr_financas_app','neondb','TEMP')) THEN
    RAISE EXCEPTION 'Database privileges precondition not satisfied';
  END IF;
END `$`$;
ROLLBACK;
SET standard_conforming_strings = on;
ALTER ROLE emr_financas_app WITH PASSWORD '$escapedAppPassword';
SELECT 'PASSWORD_SET_OK:emr_financas_app' AS password_status
WHERE EXISTS (
  SELECT 1 FROM pg_roles
  WHERE rolname='emr_financas_app' AND rolcanlogin AND NOT rolinherit
    AND NOT (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
    AND rolconnlimit=3
);
"@

    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $PsqlPath
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardInput = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.StandardOutputEncoding = [Text.Encoding]::UTF8
    $start.StandardErrorEncoding = [Text.Encoding]::UTF8
    $start.Arguments = '-X -w -h ' + $hostName + ' -p 5432 -d neondb -U neondb_owner -v ON_ERROR_STOP=1 -v HISTFILE=none -f -'
    $start.EnvironmentVariables.Clear()
    foreach ($key in @('SystemRoot','WINDIR','TEMP','TMP','USERPROFILE')) {
        $val = [Environment]::GetEnvironmentVariable($key, 'Process')
        if ($val) { $start.EnvironmentVariables[$key] = $val }
    }
    $start.EnvironmentVariables['PGSSLMODE'] = 'verify-full'
    $start.EnvironmentVariables['PGSSLROOTCERT'] = $SslRootCert
    $start.EnvironmentVariables['PGCONNECT_TIMEOUT'] = '15'
    $start.EnvironmentVariables['PGPASSWORD'] = $plainOwnerPassword

    $proc = [Diagnostics.Process]::new()
    $proc.StartInfo = $start
    [void]$proc.Start()
    $start.EnvironmentVariables.Remove('PGPASSWORD')

    $stdoutTask = $proc.StandardOutput.ReadToEndAsync()
    $stderrTask = $proc.StandardError.ReadToEndAsync()

    $utf8NoBom = [Text.UTF8Encoding]::new($false)
    $stdinWriter = [IO.StreamWriter]::new($proc.StandardInput.BaseStream, $utf8NoBom)
    try {
        $stdinWriter.Write($sqlStdin)
        $stdinWriter.Flush()
    } finally {
        $sqlStdin = $null
        $stdinWriter.Close()
    }

    if (-not $proc.WaitForExit(60000)) {
        $proc.Kill()
        throw 'psql password step timed out'
    }

    $exitCode = $proc.ExitCode
    $rawStdout = $stdoutTask.GetAwaiter().GetResult()
    $rawStderr = $stderrTask.GetAwaiter().GetResult()

    foreach ($secret in @($plainOwnerPassword, $plainAppPassword, $escapedAppPassword)) {
        if (-not [string]::IsNullOrEmpty($secret)) {
            $rawStdout = $rawStdout.Replace($secret, '[REDACTED]')
            $rawStderr = $rawStderr.Replace($secret, '[REDACTED]')
        }
    }
    $plainOwnerPassword = $null
    $plainAppPassword = $null
    $escapedAppPassword = $null

    $report = @(
        "=== STEP B: set-app-password (exitCode=$exitCode) ===",
        $rawStdout.Trim(),
        $rawStderr.Trim()
    )
    $report | Set-Content -LiteralPath $logFile -Encoding UTF8

    $succeeded = ($exitCode -eq 0) `
        -and ($rawStdout -match '(?m)^ALTER ROLE\r?$') `
        -and ($rawStdout -match 'PASSWORD_SET_OK:emr_financas_app') `
        -and ($rawStderr -notmatch '(?i)(ERROR:|FATAL:)')

    if (-not $succeeded) {
        $failCode = if ($exitCode -ne 0) { $exitCode } else { 2 }
        Write-Output "password_set=FAIL exitCode=$failCode"
        if ($rawStderr.Trim()) { Write-Output $rawStderr.Trim() }
        exit $failCode
    }

    Write-Output "password_set=PASS exitCode=0 role=emr_financas_app status=PASSWORD_SET_OK:emr_financas_app"
    exit 0
} finally {
    $plainOwnerPassword = $null
    $plainAppPassword = $null
    $escapedAppPassword = $null
    $sqlStdin = $null
    if ($start) { $start.EnvironmentVariables.Remove('PGPASSWORD') }
    if ($proc) { $proc.Dispose() }
    if ($ownsOwnerCredential -and $OwnerCredential) { $OwnerCredential.Dispose() }
    if ($ownsAppCredential -and $AppCredential) { $AppCredential.Dispose() }
}
