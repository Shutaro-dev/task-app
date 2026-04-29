# 必要なAPI一覧

第4世代タイムマネジメントアプリ（Spring Boot + Vue 3 + PostgreSQL）

## 現在の実装状況

| メソッド | エンドポイント | 状態 |
|---------|-------------|------|
| POST | /api/tasks | 実装済み |
| POST | /api/roles | 実装済み |
| その他全て | - | **未実装** |

フロントエンドは現在ローカルストレージで独立動作中。以下の全APIを実装することで、バックエンドDB（PostgreSQL）との連携が実現する。

---

## 優先度：高（フロント連携の核心）

### ロール API（roles テーブル）

---

#### GET /api/roles — 全ロール取得

フロントの `LeftSidebar.vue` がロール一覧と配下タスクを表示するために必要。

**レスポンス（200 OK）**
```json
[
  {
    "roleId": 1,
    "roleName": "Professional",
    "isExpanded": true,
    "tasks": [
      {
        "taskId": 1,
        "title": "Weekly report",
        "roleId": 1,
        "isPermanent": true
      }
    ]
  }
]
```

**備考**
- `tasks` 配列を含めた形で返す（N+1問題に注意し、JOIN で一括取得）
- フロントの `Role` 型の `id` は string だが、バックエンドは Integer。レスポンスで統一する

---

#### PUT /api/roles/{id} — ロール更新

ロール名の変更 / サイドバーの開閉状態（isExpanded）保存に使用。

**リクエスト**
```json
{
  "roleName": "Professional",
  "isExpanded": false
}
```

**レスポンス（200 OK）**
```json
{
  "roleId": 1,
  "roleName": "Professional",
  "isExpanded": false
}
```

---

#### DELETE /api/roles/{id} — ロール削除

**レスポンス（204 No Content）**

**備考**
- `tasks` テーブルは `role_id` に `ON DELETE CASCADE` が設定済みのため、紐づくタスクは自動削除される
- `scheduled_tasks` にも `role_id` の CASCADE 設定あり

---

### タスク API（tasks テーブル）

---

#### GET /api/tasks — 全タスク取得

**クエリパラメータ（省略可）**
- `?roleId=1` — 特定ロールのタスクのみ取得

**レスポンス（200 OK）**
```json
[
  {
    "taskId": 1,
    "title": "Weekly report",
    "roleId": 1,
    "isPermanent": true
  }
]
```

**備考**
- 現在 `TaskMapper.xml` の `findAll` は `role_id`, `is_permanent`, `created_at`, `updated_at` が SELECT 句から漏落しているため要修正

---

#### PUT /api/tasks/{id} — タスク更新

タスクのタイトル変更・ロール変更・永続フラグ変更に使用。

**リクエスト**
```json
{
  "title": "Weekly report (updated)",
  "roleId": 1,
  "isPermanent": false
}
```

**レスポンス（200 OK）**
```json
{
  "taskId": 1,
  "title": "Weekly report (updated)",
  "roleId": 1,
  "isPermanent": false
}
```

---

#### DELETE /api/tasks/{id} — タスク削除

**レスポンス（204 No Content）**

**備考**
- `scheduled_tasks` テーブルは `task_id` に `ON DELETE CASCADE` 設定済みのため、紐づくスケジュールも自動削除される

---

### 週間データ API（week_data テーブル）

---

#### GET /api/week-data/{weekStart} — 週データ取得

`WeeklyCalendar.vue` が週を切り替えるたびに呼び出す。`weekStart` は ISO 8601 形式（例：`2025-04-14`）。

**レスポンス（200 OK）**
```json
{
  "id": 1,
  "weekStart": "2025-04-14",
  "weeklyNotes": "今週の目標...",
  "scheduledTasks": [
    {
      "id": 1,
      "taskId": 2,
      "day": 0,
      "startTime": "09:00",
      "duration": 60,
      "title": "Weekly report",
      "roleId": 1
    }
  ],
  "dayNotes": [
    {
      "id": 1,
      "day": 0,
      "notes": "今日は...",
      "sleepStart": "23:00",
      "sleepEnd": "07:00"
    }
  ]
}
```

**備考**
- 週データが存在しない場合は `404 Not Found` を返す。フロントは POST で新規作成する

---

#### POST /api/week-data — 週データ新規作成

新しい週を初めて開いたときに呼び出す。

**リクエスト**
```json
{
  "weekStart": "2025-04-21",
  "weeklyNotes": ""
}
```

**レスポンス（201 Created）**
```json
{
  "id": 2,
  "weekStart": "2025-04-21",
  "weeklyNotes": "",
  "scheduledTasks": [],
  "dayNotes": []
}
```

---

#### PUT /api/week-data/{id}/notes — 週間メモ更新

`RightSidebar.vue` の週間メモ欄を保存するときに使用。

**リクエスト**
```json
{
  "weeklyNotes": "今週の振り返り..."
}
```

**レスポンス（200 OK）**
```json
{
  "id": 1,
  "weeklyNotes": "今週の振り返り..."
}
```

---

### スケジュール済みタスク API（scheduled_tasks テーブル）

---

#### POST /api/scheduled-tasks — スケジュールへタスク登録

`WeeklyCalendar.vue` でタスクをカレンダーにドラッグ&ドロップしたときに呼び出す。

**リクエスト**
```json
{
  "weekDataId": 1,
  "taskId": 2,
  "day": 0,
  "startTime": "09:00",
  "duration": 60,
  "title": "Weekly report",
  "roleId": 1
}
```

**レスポンス（201 Created）**
```json
{
  "id": 5,
  "weekDataId": 1,
  "taskId": 2,
  "day": 0,
  "startTime": "09:00",
  "duration": 60,
  "title": "Weekly report",
  "roleId": 1
}
```

---

#### PUT /api/scheduled-tasks/{id} — スケジュール更新

カレンダー上でタスクをドラッグして時間・日付を変更したときに使用。

**リクエスト**
```json
{
  "day": 1,
  "startTime": "10:00",
  "duration": 90
}
```

**レスポンス（200 OK）**
```json
{
  "id": 5,
  "day": 1,
  "startTime": "10:00",
  "duration": 90
}
```

---

#### DELETE /api/scheduled-tasks/{id} — スケジュール削除

カレンダーからタスクを取り除くときに使用。`taskService.ts` にすでに実装済み（有効化待ち）。

**レスポンス（204 No Content）**

---

## 優先度：中

### 日ごとのメモ API（day_notes テーブル）

---

#### POST /api/day-notes — 日記・睡眠時間登録

その日の初回保存時に呼び出す。

**リクエスト**
```json
{
  "weekDataId": 1,
  "day": 0,
  "notes": "今日は集中できた",
  "sleepStart": "23:30",
  "sleepEnd": "07:00"
}
```

**レスポンス（201 Created）**
```json
{
  "id": 3,
  "weekDataId": 1,
  "day": 0,
  "notes": "今日は集中できた",
  "sleepStart": "23:30",
  "sleepEnd": "07:00"
}
```

---

#### PUT /api/day-notes/{id} — 日記・睡眠時間更新

既存レコードの更新。`day_notes` テーブルは `(week_data_id, day)` に UNIQUE 制約があるため、2回目以降は必ず PUT を使う。

**リクエスト**
```json
{
  "notes": "今日は集中できた（更新）",
  "sleepStart": "23:00",
  "sleepEnd": "06:30"
}
```

**レスポンス（200 OK）**
```json
{
  "id": 3,
  "day": 0,
  "notes": "今日は集中できた（更新）",
  "sleepStart": "23:00",
  "sleepEnd": "06:30"
}
```

---

#### GET /api/day-notes?weekDataId={id} — 週の日記一括取得

週データ取得（GET /api/week-data/{weekStart}）のレスポンスに含める形でも取得可能だが、個別取得が必要な場合のために実装する。

**レスポンス（200 OK）**
```json
[
  {
    "id": 3,
    "weekDataId": 1,
    "day": 0,
    "notes": "今日は...",
    "sleepStart": "23:00",
    "sleepEnd": "06:30"
  }
]
```

---

### Sharpen the Saw API（sharpen_the_saw_areas / sharpen_the_saw_tasks テーブル）

---

#### GET /api/sharpen-the-saw-areas — 全エリア取得（タスク含む）

`SharpenTheSawSettings.vue` が設定ダイアログを開いたときに呼び出す。初期データは DB に INSERT 済み（Body / Intelligence / Social・Emotional / Mental）。

**レスポンス（200 OK）**
```json
[
  {
    "id": "Body",
    "name": "Body",
    "icon": "💪",
    "tasks": [
      {
        "id": 1,
        "areaId": "Body",
        "title": "30分ウォーキング"
      }
    ]
  }
]
```

---

#### POST /api/sharpen-the-saw-areas/{areaId}/tasks — エリアへタスク追加

**リクエスト**
```json
{
  "title": "30分ウォーキング"
}
```

**レスポンス（201 Created）**
```json
{
  "id": 1,
  "areaId": "Body",
  "title": "30分ウォーキング"
}
```

---

#### DELETE /api/sharpen-the-saw-areas/{areaId}/tasks/{taskId} — エリアのタスク削除

**レスポンス（204 No Content）**

---

## 既存コードの不具合（API実装前に修正が必要）

### 1. TaskMapper.xml の SELECT 句が不完全

**ファイル:** `task-app/src/main/resources/mapper/TaskMapper.xml`

`findAll` と `findById` のクエリで以下のカラムが SELECT 句から漏落している：
- `role_id`
- `is_permanent`
- `created_at`
- `updated_at`

→ 現状、タスク取得 API を呼んでも `roleId` と `isPermanent` が null になる

### 2. Role モデルにフィールドが不足

**ファイル:** `task-app/src/main/java/com/example/task_app/model/Role.java`

DBスキーマの `roles` テーブルには `is_expanded`, `created_at`, `updated_at` があるが、Java モデルクラスに定義されていない。

→ `isExpanded` を返すレスポンスが作れない（フロントは `isExpanded` を必要としている）

### 3. フロント（string型）vs バック（Integer型）の ID 型不一致

**ファイル:** `front-task-app/src/types/index.ts`

フロントの型定義は `id: string`, `roleId: string` だが、バックエンドは `Integer`。
→ API 連携時はバックエンドで数値を返し、フロント側で string へのキャスト（`String(id)`）を行う

---

## 実装の推奨順序

```
Phase 1（高優先度 / フロント連携の核心）
  1.  GET    /api/roles                         ← 既存不具合修正も含む
  2.  PUT    /api/roles/{id}
  3.  DELETE /api/roles/{id}
  4.  GET    /api/tasks
  5.  PUT    /api/tasks/{id}
  6.  DELETE /api/tasks/{id}
  7.  GET    /api/week-data/{weekStart}
  8.  POST   /api/week-data
  9.  PUT    /api/week-data/{id}/notes
  10. POST   /api/scheduled-tasks
  11. PUT    /api/scheduled-tasks/{id}
  12. DELETE /api/scheduled-tasks/{id}

Phase 2（中優先度）
  13. POST   /api/day-notes
  14. PUT    /api/day-notes/{id}
  15. GET    /api/day-notes?weekDataId={id}
  16. GET    /api/sharpen-the-saw-areas
  17. POST   /api/sharpen-the-saw-areas/{areaId}/tasks
  18. DELETE /api/sharpen-the-saw-areas/{areaId}/tasks/{taskId}

Phase 3（フロントエンド連携）
  19. taskService.ts / roleService.ts のコメントアウト解除
  20. localStorage → API への段階的な移行
```
