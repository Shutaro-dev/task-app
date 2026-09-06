# /reset-db — DB リセット

**警告: 全データが削除されます。** 開発中に DB を初期状態に戻したい場合に使用。

## 実行

```bash
# プロジェクトルートで実行
./reset-db.sh
```

内部で Rails のマイグレーション（`bin/rails db:drop db:create db:schema:load db:seed`）を実行している。

## スキーマのみ適用（データを消さない場合）

テーブルが存在しない場合のみ適用（既存データは保持）:

```bash
./apply-schema.sh
```

## マイグレーション（列追加など）

```bash
cd task-app
bin/rails generate migration AddColumnToRoles some_column:string
bin/rails db:migrate
```

## DB 接続情報

| 項目 | 値 |
|---|---|
| Host | 127.0.0.1:5433 |
| DB | task_app |
| User | user |
| Password | password |
