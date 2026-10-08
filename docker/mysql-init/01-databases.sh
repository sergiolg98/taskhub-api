#!/bin/bash
# Runs once, when the MySQL data volume is empty. One server, one database per service:
# auth_db belongs to auth-service and task_db to task-service (neither reads the other's tables).
set -e
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS task_db;
GRANT ALL PRIVILEGES ON auth_db.* TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON task_db.* TO '${MYSQL_USER}'@'%';
SQL
