#!/bin/bash
set -e
set -u

function create_user_and_database() {
    local database=$1
    local username=$2
    local password=$3
    echo "Creating user and database '${database}'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE USER ${username} WITH PASSWORD '${password}';
        CREATE DATABASE ${database}_db;
        GRANT ALL PRIVILEGES ON DATABASE ${database}_db TO ${username};
        ALTER DATABASE ${database}_db OWNER TO ${username};
EOSQL
}

if [ -n "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
    echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
        upper_db="$(echo "${db}" | tr '[:lower:]' '[:upper:]')"
        user_var_name="${upper_db}_DB_USERNAME"
        pass_var_name="${upper_db}_DB_PASSWORD"
        username="${!user_var_name:-${db}_user}"
        password="${!pass_var_name:-${db}_pass}"
        create_user_and_database "$db" "$username" "$password"
    done
fi