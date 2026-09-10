# CLAUDE.md — WeeklyCompass

## プロジェクト概要

**WeeklyCompass** — 7つの役割（ロール）ごとにタスクを管理し、週次カレンダーにスケジュールするタイムマネジメントアプリ。Stephen Covey の「7つの習慣」の概念（ロールと目標、Sharpen the Saw）に基づく第四世代タイムマネジメント。

GitHub: https://github.com/Shutaro-dev/task-app

## モノレポ構成

```
my-app/
├── front-task-app/   # React 19 + TypeScript + Vite (port: 5173)
├── task-app/         # Ruby on Rails 8 (API) + PostgreSQL (port: 8080)
├── startup-guide.md  # 起動手順（DB・バックエンド・フロントエンド）
├── feature-log.md    # 実装済み機能の変更ログ
├── reset-db.sh       # DB 完全リセット＆スキーマ適用
└── apply-schema.sh   # スキーマのみ適用
```

## 起動方法（簡易）

```bash
docker start my-postgres                   # DB (ホスト側ポート5433)
cd task-app && bin/rails server -p 8080    # バックエンド（別ターミナル）
cd front-task-app && npm run dev           # フロントエンド（別ターミナル）
```

カスタムコマンド `/start` でも起動手順を確認できる。詳細は [startup-guide.md](startup-guide.md) を参照。

---

## フロントエンド

2026-09 に Vue 3（Options API）から React 19 + TypeScript へ完全移行済み（見た目・挙動は無変更）。

### 状態管理の仕組み

- **関数コンポーネント + hooks**（Redux / Zustand などの外部状態管理ライブラリは使用しない）
- `Dashboard.tsx` が `useState` で全データを保持し、props（コールバック関数）で子コンポーネントに連携。子は Vue の emit に相当する `onXxx` コールバック props を呼ぶだけで、直接 state を変更しない
- **永続化は2026-09にPostgreSQL(Rails API経由)へ完全移行済み**。`localStorage`に残るのは `isListMode`（表示モード、端末ローカルな表示設定）と旧バージョンからの移行済みフラグ(`{storageKey}:migrated`)のみ。マウント時に `roleService`/`taskService`/`weekDataService`/`sharpenTheSawService`/`missionStatementService` から並行fetchして初期stateを構築し、各ハンドラは「ローカルstateを楽観的に更新 → 対応するAPIを呼ぶ」の順で動く。作成系は仮ID(`temp-${Date.now()}`)で即時描画し、サーバー応答後に本物のIDへ差し替える。詳細なデータフローは [ARCHITECTURE.md](ARCHITECTURE.md) を参照
- 高頻度更新（カレンダーのドラッグ/リサイズ、メモのキー入力）は `utils/debounce.ts` の `KeyedDebouncer` でサーバー保存だけ間引く。週切り替え・ログアウト時は `flushAll()` で保留中の書き込みを確定させる
- CSS は各コンポーネントごとの `*.module.css`（CSS Modules）で、Vue の `<style scoped>` と同等にスコープを分離。App.vue 由来のグローバルスタイルのみ `src/index.css` に残す
- ドラッグ&ドロップ: ロール/タスクの並び替えは `sortablejs`（Vue版の `vue-draggable-next` の内部実装と同一ライブラリ）、サイドバー→カレンダーへのスケジューリングはネイティブ HTML5 Drag and Drop をそのまま踏襲

### コンポーネント構成

```
App.tsx                 ← AuthProvider配下でログイン状態を見て AuthPage / Dashboard を出し分け
├── AuthPage.tsx        ← ログイン・サインアップ画面（未ログイン時）
└── Dashboard.tsx       ← 状態ハブ（useState で全データ保持・自動保存 useEffect）
    ├── LeftSidebar.tsx    ← ロール・タスク一覧、ドラッグ操作起点（SortableJS）、最下部にアカウント情報(email・ログアウト)
    ├── WeeklyCalendar.tsx ← 週次カレンダーグリッド、ドロップ＆リサイズ（mousedown/mousemove）
    ├── RightSidebar.tsx   ← ミッションステートメント要約・週次メモ
    ├── SharpenTheSawSettings.tsx ← モーダル（刷新領域タスク設定）
    └── MissionStatementModal.tsx ← モーダル（ミッションステートメント編集）
```

### 認証（`src/contexts/AuthContext.tsx` / `src/services/authService.ts`）

- Rails 側の Cookie セッション認証を利用。`AuthProvider` がマウント時に `GET /api/session` でログイン状態を確認し、`user` / `isLoading` / `login` / `signup` / `logout` を `useAuth()` で提供する
- axios は `main.tsx` で `axios.defaults.withCredentials = true` をグローバル設定済み（Cookie の送受信に必須）
- `App.tsx` は `user` が無ければ `AuthPage`、あれば `Dashboard` を表示。`Dashboard` は `key={user.id}` で別アカウント切り替え時に再マウントされ、localStorage の汚染を防ぐ
- ログアウトボタンは `LeftSidebar` 最下部のアカウントバーに配置（`.roles-section`/`.roles-list` は `flex:1` で伸縮するので、ロールが増えてスクロールしてもアカウントバーは常に最下部に固定表示される。`position: fixed` の浮き要素にすると `WeeklyCalendar` の PDF Download ボタンや `RightSidebar` の Mission Statement 見出しと衝突するため避けている）

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

`roleService.ts` / `taskService.ts` / `weekDataService.ts`（週データ本体・scheduled_tasks・day_notes）/ `sharpenTheSawService.ts` / `missionStatementService.ts` が各リソースのAPIクライアント。レスポンスのIDは数値だが、フロント側の型（`types/index.ts`）はIDを全て`string`で統一しているため、各サービスが受信時に`String()`変換してから返す。`migrationService.ts` は旧localStorageデータをDBへ一度だけ取り込む処理（Dashboard.tsxのマウント時、DB側にロールが1件も無く旧データが残っている場合のみ実行）。

バックエンドのオリジンは `apiBase.ts` の `API_ORIGIN`（環境変数 `VITE_API_BASE_URL` で上書き可能、未設定時は `http://localhost:8080` にフォールバック）に集約しており、各サービスはここから `BASE` を組み立てる。本番ビルド時はデプロイ先のバックエンドURLを `.env`（`.env.example` 参照）で設定する。

---

## バックエンド

2026-09 に Spring Boot(Java)+ MyBatis から Ruby on Rails 8(API モード)へ完全移行済み（見た目・API 契約は無変更）。

### 構成

- **Ruby on Rails 8.1 / Ruby 3.3(`--api` モード）**
- **ActiveRecord**: `app/models/role.rb` / `app/models/task.rb` / `app/models/user.rb`（DB エンティティ = MyBatis の model + mapper 相当を1つに統合）
- **PostgreSQL**: DB名 `task_app`, user: `user`, password: `password`, ホスト側ポートは **5433**（後述）
- **CORS**: `config/initializers/cors.rb` で許可オリジンを設定（`rack-cors` gem、`credentials: true` でセッションCookieを送受信可能にしている）。`ALLOWED_ORIGINS`環境変数（カンマ区切り）で上書き可能で、未設定時は開発用に`http://localhost:5173`のみ許可
- **認証**: `has_secure_password`（bcrypt）+ Rails の Cookie セッション。`config/application.rb` で API only モードでは省かれる `ActionDispatch::Cookies` / `ActionDispatch::Session::CookieStore` を明示的に追加している。`ApplicationController#current_user` / `#authenticate_user!` を各コントローラーの `before_action` で使う。セッションCookieの`same_site`は本番（フロントエンドと別ドメインになる想定）では`:none`+`secure: true`、開発では`:lax`を`Rails.env.production?`で切り替えている（`:lax`のままだと本番のクロスサイトXHRにCookieが付与されずログインが機能しないため）

### ディレクトリ構成

```
app/controllers/api/  ← REST コントローラー（roles_controller.rb / tasks_controller.rb /
                         sessions_controller.rb / users_controller.rb / mission_statement_controller.rb /
                         sharpen_the_saw_areas_controller.rb / week_data_controller.rb /
                         scheduled_tasks_controller.rb / day_notes_controller.rb）
                         Spring版の controller + service を1層に統合（Railsではこの規模で
                         別レイヤーを設けないのが一般的なため）
app/models/            ← ActiveRecord モデル（Role / Task / User / WeekData / ScheduledTask /
                         DayNote / SharpenTheSawArea / SharpenTheSawTask）
db/migrate/            ← スキーマ定義（旧 database_schema.sql から移植 + その後の追加マイグレーション）
db/schema.rb           ← 現在のDBスキーマのスナップショット（自動生成、直接編集しない）
db/seeds.rb            ← 初期データ（Sharpen the Saw areas・dev@example.com ユーザーとそのサンプルロール）
```

レスポンス/リクエストの JSON キーは Spring版の DTO と同じ camelCase（`roleId` / `roleName` /
`isExpanded` / `isPermanent` など）を各コントローラーでそのまま組み立てて維持している。

### テスト

PostgreSQL の `task_app_test` DB を使用（Rails のトランザクショナルテストで各テスト後に自動ロールバックされる）。

```bash
cd task-app && bin/rails test
```

### DB 接続とポートについて

このマシンでは Homebrew のネイティブ PostgreSQL がポート 5432 を専有しているため、
`my-postgres` コンテナはホスト側ポート **5433** にマッピングしている
（`docker run ... -p 5433:5432`）。`task-app/config/database.yml` は
`DB_HOST` / `DB_PORT` / `DB_USERNAME` / `DB_PASSWORD` 環境変数で上書き可能（デフォルトは
`127.0.0.1:5433` / `user` / `password`）。詳細は [startup-guide.md](startup-guide.md) を参照。

### 実装済み API

全エンドポイントで `authenticate_user!` が必須、`current_user` に紐づく行のみを返す・操作できる（他ユーザーのIDを指定すると404）。

| Method | Path | 状態 |
|---|---|---|
| GET | `/api/roles` | ✅ 実装済み（タスクのネストは永続タスクのみ・color・sort_order 含む） |
| POST | `/api/roles` | ✅ 実装済み（roleName・color・isExpanded） |
| PUT | `/api/roles/{id}` | ✅ 実装済み（roleName/isExpanded/colorの部分更新に対応） |
| DELETE | `/api/roles/{id}` | ✅ 実装済み |
| PUT | `/api/roles/reorder` | ✅ 実装済み |
| POST | `/api/tasks` | ✅ 実装済み（`isPermanent:false`のときは`weekStart`必須） |
| GET | `/api/tasks` | ✅ 実装済み |
| PUT | `/api/tasks/{id}` | ✅ 実装済み（title・isPermanent。永続↔一時の切替で`week_data_id`も付け替え） |
| DELETE | `/api/tasks/{id}` | ✅ 実装済み |
| PUT | `/api/tasks/reorder` | ✅ 実装済み |
| GET/PUT | `/api/mission_statement` | ✅ 実装済み |
| GET/PUT | `/api/sharpen_the_saw_areas` | ✅ 実装済み（PUTは全領域まとめての一括diff保存） |
| GET/PUT | `/api/week_data/{week_start}` | ✅ 実装済み（無ければ自動作成。scheduledTasks/dayNotes/temporaryTasksを含めて返す） |
| POST | `/api/week_data/{week_start}/scheduled_tasks` | ✅ 実装済み |
| PUT/DELETE | `/api/scheduled_tasks/{id}` | ✅ 実装済み |
| PUT | `/api/week_data/{week_start}/day_notes/{day}` | ✅ 実装済み（notes・sleepStart/sleepEndのupsert） |
| POST | `/api/users` | ✅ 実装済み（サインアップ、成功時はそのままログイン状態になる） |
| GET | `/api/session` | ✅ 実装済み（ログイン状態確認、未ログインは401） |
| POST | `/api/session` | ✅ 実装済み（ログイン） |
| DELETE | `/api/session` | ✅ 実装済み（ログアウト） |

---

## DB スキーマ（現在）

```sql
users     : id, email(一意・大小無視), password_digest, name, mission_statement, created_at, updated_at
roles     : role_id, role_name, is_expanded, color(VARCHAR7), sort_order, user_id(FK→users.id, nullable), created_at, updated_at
tasks     : id, title, role_id(FK→roles.role_id), is_permanent, sort_order, week_data_id(FK, nullable), user_id(FK→users.id, nullable), created_at, updated_at
week_data : id, week_start, weekly_notes, user_id(FK→users.id, nullable)。(user_id, week_start)の複合ユニーク
scheduled_tasks : id, week_data_id(FK), task_id(FK), role_id(FK), day(0-6), start_time, duration, completed, title
day_notes : id, week_data_id(FK), day(0-6), notes, sleep_start, sleep_end
sharpen_the_saw_areas : id(string, physical/mental/social-emotional/spiritual固定), name, icon  -- 全ユーザー共通マスタ
sharpen_the_saw_tasks : id, area_id(FK), user_id(FK→users.id), title
```

- `tasks.week_data_id` は一時タスク(`is_permanent:false`)だけがその週の`week_data`を指す。永続タスクは常に`NULL`（`on_delete: :nullify`）
- `roles` / `tasks` / `week_data` の `user_id` は既存データを壊さないよう NOT NULL 制約は付けていない（過去に作られた行は `user_id IS NULL` のまま残る点に注意。ログイン導入時に当時実データがあった場合は `dev@example.com` に付け替え済み）
- `sharpen_the_saw_areas`の4領域は`physical`/`mental`/`social-emotional`/`spiritual`固定（名称`Physical`/`Intellectual`/`Social/Emotional`/`Spiritual`）。旧Spring版由来の値(`Body`等)だった時期があり、[`FixSharpenTheSawAreasSeedData`](task-app/db/migrate/20260910151100_fix_sharpen_the_saw_areas_seed_data.rb)マイグレーションで是正済み
- 全テーブルとも既にREST APIが実装済み・フロントから実際に読み書きされている（詳細は [ARCHITECTURE.md](ARCHITECTURE.md) 参照）

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
| ミッションステートメント設定 | ✅ 完了 |
| 週次メモ・日次メモ | ✅ 完了 |
| ログイン認証（サインアップ・ログイン・ログアウト、Cookieセッション） | ✅ 完了 |
| 全データのDB永続化（旧localStorage一本化からの移行、初回ログイン時の自動インポート含む） | ✅ 完了 |

---

## コーディング規約

- **React**: 関数コンポーネント + hooks を維持。クラスコンポーネントへの移行は行わない
- **状態変更**: Dashboard の state は `setXxx` で更新するのみで、直接ミューテートしない。永続化は各ハンドラ内で対応するAPIサービス関数を呼んで行う（楽観的更新パターン。高頻度な更新は`KeyedDebouncer`で間引く）
- **新しいコールバック**: 子コンポーネントの props 型に `onXxx` を追加し、Dashboard 側で実装して渡す（Vue の emits 配列 + リスナー登録に相当）
- **コメント**: 理由が非自明なもののみ記述。何をするかの説明は書かない
- **型**: `any` を避け、`types/index.ts` の型を使う
- **CSS**: 新しいコンポーネントを追加する場合は `ComponentName.module.css` を作成し `styles` オブジェクト経由でクラスを参照する（他コンポーネントと同名クラスがあってもスコープが分離され衝突しない）

## 注意事項

- `LeftSidebar` の `mergedRoles` は `useMemo` の派生値なので直接変更不可。props のコールバックで Dashboard に委譲する
- `role.tasks` は `isPermanent: true` のタスクのみ保持（`RolesController#role_json`がサーバー側でフィルタ済み）。一時タスクは`WeekData.temporaryTasks`（`GET /api/week_data/:week_start`のレスポンス）由来
- 作成直後のロール/タスク/スケジュール済みタスクは仮ID(`temp-`プレフィックス)で描画されるため、サーバー採番ID反映前にそれらへ追加更新をかける処理では`id.startsWith('temp-')`のガードが要る（`Dashboard.tsx`の各ハンドラを参照）
- WeeklyCalendar の `.scheduled-task` CSS に `background-color` の固定値を残すと inline style が負けるので注意
- SharpenTheSaw の4領域: Physical / Social・Emotional / Spiritual / **Intellectual**（旧: Mental）
- ブラウザタブタイトル: **WeeklyCompass**（`front-task-app/index.html`）
- 開発用ログイン: `db/seeds.rb` で `dev@example.com` / `password123` を作成する（`bin/rails db:seed`）
- ローカルDBで `roles` テーブルのシーケンスが `roles_role_id_seq`（Railsの標準命名）以外になっていると `db:schema:load`（`bin/rails test` のテストDB作成含む）が `relation "roles_id_seq" does not exist` で失敗する。もし発生したら `ALTER SEQUENCE <実際の名前> RENAME TO roles_role_id_seq;` を実行してから `bin/rails db:schema:dump` で schema.rb を作り直す
- 認証必須になった `roles` / `tasks` の統合テストは `test/test_helper.rb` の `create_and_sign_in_user` でセッションCookieを確立してから叩く。素の `Role.create!` / `Task.create!` は `user: @user` を渡さないと `current_user.roles` / `current_user.tasks` 経由の検索に引っかからず404になる
