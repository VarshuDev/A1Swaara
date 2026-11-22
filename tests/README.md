# Supabase policy regression test

Use a disposable Supabase instance, never production. Apply
`app/src/main/assets/supabase_schema.sql`, then run:

```sh
psql "$SUPABASE_TEST_DATABASE_URL" -v ON_ERROR_STOP=1 -f tests/supabase_rls.sql
```

The script requires permission to insert test users and switch to `anon` and
`authenticated`. It runs in a transaction and rolls back its fixtures. It tests
profile email privileges, public/private room reads, host-only mutation and
queue access. Deployment-specific grants, existing policies with different names,
and existing data still need a separate audit. The checked-in SQL is not itself
a deployment migration and does not change any hosted project.
