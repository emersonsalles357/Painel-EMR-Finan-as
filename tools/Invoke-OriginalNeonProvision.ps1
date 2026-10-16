param(
    [string]$PsqlPath = 'C:\Program Files\PostgreSQL\17\bin\psql.exe',
    [string]$SslRootCert = 'C:\Program Files\Git\mingw64\etc\ssl\certs\ca-bundle.crt',
    [Security.SecureString]$Credential = $null
)
$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $PSScriptRoot
$provisionSql = Join-Path $appRoot 'deploy\neon-original\10-provision-app.sql'
$inspectSql = Join-Path $appRoot 'deploy\neon-original\00-inspect-readonly.sql'
$expectedProvisionHash = '73ABDE6C2B519991870AC794E4EFFA246265D583969AB5649DADFADF347925CF'
$expectedInspectHash = '08F0D802C4547D2ECC43B3CB4010DBF0A69E7E71BD5E20AC19C5191F3F60F7DE'

if (-not (Test-Path -LiteralPath $PsqlPath)) { throw 'psql executable unavailable' }
if (-not (Test-Path -LiteralPath $SslRootCert)) { throw 'CA root bundle unavailable for verify-full' }
if ((Get-FileHash -LiteralPath $provisionSql -Algorithm SHA256).Hash -ne $expectedProvisionHash) {
    throw '10-provision-app.sql SHA-256 mismatch'
}
if ((Get-FileHash -LiteralPath $inspectSql -Algorithm SHA256).Hash -ne $expectedInspectHash) {
    throw '00-inspect-readonly.sql SHA-256 mismatch'
}

$logDir = Join-Path $appRoot '.superpowers\sdd\task5-local-integration'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$logFile = Join-Path $logDir 'neon-original-provision-10.log'

function Invoke-PsqlStep {
    param(
        [string]$StepName,
        [string]$Arguments,
        [Security.SecureString]$StepCredential
    )
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $PsqlPath
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.Arguments = $Arguments
    $start.EnvironmentVariables.Clear()
    foreach ($key in @('SystemRoot','WINDIR','TEMP','TMP','USERPROFILE')) {
        $val = [Environment]::GetEnvironmentVariable($key, 'Process')
        if ($val) { $start.EnvironmentVariables[$key] = $val }
    }
    $start.EnvironmentVariables['PGSSLMODE'] = 'verify-full'
    $start.EnvironmentVariables['PGSSLROOTCERT'] = $SslRootCert
    $start.EnvironmentVariables['PGCONNECT_TIMEOUT'] = '15'
    $start.EnvironmentVariables['PGPASSWORD'] = [Net.NetworkCredential]::new('', $StepCredential).Password
    $proc = [Diagnostics.Process]::new()
    $proc.StartInfo = $start
    try {
        [void]$proc.Start()
        $start.EnvironmentVariables.Remove('PGPASSWORD')
        $stdout = $proc.StandardOutput.ReadToEndAsync()
        $stderr = $proc.StandardError.ReadToEndAsync()
        if (-not $proc.WaitForExit(60000)) {
            $proc.Kill()
            throw "$StepName timed out"
        }
        return [PSCustomObject]@{
            Step = $StepName
            ExitCode = $proc.ExitCode
            Stdout = $stdout.GetAwaiter().GetResult()
            Stderr = $stderr.GetAwaiter().GetResult()
        }
    } finally {
        $start.EnvironmentVariables.Remove('PGPASSWORD')
        $proc.Dispose()
    }
}

$ownsCredential = ($null -eq $Credential)
try {
    if ($ownsCredential) {
        $Credential = Read-Host 'Existing password of neondb_owner (hidden; used only in memory for 10-provision-app.sql + read-only verification)' -AsSecureString
    }
    $hostName = 'ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech'
    $provisionArgs = '-X -w -h ' + $hostName + ' -p 5432 -d neondb -U neondb_owner -v ON_ERROR_STOP=1 -v approve_original_app_provision=yes -f "' + $provisionSql + '"'
    $provisionResult = Invoke-PsqlStep -StepName '10-provision-app' -Arguments $provisionArgs -StepCredential $Credential

    $report = @(
        "=== STEP 1: 10-provision-app.sql (exitCode=$($provisionResult.ExitCode)) ===",
        $provisionResult.Stdout.Trim(),
        $provisionResult.Stderr.Trim()
    )

    $step1Committed = ($provisionResult.ExitCode -eq 0) `
        -and ($provisionResult.Stdout -match '(?m)^COMMIT\r?$') `
        -and ($provisionResult.Stdout -match 'PROVISION_COMMITTED:emr_financas_app') `
        -and ($provisionResult.Stderr -notmatch '(?i)(ERROR:|FATAL:|aviso:\s*\\quit|warning:\s*\\quit)')

    if (-not $step1Committed) {
        $report | Set-Content -LiteralPath $logFile -Encoding UTF8
        $failCode = if ($provisionResult.ExitCode -ne 0) { $provisionResult.ExitCode } else { 2 }
        Write-Output "provision=FAIL stage=provision exitCode=$failCode"
        if ($provisionResult.Stdout.Trim()) { Write-Output $provisionResult.Stdout.Trim() }
        if ($provisionResult.Stderr.Trim()) { Write-Output $provisionResult.Stderr.Trim() }
        exit $failCode
    }

    $inspectArgs = '-X -w -h ' + $hostName + ' -p 5432 -d neondb -U neondb_owner -v ON_ERROR_STOP=1 -f "' + $inspectSql + '"'
    $inspectResult = Invoke-PsqlStep -StepName '00-inspect-readonly' -Arguments $inspectArgs -StepCredential $Credential

    $report += @(
        "=== STEP 2: 00-inspect-readonly.sql (exitCode=$($inspectResult.ExitCode)) ===",
        $inspectResult.Stdout.Trim(),
        $inspectResult.Stderr.Trim()
    )

    $verifyQuery = @"
BEGIN READ ONLY;
DO `$`$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_roles
    WHERE rolname='emr_financas_app' AND rolcanlogin AND NOT rolinherit
      AND NOT (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls)
      AND rolconnlimit=3
  ) THEN
    RAISE EXCEPTION 'Independent verification failed: emr_financas_app missing or invalid role attributes';
  END IF;
  IF EXISTS (SELECT 1 FROM pg_auth_members WHERE member='emr_financas_app'::regrole) THEN
    RAISE EXCEPTION 'Independent verification failed: unexpected role membership';
  END IF;
  IF NOT has_database_privilege('emr_financas_app','neondb','CONNECT')
     OR has_database_privilege('emr_financas_app','neondb','CREATE')
     OR NOT has_schema_privilege('emr_financas_app','public','USAGE')
     OR has_schema_privilege('emr_financas_app','public','CREATE') THEN
    RAISE EXCEPTION 'Independent verification failed: invalid database/schema privileges';
  END IF;
  IF NOT (has_table_privilege('emr_financas_app','public.tb_emr_usuario','SELECT')
      AND has_table_privilege('emr_financas_app','public.tb_emr_usuario','INSERT')
      AND has_table_privilege('emr_financas_app','public.tb_emr_usuario','UPDATE')
      AND has_table_privilege('emr_financas_app','public.tb_emr_usuario','DELETE')
      AND has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','SELECT')
      AND has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','INSERT')
      AND has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','UPDATE')
      AND NOT has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','DELETE')
      AND NOT has_table_privilege('emr_financas_app','public.flyway_schema_history','SELECT')
      AND has_sequence_privilege('emr_financas_app','public.seq_emr_usuario','USAGE')
      AND NOT has_sequence_privilege('emr_financas_app','public.seq_emr_usuario','UPDATE')) THEN
    RAISE EXCEPTION 'Independent verification failed: invalid table/sequence privileges';
  END IF;
END `$`$;
SELECT 'READONLY_VERIFY_OK:emr_financas_app' AS verify_status;
ROLLBACK;
"@
    $verifyArgs = '-X -w -h ' + $hostName + ' -p 5432 -d neondb -U neondb_owner -v ON_ERROR_STOP=1 -c "' + ($verifyQuery -replace '"', '\"' -replace "`r?`n", ' ') + '"'
    $verifyResult = Invoke-PsqlStep -StepName 'readonly-verify-role' -Arguments $verifyArgs -StepCredential $Credential

    $report += @(
        "=== STEP 3: readonly-verify-role (exitCode=$($verifyResult.ExitCode)) ===",
        $verifyResult.Stdout.Trim(),
        $verifyResult.Stderr.Trim()
    )
    $report | Set-Content -LiteralPath $logFile -Encoding UTF8

    $verifiedOk = ($inspectResult.ExitCode -eq 0) `
        -and ($inspectResult.Stdout -match '\bemr_financas_app\b') `
        -and ($verifyResult.ExitCode -eq 0) `
        -and ($verifyResult.Stdout -match 'READONLY_VERIFY_OK:emr_financas_app') `
        -and ($verifyResult.Stderr -notmatch '(?i)(ERROR:|FATAL:)')

    if (-not $verifiedOk) {
        $failCode = if ($verifyResult.ExitCode -ne 0) { $verifyResult.ExitCode } elseif ($inspectResult.ExitCode -ne 0) { $inspectResult.ExitCode } else { 3 }
        Write-Output "provision=FAIL stage=readonly-verify exitCode=$failCode"
        if ($verifyResult.Stdout.Trim()) { Write-Output $verifyResult.Stdout.Trim() }
        if ($verifyResult.Stderr.Trim()) { Write-Output $verifyResult.Stderr.Trim() }
        exit $failCode
    }

    Write-Output "provision=PASS inspectExitCode=0 verifyExitCode=0 role=emr_financas_app"
    Write-Output $inspectResult.Stdout.Trim()
} finally {
    if ($ownsCredential -and $Credential) { $Credential.Dispose() }
}
