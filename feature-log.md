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
