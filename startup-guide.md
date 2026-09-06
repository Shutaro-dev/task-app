# 起動ガイド

## 構成概要

```
my-app/
├── front-task-app/   # フロントエンド (React 19 + Vite, port: 5173)
└── task-app/         # バックエンド (Ruby on Rails 8 API, port: 8080)
                      # DB: PostgreSQL (port: 5433, DB名: task_app)
```

---

## 1. PostgreSQL の起動

Docker コンテナ（`my-postgres`）で PostgreSQL を起動する。

```bash
# 起動
docker start my-postgres

# 停止
docker stop my-postgres

# 状態確認
docker ps | grep my-postgres
```

> **注意:** このマシンではポート5432をネイティブ(Homebrew)PostgreSQLが先に専有しているため、
> `my-postgres` コンテナはホスト側ポート **5433** にマッピングしている(`-p 5433:5432`)。
> 他のマシンで5432が空いている場合はそちらを使っても構わないが、その場合は
> `task-app/config/database.yml` の `DB_PORT`(または直書きの `5433`)を合わせて変更すること。

### コンテナの初回作成（my-postgres が存在しない場合のみ）

```bash
docker run --name my-postgres \
  -e POSTGRES_USER=user \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=task_app \
  -p 5433:5432 \
  -d postgres
```

### スキーマの適用

プロジェクトルート（`my-app/`）で実行：

```bash
# DB を完全リセットしてスキーマを再適用（注意: 全データ削除）
./reset-db.sh

# 既存 DB にスキーマのみ適用（テーブルが存在しない場合）
./apply-schema.sh
```

内部的には `task-app` の Rails マイグレーション（`bin/rails db:schema:load` / `db:migrate`）を実行している。

### DB に直接接続

```bash
# psql クライアントが手元にある場合
PGPASSWORD=password psql -h 127.0.0.1 -p 5433 -U user -d task_app

# Docker 経由で接続する場合（ホスト側ポート競合を気にしなくてよい）
docker exec -it my-postgres psql -U user -d task_app
```

---

## 2. バックエンドの起動

`task-app/` ディレクトリで実行：

```bash
cd task-app

# 初回のみ: gem インストール
bundle install

# 起動
bin/rails server -p 8080

# テスト実行（task_app_test DB を使用）
bin/rails test

# マイグレーション状態の確認
bin/rails db:migrate:status
```

起動後: `http://localhost:8080`

### 主なエンドポイント

| Method | Path | 概要 |
|---|---|---|
| GET | `/api/roles` | ロール一覧（タスク含む） |
| POST | `/api/roles` | ロール作成 |
| PUT | `/api/roles/{id}` | ロール更新 |
| PUT | `/api/roles/reorder` | ロール並び順一括更新 |
| DELETE | `/api/roles/{id}` | ロール削除 |
| GET | `/api/tasks` | タスク一覧 |
| POST | `/api/tasks` | タスク作成 |
| PUT | `/api/tasks/{id}` | タスク更新 |
| PUT | `/api/tasks/reorder` | タスク並び順一括更新 |
| DELETE | `/api/tasks/{id}` | タスク削除 |

---

## 3. フロントエンドの起動

`front-task-app/` ディレクトリで実行：

```bash
cd front-task-app

# 依存パッケージインストール（初回のみ）
npm install

# 開発サーバー起動
npm run dev

# ビルド
npm run build

# ビルド成果物のプレビュー
npm run preview
```

起動後: `http://localhost:5173`

---

## 4. 通常の開発時の起動順序

```bash
# 1. PostgreSQL 起動
docker start my-postgres

# 2. バックエンド起動（別ターミナル）
cd task-app && bin/rails server -p 8080

# 3. フロントエンド起動（別ターミナル）
cd front-task-app && npm run dev
```

> **現状の注意:** フロントエンドはバックエンドAPIを呼び出さず、localStorage のみで動作している。バックエンドを起動しなくてもフロントエンドの動作確認は可能。

---

## 5. DB 接続情報

| 項目 | 値 |
|---|---|
| Host | `127.0.0.1` |
| Port | `5433`（ホスト側。コンテナ内部は5432） |
| DB名 | `task_app` |
| ユーザー | `user` |
| パスワード | `password` |

`task-app/config/database.yml` に設定済み（`DB_HOST` / `DB_PORT` / `DB_USERNAME` / `DB_PASSWORD` 環境変数で上書き可能）。

---

## 6. マイグレーション（列の追加など）

Rails の標準的なマイグレーションで管理する。

```bash
cd task-app
bin/rails generate migration AddColumnToRoles some_column:string
bin/rails db:migrate
```

`db/schema.rb` が自動更新される。テスト用DB（`task_app_test`）にも同じスキーマを反映する場合は:

```bash
RAILS_ENV=test bin/rails db:schema:load
```
