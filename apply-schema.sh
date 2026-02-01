#!/bin/bash

# PostgreSQL接続情報
DB_USER="user"
DB_NAME="task_app"
DB_PASSWORD="password"
SCHEMA_FILE="task-app/src/main/resources/sql/database_schema.sql"

echo "📝 Applying schema to existing database..."
echo "⚠️  Warning: This will attempt to create tables. Existing tables may cause errors."
echo ""

PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -d $DB_NAME -f $SCHEMA_FILE

echo ""
echo "✅ Schema application complete!"
echo ""
echo "📊 Current tables:"
PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -d $DB_NAME -c "\dt"


