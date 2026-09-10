# 機能追加ログ

## 2026-05-27

### フロントエンド

#### ロール・タスクのドラッグ並び替え
- `vue-draggable-next`（SortableJS ラッパー）を導入
- ロール一覧をドラッグハンドル（`⠿`）でドラッグして並び替えできるようにした
- タスク一覧も同じ仕組みで並び替えできるようにした
- ドラッグ中はゴースト要素（薄い青点線枠）でプレースホルダーを表示
- カレンダーへのドラッグと並び替えドラッグをハンドル経由で区別し、競合を解消

#### ロールカラー設定
- ロールごとにカラーパレットから色を設定できるようにした
- 設定した色はロールヘッダーのインジケーターに反映される
- カラーパレットのポップオーバーが下の要素を透過してしまう z-index バグを修正（`.role-item` の `will-change: transform` 削除）

#### 編集UIの改善
- タスク行にホバーすると編集ボタン（✏️）が表示されるようにした（常時表示をやめ視覚ノイズを低減）
- ロールのクリックで開閉するシェブロン（`›`）を追加し、展開状態を回転アニメーションで明示した

#### データ破損バグの修正
- ロール並び替え時に `mergedRoles`（永続タスク＋一時タスク）をそのまま `role.tasks` に書き戻してしまい、タスク数が指数的に増殖する問題を修正
  - `reorderRoles` を既存の `this.roles` から ID で引き当てる方式に変更
  - `loadData` にて `isPermanent: false` のタスクの除去と重複 ID の排除を追加
  - `mounted` で `loadData` 直後に `saveData` を呼び、破損データを即時クリーンアップ

---

### バックエンド

#### タスク API の追加実装
| メソッド | パス | 内容 |
|---|---|---|
| `GET` | `/api/tasks` | タスク一覧取得 |
| `PUT` | `/api/tasks/{id}` | タスク更新（title・isPermanent） |
| `DELETE` | `/api/tasks/{id}` | タスク削除 |
| `PUT` | `/api/tasks/reorder` | タスク並び順一括更新 |

#### ロール API の追加実装
| メソッド | パス | 内容 |
|---|---|---|
| `PUT` | `/api/roles/reorder` | ロール並び順一括更新 |

- `PUT /api/roles/{id}` に `color` フィールドを追加（既存 API を拡張）
- `RoleDto` / `RoleResponse` / `RoleService.updateRole` に `color` 引数を追加

---

### テスト

- `RoleServiceTest`: color の保存・取得・更新、reorderRoles のテストを追加
- `TaskServiceTest`: reorderTasks のテストを追加（sort_order の DB 反映、空リストの安全性）
- `RoleRestControllerTest`: color フィールドの POST/GET/PUT、PUT `/reorder` エンドポイントのテストを追加
- `TaskRestControllerTest`: PUT `/api/tasks/reorder` エンドポイントのテストを追加
- 上記4ファイルについて、`RoleDto` / `RoleResponse` / `updateRole` のシグネチャ変更に伴うコンパイルエラーをすべて修正

## 2026-09-06

### フロントエンド

#### Vue 3 → React 19 完全移行
- `front-task-app` のフロントエンドを Vue 3(Options API)から React 19 + TypeScript へ全面的に書き換え。見た目・操作性・localStorage のデータスキーマ(キー: `fourth-gen-time-management`)は完全に維持
- **状態管理**: `Dashboard.tsx` が `useState`/`useEffect` で全データを保持する単一ハブ構成に置き換え(Vue版の「Dashboard が全データを保持し props/emit で連携」という設計方針は維持)
  - 自動保存は `[roles, sharpenTheSawAreas, isListMode, currentWeek, weekData]` を依存配列とする `useEffect` に一本化(Vue版で各メソッド末尾に書かれていた `saveData()` 呼び出しの代替)
  - 初回読み込みは `useState` の lazy initializer で同期的に実行し、デフォルト値のちらつきを防止
- **CSS**: 各コンポーネントの `<style scoped>` を `*.module.css`(CSS Modules)に変換。同名クラス(`.task-item` 等)が複数コンポーネントで衝突しないよう分離。SortableJS のゴーストクラス(`.role-ghost` / `.task-ghost`)のみ `index.css` にグローバル定義
- **ドラッグ&ドロップ**: `vue-draggable-next` を `sortablejs` の直接利用に置き換え(内部実装は同一ライブラリのため挙動は変化なし)。ロール展開時のみ生成されるタスク一覧の Sortable インスタンスは roleId ごとにコールバック参照をキャッシュし、無関係な再レンダーで破棄・再生成されないようにした
- カレンダーのマウスドラッグ(移動・リサイズ)・右クリックコンテキストメニュー・睡眠時間ダイアログ・リストモードの並び替えは全てロジックをそのまま移植
- PDF ダウンロード機能(`html2canvas` + `jspdf`)はそのまま流用
- Playwright を用いた手動スモークテストで、ロール/タスク表示、ドラッグでの時間変更、リストモード切り替えがコンソールエラーなく動作することを確認
- 削除: `App.vue` / `Dashboard.vue` / `LeftSidebar.vue` / `RightSidebar.vue` / `SharpenTheSawSettings.vue` / `WeeklyCalendar.vue`、`vue` / `vue-draggable-next` / `vue-tsc` 等の依存関係
- 追加: `react` / `react-dom` / `sortablejs` 等の依存関係、`@vitejs/plugin-react`

---

### バックエンド

#### Spring Boot → Ruby on Rails 完全移行
- `task-app` のバックエンドを Spring Boot 3.4.4(Java 21)+ MyBatis から Ruby on Rails 8.1(`--api` モード、Ruby 3.3)へ全面的に書き換え。API契約(エンドポイント・リクエスト/レスポンスのJSONキー名・ステータスコード)と DB スキーマ(`task_app`)は無変更
- **アーキテクチャ**: Spring版の `controller` + `service` + `mapper` + `model` の4層を、Rails では `app/controllers/api/roles_controller.rb` / `tasks_controller.rb`(コントローラー)と `app/models/role.rb` / `task.rb`(ActiveRecord モデル)の2層に統合。この規模のCRUDでは Rails 流に別レイヤーを設けない方が自然なため
- **ルーティング**: `namespace :api do resources :roles/:tasks ... end` で `/api/roles`・`/api/tasks` 以下を再現。`PUT /api/roles/reorder` 等は `collection do put :reorder end` で `/:id` ルートと衝突しないよう定義
- **レスポンス整形**: `RoleResponse`/`TaskResponse` DTO 相当のハッシュを各コントローラーで組み立て、`roleId`/`roleName`/`isExpanded`/`isPermanent` 等の camelCase キーをそのまま維持
- **バリデーション/エラー処理**: `@NotBlank`/`@NotNull` による 400、`NoSuchElementException` による 404、Content-Type 未指定時の 415 を `ApplicationController` の `rescue_from` と `before_action` で再現
- **DBスキーマ移植**: 旧 `database_schema.sql`(7テーブル)を `db/migrate/*.rb` に移植。移植時、未使用テーブル `scheduled_tasks.role_id` が本来存在しない `roles(id)` を参照していた既存バグ(`roles` の主キーは `role_id`)を `roles(role_id)` に修正(現行APIからは触れられないテーブルのため挙動への影響なし)
- **テスト**: JUnit の Service/Controller テスト(計53ケース相当)を `test/controllers/api/roles_controller_test.rb`・`tasks_controller_test.rb` のリクエストテスト(Minitest、実DB使用)として移植し、全件成功を確認。Rails のトランザクショナルテストにより各テスト後は自動ロールバックされる
- **ローカル環境の問題を解消**: 開発機ではネイティブ(Homebrew)PostgreSQLがポート5432を専有しており、`my-postgres` Dockerコンテナ(バックエンド用DB)に接続できない状態だった。コンテナをホスト側ポート **5433** に再マッピングして解消(既存のデータボリュームはそのまま引き継ぎ、格納されていた実データ(ロール「エンジニア」等)は損失なし)
- ドキュメント更新: `CLAUDE.md`・`startup-guide.md`・`reset-db.sh`・`apply-schema.sh`・`.claude/commands/{start,reset-db,test}.md` を Rails 版の手順・ポート番号(5433)に更新

## 2026-09-07

### フロントエンド

#### デザイン全面刷新
- `front-task-app` 全体のCSSを1px単位で見直し、シンプルでモダンな見た目に刷新。TSX(ロジック・DOM構造・機能)は無変更
- `index.css` にデザイントークン(色・角丸・影・余白のスケール、`--bg`/`--surface`/`--accent`等のCSS変数)を導入し、全 `*.module.css` から参照する形に統一
- Inter フォントを `index.html` から読み込み、システムフォントのフォールバックと併用
- ボタン/inputのフォーカスリング、スクロールバー、SortableJSのゴースト表示(`.role-ghost`/`.task-ghost`)も刷新
- カレンダーのタイムライン計算(`hourHeight = 26`、タイムライン高さ`520px`、時間軸カラム幅`70px`)に直結する数値は`WeeklyCalendar.tsx`のJS実装と一致させたまま完全維持し、ドラッグ・リサイズの挙動に影響がないことを確認
- `npm run build` 成功、Playwrightでの手動スモークテスト(ロール展開、タスクのドラッグ、リストモード切替、カラーピッカー、Sharpen the Saw設定モーダル)で挙動に変化がないことを確認

#### リストモードのメモ列ズレ修正
- リストモードで `time-column-header`/`time-slots-column`(時間軸の70px列)が非表示になる一方、メモ行の `notes-time-label`(「Notes」ラベルの70px列)だけ常に表示されたままだったため、メモ入力欄が日付列に対して70px右にズレていた
- `WeeklyCalendar.tsx` で `notes-time-label` も `isListMode` 時は非表示にし、ヘッダー行・タイムライン行と同じ列グリッドに揃えて解消

#### ミッションステートメント機能を追加
- 右サイドバー最上部(Weekly Notesの上)に Mission Statement の要約パネルを新設
  - 未設定時は「+ ミッションステートメントを設定」ボタンを表示
  - 設定済みの場合は3行までのプレビュー(アクセントカラーの左ボーダー付きの引用風カード)を表示し、クリックで編集モーダルを開く
  - 当初は左サイドバー最上部(Sharpen the Sawの上)に配置していたが、週次の振り返り・計画という性質が近い Weekly Notes の隣(右サイドバー)に移設
- 編集は `SharpenTheSawSettings` と同様の独立モーダルで行う(新規 `MissionStatementModal.tsx` / `MissionStatementModal.module.css`)
- データは `Dashboard.tsx` のトップレベル state(`missionStatement: string`)として保持し、`localStorage`(キー: `fourth-gen-time-management`)に他の永続データ(`roles`・`sharpenTheSawAreas`等)と同じ自動保存 `useEffect` で保存・復元
- 既存の保存データに `missionStatement` が無い場合(過去バージョンのデータ)は空文字にフォールバックする後方互換処理を追加
- `CLAUDE.md` の状態管理の説明(自動保存 `useEffect` の依存配列)・コンポーネント構成図・実装済み機能表を更新

#### ログイン認証機能を追加
- **バックエンド**: `User` モデル(`has_secure_password` + bcrypt)、`users` テーブルを追加。API onlyモードで省かれる Cookie/セッションミドルウェアを `config/application.rb` で明示的に有効化し、Rails標準の Cookie セッション認証を実装
  - `POST /api/users`(サインアップ、成功時はログイン状態になる)、`GET/POST/DELETE /api/session`(状態確認・ログイン・ログアウト)を新設
  - `roles`・`tasks` に `user_id` を追加し、`RolesController`/`TasksController` を `authenticate_user!` 必須にした上で `current_user` にスコープ(他ユーザーのIDを指定すると404)。`week_data` にも `user_id` と `(user_id, week_start)` の複合ユニーク制約を追加(APIは未実装のため先行対応)
  - 既存の実データ(ログイン導入前に作られたロール等)は失わないよう seed の `dev@example.com` ユーザーに紐付け直し
  - 副次的に見つかった pre-existing のDBドリフト(`roles`テーブルのシーケンス名不一致で `bin/rails test` が全滅する不具合)を修正
  - `roles_controller_test.rb`/`tasks_controller_test.rb` を認証必須の挙動に追随させ、`sessions_controller_test.rb`/`users_controller_test.rb` を新規追加。全73件成功
- **フロントエンド**: `AuthContext`/`authService.ts` で Cookie 認証状態(`user`/`isLoading`/`login`/`signup`/`logout`)を管理し、`axios.defaults.withCredentials = true` をグローバル設定
  - 未ログイン時は `AuthPage`(ログイン/サインアップ切り替え)を表示、ログイン後は `Dashboard` を表示。`Dashboard` は `key={user.id}` で別アカウント切り替え時に再マウントされる
  - `localStorage` キーをユーザーIDで分離(`fourth-gen-time-management:{userId}`)し、同一ブラウザで複数アカウントを使っても週次データが混ざらないようにした
  - ログアウトボタンは `LeftSidebar` 最下部のアカウントバーに配置。当初 `position: fixed` の浮きバッジで実装したところ、実機確認で `WeeklyCalendar` の PDF Download ボタンや `RightSidebar` の Mission Statement 見出しと重なるバグを発見し、`.roles-list`(`flex:1`)の下に通常のレイアウトフローで組み込む形に修正(ロールが増えてスクロールしても常に最下部に固定表示される)
  - Playwrightでの手動スモークテスト(未ログイン表示・サインアップ・ログアウト・誤パスワードエラー・ロール多数時のレイアウト崩れ確認)を実施
- 開発用ログイン: `db/seeds.rb` で `dev@example.com` / `password123` を作成
- `CLAUDE.md` に認証の仕組み・新規API・DBスキーマ変更・注意事項を反映

#### 新規登録ユーザー向けオンボーディングツアーを追加
- サインアップ直後だけ自動起動する、スポットライト+吹き出し形式の機能紹介ツアーを新設(通常のログインでは起動しない)
- `src/onboarding/tourSteps.ts` に7ステップを定義: 導入 → Roles and Goals → Sharpen the Saw → カレンダーへのドラッグ&ドロップ → リスト表示切替 → ミッションステートメント → 終了
- `src/components/OnboardingTour.tsx`/`.module.css` が本体。`box-shadow: 0 0 0 9999px`のスプレッドでスポットライト(暗幕に穴を開ける定番テクニック)を実現し、対象要素の矩形から吹き出しの表示位置を計算してビューポート内にクランプする。戻る/次へ/スキップ、Escで閉じる・矢印キーで前後移動、`role="dialog"`+ステップ切り替え時のフォーカス移動、`prefers-reduced-motion`時のアニメーション無効化に対応
- ツアー中は全画面オーバーレイ(`pointer-events: auto`)が背後のクリックを吸収するため、誤操作でロール追加やタスク削除などが走らない
- 各ステップの対象要素は `LeftSidebar`(`roles-section`/`sharpen-summary`)・`WeeklyCalendar`(`calendar-grid`/`list-mode-toggle`)・`RightSidebar`(`mission-summary`)に付与した `data-tour` 属性で特定
- `AuthContext` に `justSignedUp`/`consumeJustSignedUp` を追加。`signup()` 成功時のみ true になり、`Dashboard` がマウント時に一度だけ消費してツアーを起動する(ページ再読み込みや別ログインでは再起動しない、一過性のReact state)
- いつでも見返せるよう `LeftSidebar` のアカウントバーに「使い方ツアーを見る」ボタン(`bi-signpost-split`)を追加し、手動でも再生可能にした
- UI文言は既存の「紙とインクの編集的なトーン」に合わせ、進捗バーは罫線風のヘアラインで表現
- 実装前に一般的なオンボーディングUXのベストプラクティス(コーチマーク+スポットライト、いつでもスキップ可能にする、ステップ数を絞る等)を調査した上で設計
- Playwright(`chromium`)でサインアップ→全7ステップの遷移・戻るボタン・スキップ(Xボタン/Escキー)・完了後のダッシュボード操作性・アカウントバーからの手動再生・既存ユーザーの通常ログイン時に自動起動しないことを実機確認

### 共通

#### デプロイ品質の調査とバックエンド接続まわりの修正
- デプロイ可能な品質か調査したところ、本番デプロイ後にバックエンド接続が機能しなくなる不具合を3件発見・修正した
- **フロントエンドのAPI接続先が決め打ちだった不具合**: `authService.ts`/`roleService.ts`/`taskService.ts` の3ファイルすべてで `const BASE = 'http://localhost:8080/...'` とハードコードされており、どこにデプロイしてもユーザー自身のPCの`localhost:8080`を叩こうとして必ず失敗する状態だった。新設の `src/services/apiBase.ts`(`API_ORIGIN`、環境変数 `VITE_API_BASE_URL` で上書き可能・未設定時は `http://localhost:8080` にフォールバック)に接続先を集約し、3ファイルはそこから `BASE` を組み立てるように修正。`.env.example` を追加し、`.gitignore` で実際の `.env` はコミットされないようにした
- **CORSが本番フロントエンドのオリジンを許可していなかった不具合**: `config/initializers/cors.rb` が `http://localhost:5173` 固定だったため、デプロイ後のフロントエンドドメインからのリクエストが常にCORSでブロックされる状態だった。`ALLOWED_ORIGINS` 環境変数(カンマ区切りで複数オリジン指定可)で上書きできるようにし、未設定時のみ従来通り `http://localhost:5173` にフォールバックする
- **本番でのクロスサイト構成だとログインが機能しない不具合**: セッションCookieが `same_site: :lax` 固定だったため、フロントエンドとバックエンドが別ドメインにデプロイされるクロスサイト構成では、ログインのSet-Cookie自体は成功してもその後のXHRにCookieが付与されず、常に未ログイン扱いになる状態だった。`config/application.rb` で `Rails.env.production?` により本番のみ `same_site: :none, secure: true` に切り替え(開発は従来通り `:lax`)
- `CLAUDE.md`・`startup-guide.md` に本番デプロイ時に設定が必要な環境変数(`RAILS_MASTER_KEY`・`DATABASE_URL`・`ALLOWED_ORIGINS`・`VITE_API_BASE_URL`)を追記
- 検証: `bundle exec bundler-audit`(gemの既知脆弱性0件)、`bin/rails test`(73件成功)、`tsc -b --noEmit`、`vite build`(`VITE_API_BASE_URL`指定時にビルド成果物へ正しく埋め込まれ`localhost`文字列が残らないことを確認)、`RAILS_ENV=production` での `ActionDispatch::Session::CookieStore` 実際の設定値(`same_site: :none, secure: true`)を実機確認

#### 残課題3点の解消(バンドルサイズ・Hostヘッダー許可・本番seed事故防止)
- **JSバンドルの遅延分割**: `Dashboard.tsx` の `html2canvas`/`jspdf` の静的importを、PDFダウンロードボタン押下時のみ動的import(`import('html2canvas')`/`import('jspdf')`)する形に変更。メイン chunk が 928KB→334KB(gzip 286KB→110KB)に縮小し、`vite build` の「500KBを超えるchunkがある」警告も解消。実機(Playwright)でPDFダウンロードが遅延import後も従来通り動作することを確認
- **`config.hosts` のハードニング**: `config/environments/production.rb` に `RAILS_ALLOWED_HOSTS` 環境変数(カンマ区切り)を追加。未設定の間は `config.hosts` に何も追加せず(=制限なし)、デプロイ先ドメイン確定前のデプロイがHost Header不一致で落ちることはない。ドメインが決まったら設定することでDNSリバインディング対策を有効化できる。あわせてヘルスチェック(`/up`)のHost検証除外(`config.host_authorization`)を明示化
- **本番DBへの誤ったdb:seed実行を防止**: `db/seeds.rb` の先頭に、`Rails.env.production?` かつ `ALLOW_PRODUCTION_SEED=true` が明示指定されていない場合は `raise` して処理を止めるガードを追加。既知の認証情報(`dev@example.com`/`password123`)が本番DBに誤って作成されるのを防ぐ
- 検証: `tsc -b --noEmit`・`bin/rails test`(73件成功)・`vite build`(chunk構成とサイズを確認)に加え、`RAILS_ENV=production` で `RAILS_ALLOWED_HOSTS` 未設定/設定時それぞれの `config.hosts` の値、および `db:seed` がopt-inなしでは`RuntimeError`で止まり`ALLOW_PRODUCTION_SEED=true`指定時は通ることを実機確認

#### PDFダウンロードの余白解消(ページサイズを画面ぴったりに)
- 従来は固定の A4 サイズに収まるよう画像を等比縮小して中央配置していたため、ダッシュボードの縦横比と A4 の縦横比が一致せず上下または左右に白い余白ができていた
- `Dashboard.tsx` の `downloadPdf` を、PDFのページサイズ自体をキャプチャした内容(`html2canvas`の出力)のアスペクト比に合わせて作成する方式に変更(`jsPDF` の `format` にキャプチャ画像から算出した `[pageWidth, pageHeight]`(pt換算)を渡し、画像をページ全面(0,0)〜(pageWidth,pageHeight)に敷き詰める)。向き(`orientation`)は算出した幅高さの大小で自動判定
- 検証: Playwright + `pdf-lib` で実際にダウンロードしたPDFのページサイズを読み取り、ダッシュボードのDOMサイズとアスペクト比が完全一致(例: 1440×900px → 1080×675pt、いずれも比率1.6)することを確認。余白なく画面いっぱいのPDFになる

## 2026-09-11

### 共通

#### 全データのDB永続化(localStorage運用からの完全移行)
- これまでロール・タスク・週次スケジュール・日次/週次メモ・Sharpen the Saw・ミッションステートメントを含む主要データが`localStorage`のみに保存されており、`roles`/`tasks`テーブルとAPIは実装済みなのにフロントから未使用、`week_data`/`scheduled_tasks`/`day_notes`/`sharpen_the_saw_*`はAPI自体が存在しない状態だった。今回、足りないAPIを全て実装し、フロントエンドをDB運用に一本化した
- 詳細なアーキテクチャは [ARCHITECTURE.md](ARCHITECTURE.md) を参照

### バックエンド

#### スキーマ変更(マイグレーション5件)
- `users.mission_statement`(text)を追加
- `scheduled_tasks.completed`(boolean, default false)を追加(リストモードの完了チェック用)
- `tasks.week_data_id`(nullable FK, on_delete: :nullify)を追加。一時タスク(`is_permanent: false`)だけがその週の`week_data`に紐づき、永続タスクは常にNULL
- `sharpen_the_saw_tasks.user_id`(NOT NULL FK)を追加。旧実装はこの列が無く全ユーザー共有になってしまうバグがあった(テーブルが空だったため安全に追加)
- `sharpen_the_saw_areas`の既存シード値(`Body`/`Intelligence`/`Social・Emotional`/`Mental`、旧Spring版由来で移植時からフロントの`DEFAULT_SAW_AREAS`と不一致だった)を`physical`/`mental`/`social-emotional`/`spiritual`(名称`Physical`/`Intellectual`/`Social/Emotional`/`Spiritual`)に是正するデータ移行マイグレーションを追加。`db/seeds.rb`も同じ値に修正

#### 新規コントローラ・ルーティング
| メソッド | パス | 内容 |
|---|---|---|
| `GET`/`PUT` | `/api/mission_statement` | ミッションステートメントの取得・更新 |
| `GET`/`PUT` | `/api/sharpen_the_saw_areas` | 4領域＋タスク取得、全領域まとめての一括diff保存(削除/更新/新規作成) |
| `GET`/`PUT` | `/api/week_data/:week_start` | 週データ(無ければ自動作成)。`scheduledTasks`/`dayNotes`/`temporaryTasks`を含めて返す |
| `POST` | `/api/week_data/:week_start/scheduled_tasks` | スケジュール済みタスク作成 |
| `PUT`/`DELETE` | `/api/scheduled_tasks/:id` | 部分更新・削除 |
| `PUT` | `/api/week_data/:week_start/day_notes/:day` | 日次メモ・睡眠時間のupsert(`updateDayNotes`/`updateSleepTime`両方をこの1本でカバー) |

- 新規モデル: `WeekData`/`ScheduledTask`/`DayNote`/`SharpenTheSawArea`/`SharpenTheSawTask`(`week_data`関連は"week_data"がRailsの英語推論だと単数"WeekDatum"になってしまうため、全ての関連付けで`class_name: "WeekData"`を明示)
- 既存`RolesController`の修正: `role_json`が返す`tasks`を永続タスクのみに絞るよう修正(一時タスクも同じ`tasks`テーブルに入るため)。`update`アクションの`roleName`必須チェックを緩和し、色・isExpandedだけの部分更新に対応(roleName/isExpanded/colorのいずれか1つも無い場合のみ400)。`create`アクションを`head :created`から作成したロールのJSONを返すよう変更(フロントが仮IDを本物のIDへ差し替えるために必要)
- 既存`TasksController`の修正: `create`で`isPermanent: false`のとき`weekStart`を必須にし、対応する週の`week_data`へ`week_data_id`で紐づける。`update`でも`isPermanent`切替時に`week_data_id`を付け替え(永続化時はNULLに戻す)。`create`のレスポンスも`head :created`から作成したタスクのJSONを返すよう変更

#### テスト
- 新規5コントローラ分の統合テストを追加(`mission_statement_controller_test.rb`/`sharpen_the_saw_areas_controller_test.rb`/`week_data_controller_test.rb`/`scheduled_tasks_controller_test.rb`/`day_notes_controller_test.rb`)
- 既存`roles_controller_test.rb`/`tasks_controller_test.rb`を、上記の仕様変更(部分更新の許可・tasksの永続タスクのみフィルタ・weekStart必須化)に合わせて修正
- `test/test_helper.rb`に`sharpen_the_saw_areas`のマスタデータ投入を追加(`bin/rails test`が使う`db:test:prepare`はスキーマ構造のみロードしデータ移行マイグレーションの中身は実行しないため、テストDBには明示的な投入が必要だった)
- 検証: `bin/rails test`(100件成功)。さらに開発DB(`dev@example.com`)に対して全新規エンドポイントをcurlで実行し、`psql`で`roles`/`tasks`/`scheduled_tasks`/`day_notes`/`week_data`に実データが入ることを直接確認(検証用データは作業後に削除)

### フロントエンド

#### サービス層の追加・拡張
- 新規: `weekDataService.ts`(週データ本体・scheduled_tasks・day_notes)、`sharpenTheSawService.ts`、`missionStatementService.ts`、`migrationService.ts`(旧localStorageデータの一度限りのDB取り込み)
- 拡張: `roleService.ts`(`fetchRoles`/`createRole`/`deleteRole`/`updateRoleName`/`updateRoleExpanded`を追加、`updateRoleColor`はroleName不要の部分更新に対応)、`taskService.ts`(`createTask`/`deleteTask`/`updateTask`に`weekStart`対応を追加)
- 新規ユーティリティ`utils/debounce.ts`の`KeyedDebouncer`: カレンダーのドラッグ/リサイズ(mousemoveのたびに発火)やメモのキー入力のような高頻度更新を、キーごとにサーバー保存だけ400〜600ms間引く。同一キーへの連続呼び出しはマージ(`schedule`)、楽観的作成のID差し替えに追従する`rekey`、週切り替え/ログアウト時に保留分を即時確定する`flushAll`を持つ

#### `Dashboard.tsx` の全面書き換え
- 旧: `useState`のlazy initializerでlocalStorageを同期読み込み→巨大`useEffect`で全stateをlocalStorageへ毎回書き戻す構成
- 新: マウント時に`GET /api/roles`+`/api/sharpen_the_saw_areas`+`/api/mission_statement`+`/api/week_data/:currentWeekKey`を並行取得して初期state構築。各ハンドラは「ローカルstateを楽観的に更新→対応するAPIを呼ぶ」に統一
- 初回ログイン時の自動移行: マウント時、DBの`roles`が空かつブラウザに旧localStorageデータが残っている場合のみ`migrationService`がロール→タスク→SharpenTheSaw→ミッションステートメント→週データの順にDBへ取り込み、成功後にlocalStorageを削除する
- 楽観的作成のID差し替え: `addRole`/`addTask`/カレンダーへのドロップ(`handleTaskDrop`)/コピー&ペースト(`addCopiedTask`)は仮ID(`temp-${Date.now()}`)で即時描画し、サーバー応答後に本物のIDへ置き換える(失敗時はロールバックしてエラーバナー表示)
- 週の切り替え(`changeWeek`)は訪問済みの週だけをセッション内`Map`にキャッシュし、未訪問の週のみ`GET /api/week_data/:weekKey`を呼ぶ。切り替え前に保留中の書き込みを`flushAll()`で確定させる
- `isListMode`(リスト表示モード)は端末ローカルな表示設定として`localStorage`に残した(DBに持たせるほどの価値が無い表示状態のため)。`currentWeek`(閲覧中の週)はリロードのたびに実際の「今週」から開始する仕様に変更(DBに保存する自然なカラムが無く、優先度も低いため)
- ローディング画面(`isLoading`)・読み込み失敗画面(`loadError`)・保存失敗バナー(`saveError`、画面上部固定・×で閉じられる)を追加
- 検証: `tsc --noEmit`・`vite build`が成功することを確認

#### 上記実装の検証・不具合修正
実装直後に改めてコードレビューし、以下を発見・修正した。

- **`isListMode`の移行漏れ**: 新しい移行処理(`migrationService`)が旧localStorageの`isListMode`を読み取らず、新しい専用キーにも書き込んでいなかったため、既存ユーザーがアップグレード後にリスト表示設定を静かに失う状態だった。`migrationService.ts`に`readLegacyListMode`を追加し、`Dashboard.tsx`の`isListMode`初期化時にDB移行の完了を待たず同期的に旧データから復元し、新しい専用キーへ書き込むよう修正
- **削除・構造変更系ハンドラのロールバック漏れ**: `deleteRole`/`deleteTask`/`handleTaskDeleted`/`toggleTaskPermanent`がAPI呼び出し失敗時にローカルstateを元に戻しておらず、画面上は削除・移動されたのにDB側は変更されていない(次回リロードまで気付けない)不整合が起きうる状態だった。各ハンドラで変更前のstateを保持し、失敗時に`setRoles`/`setWeekDataCache`/`setSharpenTheSawAreas`で復元するよう修正(`updateSharpenTheSawAreas`も同様に対応)
- **`deleteRole`で一時タスクの参照が残る**: ロール削除時にscheduled_tasksはローカルキャッシュから除去していたが、削除したロールを参照する`temporaryTasks`(一時タスク)は除去しておらず、DB側はカスケード削除されるのにセッション内キャッシュでは残ってしまっていた。`temporaryTasks`も合わせて除去するよう修正
- **並び替えの`sortOrder`計算バグ**: `reorderTasks`/`reorderRoles`で仮ID(サーバー未反映)の項目を`filter`してから`index`を採番していたため、リスト中に仮ID項目があると以降の項目の`sortOrder`がずれる状態だった。`map`で全項目の位置を確定させてから`filter`する順序に修正
- **`ScheduledTasksController#create`のバリデーション不足**: 必須パラメータ(taskId/roleId/day/startTime/duration/title)が欠けたリクエストが、素のNOT NULL制約違反で500エラーになる状態だった。他のcreateアクションと同様に明示的な400チェックを追加(`day: 0`が`blank?`判定で弾かれないことをテストで確認)
- **`RolesController#role_json`のN+1気味な非効率**: `role.tasks.select(&:is_permanent)`とRuby側でフィルタしていたため、一時タスクが週を重ねて増えるほど毎回のロール取得が重くなる作りだった。`role.tasks.where(is_permanent: true)`とSQL側の絞り込みに変更
- **移行処理の頑健性不足**: 旧localStorageに削除済みロールへの参照など不整合データが含まれていると、その1週分の移行失敗で処理全体が例外停止し、かつ移行済みフラグが立たないため次回ログイン時に既に作成済みの分まで重複作成されうる状態だった。週ごとの移行を個別に`try/catch`し、参照先ロールが見つからない一時タスク/スケジュール済みタスクは(既存のタスク欠落時と同様に)ログを出してスキップするよう修正
- 検証: `bin/rails test`(102件成功、新規2件追加)、`tsc --noEmit`・`vite build`成功、`/api/roles/reorder`を文字列IDでcurl実行しDBへ正しく反映されることを実機確認
