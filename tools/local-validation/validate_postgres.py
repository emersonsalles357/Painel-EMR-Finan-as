"""Create/attest ONE new PG16 tmpfs container, validate, stop only that container.

Never starts Docker Desktop; never enumerates, mounts or removes Docker volumes.
No secrets in arguments, persisted evidence, or printed output. Requires Java17/Maven.
"""
from datetime import date, datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import socket
import subprocess
import time
import urllib.error
import urllib.request
import uuid
from run_maven import APP, BACKEND, EVIDENCE, JAVA, environment, run as maven

IMAGE = 'postgres:16.15-bookworm'
JAR = BACKEND / 'target/app.jar'
MAIN = 'br.com.emr.emrfinancas.database.DatabaseMigration'

def require(ok, reason):
    if not ok:
        raise RuntimeError(reason)

def main():
    require(JAR.is_file(), 'Build the backend first')
    nonce = uuid.uuid4().hex
    name = 'emr-task5-local-' + nonce[:12]
    database = 'emr_task5_it_' + nonce[:12]
    marker = 'emr-task5-disposable:' + nonce
    out = EVIDENCE / ('postgres-' + nonce[:12])
    out.mkdir(parents=True)
    protected = [secrets.token_urlsafe(36) for _ in range(4)]
    admin_password, migration_password, app_password, jwt = protected
    secret_values = list(protected)
    report = dict(startedUtc=datetime.now(timezone.utc).isoformat(), status='RUNNING', containerName=name,
                  database=database, historicalVolumeAccess=False, remoteDatabaseAccess=False,
                  storage='new tmpfs, no volumes or binds', steps=[])
    container = None
    runtime = None
    runtime_log = None
    attachment = None

    def scrub(value):
        for secret in secret_values:
            value = value.replace(secret, '[REDACTED]')
        return re.sub(r'jdbc:postgresql://[^\s\x1b"\']+', '[JDBC URL REDACTED]', value)

    def save():
        (out / 'result.json').write_text(scrub(json.dumps(report, indent=2)), encoding='utf-8')

    def docker(args, stdin=None, env=None, timeout=60):
        result = subprocess.run(['docker', *args], input=stdin, env=env, capture_output=True,
                                text=True, encoding='utf-8', errors='replace', timeout=timeout)
        require(result.returncode == 0, 'Docker operation failed: ' + scrub(result.stderr[-2000:]))
        return result.stdout.strip()

    def attest():
        require(container is not None and re.fullmatch('[a-f0-9]{64}', container), 'Unknown container identity')
        obj = json.loads(docker(['inspect', container]))[0]
        require(obj['Name'] == '/' + name and obj['Image'] == image['Id'], 'Container identity mismatch')
        require(obj['Config']['Labels'].get('emr.task5.local') == nonce, 'Container label mismatch')
        require(obj['HostConfig']['RestartPolicy']['Name'] == 'no', 'Unexpected restart policy')
        require(not obj['HostConfig']['Privileged'], 'Privileged container rejected')
        require(not obj['HostConfig'].get('Binds') and all(m['Type'] == 'tmpfs' for m in obj['Mounts']), 'Persistent mounts rejected')
        require(not any(item.startswith('POSTGRES_PASSWORD=') for item in obj['Config']['Env']), 'Secret persisted in Docker metadata')
        require(set(obj['HostConfig']['Tmpfs']) == {'/var/lib/postgresql/data'}, 'Unexpected storage')
        ports = obj['HostConfig']['PortBindings']
        require(set(ports) == {'5432/tcp'} and len(ports['5432/tcp']) == 1
                and ports['5432/tcp'][0]['HostIp'] == '127.0.0.1', 'Non-loopback binding rejected')
        return obj

    def sql(text, db=database, variables=None):
        attest()
        args = ['exec', '-i', container, 'psql', '-X', '-v', 'ON_ERROR_STOP=1', '-U', 'postgres', '-d', db, '-A', '-t']
        for key, value in (variables or {}).items():
            args += ['-v', key + '=' + value]
        return docker(args, stdin=text)

    def migrate(extra=None, validate=False):
        env = environment()
        env.update(MIGRATION_MODE='local', MIGRATION_DB_URL=jdbc, MIGRATION_DB_USERNAME='emr_task5_migrator',
                   MIGRATION_DB_PASSWORD=migration_password, MIGRATION_LOCAL_MARKER=marker)
        env.update(extra or {})
        command = [str(JAVA / 'bin/java.exe'), '-Dloader.main=' + MAIN, '-cp', str(JAR),
                   'org.springframework.boot.loader.launch.PropertiesLauncher'] + (['--validate-only'] if validate else [])
        return subprocess.run(command, env=env, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=120)

    try:
        image = json.loads(docker(['image', 'inspect', IMAGE]))[0]
        image_env = dict(item.split('=', 1) for item in image['Config']['Env'] if '=' in item)
        require(image_env.get('PG_MAJOR') == '16' and image_env.get('PG_VERSION', '').startswith('16.15'), 'PostgreSQL16.15 required')
        report.update(imageId=image['Id'], imageDigests=image.get('RepoDigests', []), jarSha256=hashlib.sha256(JAR.read_bytes()).hexdigest())
        container = docker(['create', '--interactive', '--entrypoint', '/bin/sh', '--name', name, '--label', 'emr.task5.local=' + nonce, '--restart=no',
                            '--publish', '127.0.0.1::5432', '--memory=512m', '--cpus=1', '--pids-limit=128',
                            '--security-opt=no-new-privileges', '--tmpfs', '/var/lib/postgresql/data:rw,nosuid,noexec,size=256m',
                            '--env', 'POSTGRES_INITDB_ARGS=--auth-host=scram-sha-256', image['Id'], '-c',
                            'IFS= read -r POSTGRES_PASSWORD || exit 1; export POSTGRES_PASSWORD; exec /usr/local/bin/docker-entrypoint.sh postgres'])
        before = attest()
        require(before['State']['Status'] == 'created' and not before['State']['Running'], 'Container already started')
        report['isolation'] = dict(containerId=container, attestedBeforeStart=True, mounts=before['Mounts'],
                                   tmpfs=before['HostConfig']['Tmpfs'], restartPolicy='no', bindHost='127.0.0.1', bootstrapSecretInConfigEnv=False)
        save()
        # Send the bootstrap secret over stdin, not Docker Config.Env/arguments/files.
        attachment = subprocess.Popen(['docker', 'start', '--attach', '--interactive', container],
                                      stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, text=True)
        attachment.stdin.write(admin_password + '\n')
        attachment.stdin.flush()
        attachment.stdin.close()
        deadline = time.monotonic() + 60
        while True:
            ready = subprocess.run(['docker', 'exec', container, 'pg_isready', '-U', 'postgres'], capture_output=True)
            if ready.returncode == 0:
                break
            require(time.monotonic() < deadline, 'PostgreSQL readiness timeout')
            time.sleep(1)
        current = attest()
        port = int(current['NetworkSettings']['Ports']['5432/tcp'][0]['HostPort'])
        require(1024 < port <= 65535 and port != 5432, 'Unsafe disposable port')
        jdbc = 'jdbc:postgresql://127.0.0.1:' + str(port) + '/' + database
        secret_values.append(jdbc)
        bootstrap = (
            "CREATE ROLE emr_task5_migrator LOGIN PASSWORD '" + migration_password + "' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;\n"
            "CREATE ROLE emr_task5_app LOGIN PASSWORD '" + app_password + "' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS;\n"
            'CREATE DATABASE ' + database + ' OWNER emr_task5_migrator;\n'
            "COMMENT ON DATABASE " + database + " IS '" + marker + "';\n")
        sql(bootstrap, db='postgres')
        report['serverVersion'] = sql("SELECT current_setting('server_version')")
        require(report['serverVersion'].startswith('16.15'), 'Actual PostgreSQL version mismatch')
        sql('REVOKE ALL ON DATABASE ' + database + ' FROM PUBLIC; GRANT CONNECT ON DATABASE ' + database
            + ' TO emr_task5_app,emr_task5_migrator; REVOKE CREATE ON SCHEMA public FROM PUBLIC;')
        print('New PostgreSQL16 disposable container attested; no persistent mounts.', flush=True)

        rejected = migrate({'MIGRATION_LOCAL_MARKER': 'emr-task5-disposable:' + '0' * 32})
        require(rejected.returncode != 0, 'Wrong marker must be rejected')
        require(sql("SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind='r'") == '0', 'Rejected target was mutated')
        report['steps'].append(dict(name='wrong-marker-rejected-before-migration', passed=True))

        result = migrate()
        (out / 'migration.log').write_text(scrub(result.stdout + result.stderr), encoding='utf-8')
        require(result.returncode == 0 and 'executed=6 validated=6 http=false' in result.stdout, 'One-shot migration failed; inspect sanitized migration.log')
        again = migrate(validate=True)
        (out / 'migration-validate.log').write_text(scrub(again.stdout + again.stderr), encoding='utf-8')
        require(again.returncode == 0 and 'executed=0 validated=6' in again.stdout, 'Checksum validation failed')
        history = sql("SELECT json_agg(t) FROM (SELECT version,checksum,success,installed_by FROM public.flyway_schema_history ORDER BY installed_rank) t")
        report['migrations'] = json.loads(history)
        require(len(report['migrations']) == 6 and all(m['success'] and m['checksum'] is not None
                and m['installed_by'] == 'emr_task5_migrator' for m in report['migrations']), 'History mismatch')
        acl = (APP / 'deploy/task5-runtime-privileges.sql').read_text(encoding='utf-8')
        sql(acl, variables={'target_database': database})
        report['runtimePrivileges'] = json.loads(sql("SELECT json_build_object("
            "'role','emr_task5_app','connect',has_database_privilege('emr_task5_app',current_database(),'CONNECT'),"
            "'databaseCreate',has_database_privilege('emr_task5_app',current_database(),'CREATE'),"
            "'temporary',has_database_privilege('emr_task5_app',current_database(),'TEMP'),"
            "'schemaUsage',has_schema_privilege('emr_task5_app','public','USAGE'),"
            "'schemaCreate',has_schema_privilege('emr_task5_app','public','CREATE'),"
            "'sequenceUsage',has_sequence_privilege('emr_task5_app','public.seq_emr_usuario','USAGE'),"
            "'sequenceUpdate',has_sequence_privilege('emr_task5_app','public.seq_emr_usuario','UPDATE'),"
            "'historySelect',has_table_privilege('emr_task5_app','public.flyway_schema_history','SELECT'))"))
        report['steps'].append(dict(name='six-migrations-and-checksums', passed=True, migrationRole='emr_task5_migrator'))
        print('V001–V006 applied and checksums validated; restricted runtime ACLs applied locally.', flush=True)

        local = dict(EMR_LOCAL_PG_URL=jdbc, EMR_LOCAL_PG_PASSWORD=app_password,
                     EMR_LOCAL_PG_MARKER=marker, EMR_LOCAL_PG_ENABLED='true')
        # Mutation test: unexpected DDL capability outside public must prevent startup.
        sql('CREATE SCHEMA task5_unexpected AUTHORIZATION emr_task5_app;')
        denied_env = environment()
        denied_env.update(local, SPRING_PROFILES_ACTIVE='local-pg', JWT_SECRET=jwt)
        denied = subprocess.run([str(JAVA / 'bin/java.exe'), '-jar', str(JAR), '--server.port=0'],
                                env=denied_env, cwd=BACKEND, capture_output=True, text=True,
                                encoding='utf-8', errors='replace', timeout=60)
        (out / 'unexpected-schema-rejection.log').write_text(scrub(denied.stdout + denied.stderr), encoding='utf-8')
        require(denied.returncode != 0 and 'Database identity or privileges rejected' in denied.stdout + denied.stderr,
                'Unexpected schema ownership must prevent runtime startup')
        sql('DROP SCHEMA task5_unexpected;')
        report['steps'].append(dict(name='non-public-schema-ddl-capability-rejected', passed=True))
        code = maven('postgres-live-' + nonce[:12], ['test', '-Dtest=PostgresDatabaseLiveIntegrationTests'], local)
        require(code == 0, 'PostgreSQL integration tests failed')
        test_report = json.loads((EVIDENCE / ('postgres-live-' + nonce[:12] + '.json')).read_text())
        report['postgresTests'] = test_report['totals']

        with socket.socket() as reservation:
            reservation.bind(('127.0.0.1', 0))
            http_port = reservation.getsockname()[1]
        http_env = environment()
        http_env.update(local, SPRING_PROFILES_ACTIVE='local-pg', PORT=str(http_port), JWT_SECRET=jwt)
        http_env.pop('EMR_LOCAL_PG_ENABLED', None)
        require(not any(k.startswith('MIGRATION_') for k in http_env), 'DDL credentials leaked to HTTP environment')
        require(migration_password not in http_env.values() and admin_password not in http_env.values(), 'Privileged secret leaked to HTTP')
        report['runtime'] = dict(profile='local-pg', role='emr_task5_app', flywayEnabled=False,
                                 migrationCredentialPresent=False, hibernate='validate', bindHost='127.0.0.1')
        runtime_log = (out / 'runtime.log').open('w+', encoding='utf-8')
        runtime = subprocess.Popen([str(JAVA / 'bin/java.exe'), '-jar', str(JAR)], env=http_env,
                                   cwd=BACKEND, stdout=runtime_log, stderr=subprocess.STDOUT)
        base = 'http://127.0.0.1:' + str(http_port)
        checks = []
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))

        def request(method, path, expected, body=None, token=None):
            headers = {'Content-Type': 'application/json'}
            if token:
                headers['Authorization'] = 'Bearer ' + token
            req = urllib.request.Request(base + path, data=json.dumps(body).encode() if body is not None else None, headers=headers, method=method)
            try:
                response = opener.open(req, timeout=10)
            except urllib.error.HTTPError as error:
                response = error
            payload = response.read()
            checks.append(dict(method=method, path=path, status=response.code, expected=expected))
            require(response.code == expected, 'HTTP status mismatch for ' + method + ' ' + path + ': ' + str(response.code))
            return json.loads(payload) if payload else None

        deadline = time.monotonic() + 90
        while True:
            require(runtime.poll() is None, 'HTTP runtime failed; inspect sanitized runtime.log')
            try:
                if opener.open(base + '/health', timeout=2).status == 200:
                    break
            except (OSError, urllib.error.URLError):
                pass
            require(time.monotonic() < deadline, 'HTTP readiness timeout')
            time.sleep(1)
        request('GET', '/api/auth/me', 401)
        password = secrets.token_urlsafe(20)
        secret_values.append(password)
        tokens = []
        for suffix in ('a', 'b'):
            email = 'task5-' + suffix + '-' + nonce[:12] + '@example.test'
            request('POST', '/api/auth/register', 201, dict(nome='Task Five ' + suffix, email=email, senha=password))
            login = request('POST', '/api/auth/login', 200, dict(email=email, senha=password))
            token = login['accessToken']
            tokens.append(token)
            secret_values.append(token)
        request('GET', '/api/auth/me', 200, token=tokens[0])
        request('GET', '/api/usuarios', 403, token=tokens[0])
        day = date.today().isoformat()
        items = [('/api/gastos', dict(descricao='Despesa Task Five', categoria='Teste', valor=10.25, data=day)),
                 ('/api/recebimentos', dict(descricao='Receita Task Five', origem='Teste', valor=100.50, data=day, status='Recebido')),
                 ('/api/investimentos', dict(nome='Investimento Task Five', tipo='Teste', instituicao='Teste', valorAplicado=20.75, rentabilidadeMensal=1, dataAplicacao=day))]
        created = []
        for path, body in items:
            value = request('POST', path, 201, body, tokens[0])
            object_path = path + '/' + str(value['codigo'])
            request('GET', object_path, 200, token=tokens[0])
            request('PUT', object_path, 200, body, tokens[0])
            request('GET', object_path, 404, token=tokens[1])
            request('DELETE', object_path, 404, token=tokens[1])
            created.append(object_path)
        dashboard = request('GET', '/api/dashboard', 200, token=tokens[0])
        require(float(dashboard['saldo']) == 111.0, 'Dashboard persistence result mismatch')
        for path in created:
            request('DELETE', path, 204, token=tokens[0])
            request('GET', path, 404, token=tokens[0])
        # Persistence evidence read through the local administrative socket, never HTTP credentials.
        require(sql("SELECT count(*) FROM public.tb_emr_usuario WHERE ds_email LIKE 'task5-%'") == '2', 'HTTP users were not persisted')
        report['httpChecks'] = checks
        report['steps'].append(dict(name='http-auth-crud-dashboard-idor', passed=True, assertions=len(checks) + 2))
        report['status'] = 'PASS'
        print('Restricted Spring Boot runtime, authentication, CRUD, dashboard and IDOR verified.', flush=True)
    except Exception as error:
        report['status'] = 'FAIL'
        report['failure'] = scrub(str(error))
        raise
    finally:
        if runtime is not None:
            runtime.terminate()
            try:
                runtime.wait(timeout=15)
            except subprocess.TimeoutExpired:
                runtime.kill()
                runtime.wait(timeout=10)
        if runtime_log is not None:
            runtime_log.seek(0)
            content = runtime_log.read()
            runtime_log.seek(0)
            runtime_log.truncate()
            runtime_log.write(scrub(content))
            runtime_log.close()
        if container is not None:
            try:
                attest()
                docker(['stop', '--time', '10', container])
                report['containerStopped'] = True
                attest()
                docker(['rm', container])
                report['containerRemoved'] = True
            except Exception as stop_error:
                report['stopFailure'] = scrub(str(stop_error))
                report['status'] = 'FAIL'
        if attachment is not None:
            try:
                attachment.wait(timeout=15)
            except subprocess.TimeoutExpired:
                attachment.terminate()
                attachment.wait(timeout=10)
        report['finishedUtc'] = datetime.now(timezone.utc).isoformat()
        save()
        print(json.dumps(dict(status=report['status'], evidence=str(out / 'result.json'), containerStopped=report.get('containerStopped', False), containerRemoved=report.get('containerRemoved', False))), flush=True)
    if report['status'] != 'PASS':
        raise RuntimeError('Validation cleanup failed; see sanitized result.json')

if __name__ == '__main__':
    main()
