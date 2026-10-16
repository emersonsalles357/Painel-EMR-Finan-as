-- Read-only review input. No credentials, row contents or query texts.
\set ON_ERROR_STOP on
BEGIN READ ONLY;
SELECT current_database(),current_user,session_user,current_schema(),
       current_setting('server_version'),current_setting('transaction_read_only');
SELECT ssl,version,cipher FROM pg_stat_ssl WHERE pid=pg_backend_pid();
SELECT rolname,rolcanlogin,rolinherit,rolsuper,rolcreatedb,rolcreaterole,rolreplication,rolbypassrls
FROM pg_roles WHERE rolcanlogin OR rolname='emr_financas_app' ORDER BY rolname;
SELECT r.rolname AS member_of,u.rolname AS member,m.inherit_option,m.set_option,m.admin_option
FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.roleid JOIN pg_roles u ON u.oid=m.member
WHERE u.rolcanlogin ORDER BY 2,1;
SELECT datname,pg_get_userbyid(datdba) AS owner,datacl::text
FROM pg_database WHERE NOT datistemplate ORDER BY datname;
SELECT nspname,pg_get_userbyid(nspowner) AS owner,nspacl::text FROM pg_namespace
WHERE nspname !~ '^pg_' AND nspname<>'information_schema';
SELECT n.nspname,c.relname,c.relkind,pg_get_userbyid(c.relowner) AS owner,c.relacl::text
FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema' ORDER BY 1,2;
SELECT pg_get_userbyid(defaclrole) AS creator,defaclnamespace::regnamespace,
       defaclobjtype,defaclacl::text FROM pg_default_acl ORDER BY 1,2;
-- Aggregate connections only; no client IP, query text or user data.
SELECT usename,application_name,state,count(*) FROM pg_stat_activity
WHERE datname=current_database() GROUP BY 1,2,3 ORDER BY 1,2,3;
SELECT version,checksum,success FROM public.flyway_schema_history ORDER BY installed_rank;
ROLLBACK;
