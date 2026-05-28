# WeeklyCompass

Stephen Covey の「7つの習慣」に基づく第四世代タイムマネジメントアプリ。役割（ロール）ごとにタスクを管理し、週次カレンダーにスケジューリングする。

## 主な機能

- **ロール管理** — 人生の役割（仕事・家族・自己啓発など）ごとにタスクを整理
- **週次カレンダー** — タスクをドラッグ&ドロップでスケジュールに配置、リサイズで時間調整
- **永続/一時タスク** — 毎週引き継ぐ永続タスク（P）と今週だけの一時タスク（T）を区別
- **Sharpen the Saw** — Physical / Intellectual / Spiritual / Social・Emotional の4領域で自己刷新タスクを管理
- **睡眠時間** — 曜日ごとに起床・就寝時間を30分単位で記録しカレンダーに反映
- **週次・日次メモ** — その週の振り返りやメモを記録

## スクリーンショット

```
┌─────────────────┬──────────────────────────────┬──────────┐
│ Sharpen the Saw │      週次カレンダー           │ 週次メモ │
│─────────────────│  月  火  水  木  金  土  日   │          │
│ Roles and Goals │  ██                           │          │
│  ▸ Professional │       ██  ██                  │          │
│  ▸ Family       │  ██                           │          │
│  ▸ Personal     │            ██                 │          │
└─────────────────┴──────────────────────────────┴──────────┘
```

## 技術スタック

| レイヤー | 技術 |
|---|---|
| フロントエンド | Vue 3 (Options API) + TypeScript + Vite |
| バックエンド | Spring Boot 3.4.4 / Java 21 |
| ORM | MyBatis (XML マッパー) |
| DB | PostgreSQL 16 |
| 状態管理 | localStorage（バックエンド連携は実装済みだが未接続） |

## セットアップ

### 必要なもの

- Node.js 18+
- Java 21
- Docker

### 手順

```bash
# 1. リポジトリをクローン
git clone https://github.com/Shutaro-dev/task-app.git
cd task-app

# 2. PostgreSQL を Docker で起動
docker run --name my-postgres \
  -e POSTGRES_USER=user \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=task_app \
  -p 5432:5432 \
  -d postgres

# 3. DB スキーマを適用
./apply-schema.sh

# 4. バックエンドを起動（別ターミナル）
cd task-app
./gradlew bootRun

# 5. フロントエンドを起動（別ターミナル）
cd front-task-app
npm install
npm run dev
```

ブラウザで http://localhost:5173 を開く。

> **注意:** 現在フロントエンドは localStorage のみで動作するため、バックエンドがなくても利用可能。

## 使い方

### ロールの追加

1. 左サイドバー「Add Role」をクリック
2. ロール名を入力 → Add ボタンで追加
3. ロール左のカラードットをクリックしてカラーを設定

### タスクの追加とスケジューリング

1. ロール名をクリックして展開
2. 「+ Add Task」でタスクを追加（P: 永続 / T: 一時を選択）
3. タスクをカレンダーグリッドにドラッグして配置
4. 配置済みタスクの上下端をドラッグして時間を調整

### Sharpen the Saw

左上の歯車アイコンから各領域（Physical / Intellectual / Spiritual / Social・Emotional）にタスクを設定。

### 睡眠時間の記録

カレンダーヘッダーの曜日名をクリックすると睡眠時間入力ダイアログが開く。

## API エンドポイント

バックエンドは `http://localhost:8080` で動作。

| Method | Path | 概要 |
|---|---|---|
| GET | `/api/roles` | ロール一覧（タスク含む） |
| POST | `/api/roles` | ロール作成 |
| PUT | `/api/roles/{id}` | ロール更新（name / color / isExpanded） |
| DELETE | `/api/roles/{id}` | ロール削除 |
| PUT | `/api/roles/reorder` | ロール並び順更新 |
| POST | `/api/tasks` | タスク作成 |
| GET | `/api/tasks` | タスク一覧 |
| PUT | `/api/tasks/{id}` | タスク更新（title / isPermanent） |
| DELETE | `/api/tasks/{id}` | タスク削除 |
| PUT | `/api/tasks/reorder` | タスク並び順更新 |

## テスト

```bash
cd task-app
./gradlew test
```

H2 インメモリ DB を使用するため PostgreSQL 不要。

## ライセンス

MIT
