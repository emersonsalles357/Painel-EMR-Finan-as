"""Run bounded Maven validation with a clean environment and sanitized evidence."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

APP = Path(__file__).resolve().parents[2]
BACKEND = APP / 'BackEnd/Back End/backend'
JAVA = Path(os.environ.get('EMR_JAVA_HOME', 'C:/Users/Desktop/AppData/Local/Temp/emr-performance-java17/jdk-17.0.20.1+1'))
MAVEN = Path(os.environ.get('EMR_MAVEN', 'C:/Users/Desktop/.m2/wrapper/dists/apache-maven-3.9.16-bin/5grr65jo27hi51sujmtcldfovl/apache-maven-3.9.16/bin/mvn.cmd'))
EVIDENCE = APP / '.superpowers/sdd/task5-local-integration'

def environment():
    prefixes = ('SPRING_', 'DEV_DB_', 'DB_', 'JWT_', 'MAIL_', 'MAVEN_', 'MIGRATION_', 'FLYWAY_', 'POSTGRES_', 'PGPASSWORD', 'DATABASE_URL', 'EMR_LOCAL_PG_', 'LOCAL_PG_', 'JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS')
    env = {k: v for k, v in os.environ.items() if not k.upper().startswith(prefixes)}
    env.update(JAVA_HOME=str(JAVA), MAVEN_OPTS='-Dfile.encoding=UTF-8')
    env['PATH'] = str(JAVA / 'bin') + os.pathsep + env.get('PATH', '')
    return env

def run(label, goals, extra_env=None):
    if not label or any(c not in 'abcdefghijklmnopqrstuvwxyz0123456789-_' for c in label):
        raise ValueError('Invalid evidence label')
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    env = environment()
    env.update(extra_env or {})
    start = time.monotonic()
    started_wall = time.time()
    result = subprocess.run([str(MAVEN), '-o', '-B', '-ntp', *goals], cwd=BACKEND, env=env,
                            capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=900)
    output = result.stdout + '\n' + result.stderr
    for k, v in (extra_env or {}).items():
        if any(s in k for s in ('PASSWORD', 'SECRET', 'TOKEN', 'URL')) and v:
            output = output.replace(v, '[REDACTED]')
    (EVIDENCE / (label + '.log')).write_text(output, encoding='utf-8')
    suites = []
    for path in sorted((BACKEND / 'target/surefire-reports').glob('TEST-*.xml')):
        if path.stat().st_mtime < started_wall:
            continue
        node = ET.parse(path).getroot()
        suites.append(dict(name=node.get('name'), **{k: int(node.get(k, 0)) for k in ('tests', 'failures', 'errors', 'skipped')}))
    report = dict(exitCode=result.returncode, seconds=round(time.monotonic()-start, 2), goals=goals,
                  totals={k: sum(s[k] for s in suites) for k in ('tests', 'failures', 'errors', 'skipped')}, suites=suites)
    (EVIDENCE / (label + '.json')).write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps({k: v for k, v in report.items() if k != 'suites'}), flush=True)
    return result.returncode

if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('label')
    p.add_argument('goals', nargs='+')
    args = p.parse_args()
    raise SystemExit(run(args.label, args.goals))
