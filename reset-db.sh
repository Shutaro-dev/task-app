#!/bin/bash
# DB を完全リセットし、Rails のマイグレーションでスキーマを再適用する。
# 注意: task_app の全データが削除される。

set -e

echo "🗑️  Dropping and recreating database..."
(cd task-app && DB_PORT="${DB_PORT:-5433}" bin/rails db:drop db:create)

echo "📝 Applying schema (db:schema:load)..."
(cd task-app && DB_PORT="${DB_PORT:-5433}" bin/rails db:schema:load)

echo "🌱 Seeding initial data..."
(cd task-app && DB_PORT="${DB_PORT:-5433}" bin/rails db:seed)

echo "✅ Database reset complete!"
echo ""
echo "📊 Verifying tables..."
PGPASSWORD=password psql -h 127.0.0.1 -p "${DB_PORT:-5433}" -U user -d task_app -c "\dt"
