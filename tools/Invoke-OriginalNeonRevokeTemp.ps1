param(
    [string]$PsqlPath = 'C:\Program Files\PostgreSQL\17\bin\psql.exe',
    [string]$SslRootCert = 'C:\Program Files\Git\mingw64\etc\ssl\certs\ca-bundle.crt',
    [Security.SecureString]$Credential = $null
)
$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $PSScriptRoot
$revokeSql = Join-Path $appRoot 'deploy\neon-original\20-revoke-public-temp.sql'
$inspectSql = Join-Path $appRoot 'deploy\neon-original\00-inspect-readonly.sql'
$expectedRevokeHash = 'BC8B71A01A37135426303026BF8071D29B3BB3EBFE1077649E86A2DAE928E081'
$expectedInspectHash = '08F0D802C4547D2ECC43B3CB4010DBF0A69E7E71BD5E20AC19C5191F3F60F7DE'

if (-not (Test-Path -LiteralPath $PsqlPath)) { throw 'psql executable unavailable' }
if (-not (Test-Path -LiteralPath $SslRootCert)) { throw 'CA root bundle unavailable for verify-full' }
if ((Get-FileHash -LiteralPath $revokeSql -Algorithm SHA256).Hash -ne $expectedRevokeHash) {
    throw '20-revoke-public-temp.sql SHA-256 mismatch'
}
if ((Get-FileHash -LiteralPath $inspectSql -Algorithm SHA256).Hash -ne $expectedInspectHash) {
    throw '00-inspect-readonly.sql SHA-256 mismatch'
}

$logDir = Join-Path $appRoot '.superpowers\sdd\task5-local-integration'
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$logFile = Join-Path $logDir 'neon-original-revoke-20.log'

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
        $Credential = Read-Host 'Existing password of neondb_owner (hidden; used only in memory for 20-revoke-public-temp.sql + read-only verification)' -AsSecureString
    }
    $hostName = 'ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech'
    $revokeArgs = '-X -w -h ' + $hostName + ' -p 5432 -d neondb -U neondb_owner -v ON_ERROR_STOP=1 -v approve_revoke_public_temp=yes -f "' + $revokeSql + '"'
    $revokeResult = Invoke-PsqlStep -StepName '20-revoke-public-temp' -Arguments $revokeArgs -StepCredential $Credential

    $report = @(
        "=== STEP 1: 20-revoke-public-temp.sql (exitCode=$($revokeResult.ExitCode)) ===",
        $revokeResult.Stdout.Trim(),
        $revokeResult.Stderr.Trim()
    )

    $step1Committed = ($revokeResult.ExitCode -eq 0) `
        -and ($revokeResult.Stdout -match '(?m)^COMMIT\r?$') `
        -and ($revokeResult.Stdout -match 'PUBLIC_TEMP_REVOKED:neondb') `
        -and ($revokeResult.Stderr -notmatch '(?i)(ERROR:|FATAL:|aviso:\s*\\quit|warning:\s*\\quit)')

    if (-not $step1Committed) {
        $report | Set-Content -LiteralPath $logFile -Encoding UTF8
        $failCode = if ($revokeResult.ExitCode -ne 0) { $revokeResult.ExitCode } else { 2 }
        Write-Output "revoke=FAIL stage=revoke exitCode=$failCode"
        if ($revokeResult.Stdout.Trim()) { Write-Output $revokeResult.Stdout.Trim() }
        if ($revokeResult.Stderr.Trim()) { Write-Output $revokeResult.Stderr.Trim() }
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
DECLARE t text; s text;
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
  IF NOT (has_database_privilege('neondb_owner','neondb','CONNECT')
      AND has_database_privilege('neondb_owner','neondb','CREATE')
      AND has_database_privilege('neondb_owner','neondb','TEMP')) THEN
    RAISE EXCEPTION 'Independent verification failed: neondb_owner privileges affected';
  END IF;
  IF NOT (has_database_privilege('emr_financas_app','neondb','CONNECT')
      AND NOT has_database_privilege('emr_financas_app','neondb','CREATE')
      AND NOT has_database_privilege('emr_financas_app','neondb','TEMP')
      AND has_schema_privilege('emr_financas_app','public','USAGE')
      AND NOT has_schema_privilege('emr_financas_app','public','CREATE')) THEN
    RAISE EXCEPTION 'Independent verification failed: invalid database/schema privileges for emr_financas_app';
  END IF;
  FOREACH t IN ARRAY ARRAY['public.tb_emr_usuario','public.tb_emr_gasto','public.tb_emr_recebimento','public.tb_emr_investimento'] LOOP
    IF NOT (has_table_privilege('emr_financas_app',t,'SELECT')
        AND has_table_privilege('emr_financas_app',t,'INSERT')
        AND has_table_privilege('emr_financas_app',t,'UPDATE')
        AND has_table_privilege('emr_financas_app',t,'DELETE')
        AND NOT has_table_privilege('emr_financas_app',t,'TRUNCATE')
        AND NOT has_table_privilege('emr_financas_app',t,'REFERENCES')
        AND NOT has_table_privilege('emr_financas_app',t,'TRIGGER')) THEN
      RAISE EXCEPTION 'Independent verification failed: invalid CRUD table privileges on %', t;
    END IF;
  END LOOP;
  IF NOT (has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','SELECT')
      AND has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','INSERT')
      AND has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','UPDATE')
      AND NOT has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','DELETE')
      AND NOT has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','TRUNCATE')
      AND NOT has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','REFERENCES')
      AND NOT has_table_privilege('emr_financas_app','public.tb_emr_password_reset_token','TRIGGER')
      AND NOT has_table_privilege('emr_financas_app','public.flyway_schema_history','SELECT')
      AND NOT has_table_privilege('emr_financas_app','public.flyway_schema_history','INSERT')
      AND NOT has_table_privilege('emr_financas_app','public.flyway_schema_history','UPDATE')
      AND NOT has_table_privilege('emr_financas_app','public.flyway_schema_history','DELETE')) THEN
    RAISE EXCEPTION 'Independent verification failed: invalid token or flyway table privileges';
  END IF;
  FOREACH s IN ARRAY ARRAY['public.seq_emr_usuario','public.seq_emr_gasto','public.seq_emr_recebimento','public.seq_emr_investimento','public.seq_emr_password_reset_token'] LOOP
    IF NOT (has_sequence_privilege('emr_financas_app',s,'USAGE')
        AND NOT has_sequence_privilege('emr_financas_app',s,'SELECT')
        AND NOT has_sequence_privilege('emr_financas_app',s,'UPDATE')) THEN
      RAISE EXCEPTION 'Independent verification failed: invalid sequence privileges on %', s;
    END IF;
  END LOOP;
END `$`$;
SELECT 'REVOKE_VERIFY_OK:emr_financas_app' AS verify_status;
ROLLBACK;
"@
    $verifyArgs = '-X -w -h ' + $hostName + ' -p 5432 -d neondb -U neondb_owner -v ON_ERROR_STOP=1 -c "' + ($verifyQuery -replace '"', '\"' -replace "`r?`n", ' ') + '"'
    $verifyResult = Invoke-PsqlStep -StepName 'readonly-verify-revoke' -Arguments $verifyArgs -StepCredential $Credential

    $report += @(
        "=== STEP 3: readonly-verify-revoke (exitCode=$($verifyResult.ExitCode)) ===",
        $verifyResult.Stdout.Trim(),
        $verifyResult.Stderr.Trim()
    )
    $report | Set-Content -LiteralPath $logFile -Encoding UTF8

    $verifiedOk = ($inspectResult.ExitCode -eq 0) `
        -and ($inspectResult.Stdout -match '\bemr_financas_app\b') `
        -and ($verifyResult.ExitCode -eq 0) `
        -and ($verifyResult.Stdout -match 'REVOKE_VERIFY_OK:emr_financas_app') `
        -and ($verifyResult.Stderr -notmatch '(?i)(ERROR:|FATAL:)')

    if (-not $verifiedOk) {
        $failCode = if ($verifyResult.ExitCode -ne 0) { $verifyResult.ExitCode } elseif ($inspectResult.ExitCode -ne 0) { $inspectResult.ExitCode } else { 3 }
        Write-Output "revoke=FAIL stage=readonly-verify exitCode=$failCode"
        if ($verifyResult.Stdout.Trim()) { Write-Output $verifyResult.Stdout.Trim() }
        if ($verifyResult.Stderr.Trim()) { Write-Output $verifyResult.Stderr.Trim() }
        exit $failCode
    }

    Write-Output "revoke=PASS inspectExitCode=0 verifyExitCode=0 role=emr_financas_app status=REVOKE_VERIFY_OK:emr_financas_app"
    Write-Output $inspectResult.Stdout.Trim()
} finally {
    if ($ownsCredential -and $Credential) { $Credential.Dispose() }
}
