#!/bin/bash

# PostgreSQL接続情報
DB_USER="user"
DB_NAME="task_app"
DB_PASSWORD="password"
SCHEMA_FILE="task-app/src/main/resources/sql/database_schema.sql"

echo "🗑️  Dropping database if exists..."
PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"

echo "🆕 Creating database..."
PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -d postgres -c "CREATE DATABASE $DB_NAME;"

echo "📝 Applying schema..."
PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -d $DB_NAME -f $SCHEMA_FILE

echo "✅ Database reset complete!"
echo ""
echo "📊 Verifying tables..."
PGPASSWORD=$DB_PASSWORD psql -U $DB_USER -d $DB_NAME -c "\dt"


