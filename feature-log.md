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
