# CLAUDE.md — WeeklyCompass

## プロジェクト概要

**WeeklyCompass** — 7つの役割（ロール）ごとにタスクを管理し、週次カレンダーにスケジュールするタイムマネジメントアプリ。Stephen Covey の「7つの習慣」の概念（ロールと目標、Sharpen the Saw）に基づく第四世代タイムマネジメント。

GitHub: https://github.com/Shutaro-dev/task-app

## モノレポ構成

```
my-app/
├── front-task-app/   # Vue 3 + TypeScript + Vite (port: 5173)
├── task-app/         # Spring Boot 3.4.4 + MyBatis + PostgreSQL (port: 8080)
├── startup-guide.md  # 起動手順（DB・バックエンド・フロントエンド）
├── feature-log.md    # 実装済み機能の変更ログ
├── reset-db.sh       # DB 完全リセット＆スキーマ適用
└── apply-schema.sh   # スキーマのみ適用
```

## 起動方法（簡易）

```bash
docker start my-postgres                   # DB
cd task-app && ./gradlew bootRun           # バックエンド（別ターミナル）
cd front-task-app && npm run dev           # フロントエンド（別ターミナル）
```

カスタムコマンド `/start` でも起動手順を確認できる。詳細は [startup-guide.md](startup-guide.md) を参照。

---

## フロントエンド

### 状態管理の仕組み

- **Options API**（Composition API / Pinia は使用しない）
- `Dashboard.vue` が全データを保持し、props/emit で子コンポーネントに連携
- 永続化は `localStorage`（キー: `fourth-gen-time-management`）のみ。バックエンド API 呼び出しは現在コメントアウト済み

### コンポーネント構成

```
Dashboard.vue          ← 状態ハブ（全データ保持・saveData/loadData）
├── LeftSidebar.vue    ← ロール・タスク一覧、ドラッグ操作起点
├── WeeklyCalendar.vue ← 週次カレンダーグリッド、ドロップ＆リサイズ
├── RightSidebar.vue   ← 週次メモ
└── SharpenTheSawSettings.vue ← モーダル（刷新領域タスク設定）
```

### タスクの種別

- **永続タスク（Permanent, P）**: `Role.tasks[]` に保存。週をまたいで引き継がれる
- **一時タスク（Temporary, T）**: `WeekData.temporaryTasks[]` に保存。その週限り

### 主な型定義（`src/types/index.ts`）

```typescript
Role          // id, name, tasks, isExpanded, color?, showAddTask?
Task          // id, title, roleId, isPermanent
ScheduledTask // id, taskId, day(0-6), startTime(HH:MM), duration(分), title, roleId
DayNotes      // day, notes, sleepStart?, sleepEnd?
WeekData      // weekStart, scheduledTasks, dayNotes, weeklyNotes, temporaryTasks?
```

### サービス層（`src/services/`）

`roleService.ts` / `taskService.ts` にAPIクライアント関数を定義するが、現在は Dashboard.vue から直接呼ばれていない。API 統合時はここに関数を追加し、Dashboard のメソッドから呼び出す。

---

## バックエンド

### 構成

- **Spring Boot 3.4.4 / Java 21**
- **MyBatis**（XML マッパー）: `src/main/resources/mapper/*.xml`
- **PostgreSQL**: DB名 `task_app`, user: `user`, password: `password`
- **CORS**: `http://localhost:5173` を許可済み

### パッケージ構成

```
controller/  ← REST コントローラー
service/     ← ビジネスロジック
mapper/      ← MyBatis インターフェース
model/       ← DB エンティティ
dto/         ← サービス層の受け渡し
form/        ← リクエストのバリデーション
response/    ← レスポンス整形
```

### テスト

H2 インメモリ DB を使用。PostgreSQL が起動していなくても実行可能。

```bash
cd task-app && ./gradlew test
```

### 実装済み API

| Method | Path | 状態 |
|---|---|---|
| GET | `/api/roles` | ✅ 実装済み（タスクのネスト・color・sort_order 含む） |
| POST | `/api/roles` | ✅ 実装済み |
| PUT | `/api/roles/{id}` | ✅ 実装済み（color フィールド含む） |
| DELETE | `/api/roles/{id}` | ✅ 実装済み |
| PUT | `/api/roles/reorder` | ✅ 実装済み |
| POST | `/api/tasks` | ✅ 実装済み |
| GET | `/api/tasks` | ✅ 実装済み |
| PUT | `/api/tasks/{id}` | ✅ 実装済み（title・isPermanent） |
| DELETE | `/api/tasks/{id}` | ✅ 実装済み |
| PUT | `/api/tasks/reorder` | ✅ 実装済み |

---

## DB スキーマ（現在）

```sql
roles  : role_id, role_name, is_expanded, color(VARCHAR7), sort_order, created_at, updated_at
tasks  : id, title, role_id(FK→roles.role_id), is_permanent, sort_order, created_at, updated_at
```

その他テーブル: `sharpen_the_saw_areas`, `sharpen_the_saw_tasks`, `week_data`, `scheduled_tasks`, `day_notes`

---

## 実装済み機能

| 機能 | 状態 |
|---|---|
| 起床・就寝時間を30分単位に | ✅ 完了 |
| ロールカラー設定 + カレンダー反映 | ✅ 完了 |
| タスク名インライン編集 | ✅ 完了 |
| ロール・タスクのドラッグ並び替え | ✅ 完了 |
| 永続タスク ↔ 一時タスク切り替え | ✅ 完了 |
| Sharpen the Saw（4領域）設定 | ✅ 完了 |
| 週次メモ・日次メモ | ✅ 完了 |

---

## コーディング規約

- **Vue**: Options API を維持。Composition API / `<script setup>` への移行は行わない
- **状態変更**: 必ず `saveData()` を末尾で呼ぶ
- **新しい emit**: 子コンポーネントの `emits` 配列に追加し、Dashboard 側でリスナーを登録する
- **コメント**: 理由が非自明なもののみ記述。何をするかの説明は書かない
- **型**: `any` を避け、`types/index.ts` の型を使う

## 注意事項

- `mergedRoles` は computed のコピーなので直接変更不可。emit で Dashboard に委譲する
- `role.tasks` は `isPermanent: true` のタスクのみ保持。`changeWeek` 時に false は削除される
- localStorage の旧データには `color` / `sort_order` がない場合がある。`|| デフォルト値` でフォールバックする
- WeeklyCalendar の `.scheduled-task` CSS に `background-color` の固定値を残すと inline style が負けるので注意
- SharpenTheSaw の4領域: Physical / Social・Emotional / Spiritual / **Intellectual**（旧: Mental）
- ブラウザタブタイトル: **WeeklyCompass**（`front-task-app/index.html`）
