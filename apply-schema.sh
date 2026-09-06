#!/bin/bash
# 既存のDBにスキーマのみ適用する(Rails のマイグレーション)。
# 注意: 既にテーブルが存在する場合はエラーになる可能性がある。

set -e

echo "📝 Applying schema to existing database (db:migrate)..."
echo "⚠️  Warning: This will attempt to create tables. Existing tables may cause errors."
echo ""

(cd task-app && DB_PORT="${DB_PORT:-5433}" bin/rails db:migrate)

echo ""
echo "✅ Schema application complete!"
echo ""
echo "📊 Current tables:"
PGPASSWORD=password psql -h 127.0.0.1 -p "${DB_PORT:-5433}" -U user -d task_app -c "\dt"
