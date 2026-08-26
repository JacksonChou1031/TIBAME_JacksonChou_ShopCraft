#!/usr/bin/env bash
set -euo pipefail

SQLCMD="/opt/mssql-tools18/bin/sqlcmd"

for attempt in $(seq 1 30); do
  if "$SQLCMD" -S sqlserver -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "SELECT 1" -b >/dev/null 2>&1; then
    break
  fi
  if [ "$attempt" -eq 30 ]; then
    echo "SQL Server did not become reachable in time." >&2
    exit 1
  fi
  sleep 2
done

"$SQLCMD" -S sqlserver -U sa -P "$MSSQL_SA_PASSWORD" -C \
  -v APP_PASSWORD="$MSSQL_APP_PASSWORD" \
  -i /init/01-create-database-and-user.sql \
  -b
