# 実装計画: 5つの機能追加（フロントエンド + バックエンド）

## Context
Vue 3 + TypeScript（フロントエンド）/ Spring Boot + MyBatis + PostgreSQL（バックエンド）の時間管理アプリに5機能を追加する。
フロントエンドは現在 localStorage のみで動作しており、バックエンド呼び出しはコメントアウト済み。

---

## 対象ファイル一覧

### フロントエンド
| ファイル | 概要 |
|---|---|
| `front-task-app/src/types/index.ts` | 型定義 |
| `front-task-app/src/components/Dashboard.vue` | 状態ハブ |
| `front-task-app/src/components/LeftSidebar.vue` | ロール・タスクパネル |
| `front-task-app/src/components/WeeklyCalendar.vue` | カレンダーグリッド |
| `front-task-app/src/services/roleService.ts` | ロールAPIクライアント |
| `front-task-app/src/services/taskService.ts` | タスクAPIクライアント |

### バックエンド
| ファイル | 概要 |
|---|---|
| `task-app/src/main/resources/sql/database_schema.sql` | テーブル定義 |
| `task-app/src/main/java/.../model/Role.java` | モデル |
| `task-app/src/main/java/.../model/Task.java` | モデル |
| `task-app/src/main/java/.../dto/RoleDto.java` | DTO |
| `task-app/src/main/java/.../dto/TaskUpdateDto.java` | DTO（新規） |
| `task-app/src/main/java/.../form/RoleForm.java` | リクエストフォーム |
| `task-app/src/main/java/.../form/RoleUpdateForm.java` | リクエストフォーム |
| `task-app/src/main/java/.../form/TaskUpdateForm.java` | リクエストフォーム（新規） |
| `task-app/src/main/java/.../form/ReorderItem.java` | リクエストフォーム（新規） |
| `task-app/src/main/java/.../response/RoleResponse.java` | レスポンス |
| `task-app/src/main/java/.../response/TaskResponse.java` | レスポンス |
| `task-app/src/main/java/.../mapper/RoleMapper.java` | MyBatisマッパー |
| `task-app/src/main/java/.../mapper/TaskMapper.java` | MyBatisマッパー |
| `task-app/src/main/resources/mapper/RoleMapper.xml` | SQLマッピング |
| `task-app/src/main/resources/mapper/TaskMapper.xml` | SQLマッピング |
| `task-app/src/main/java/.../service/RoleService.java` | サービス |
| `task-app/src/main/java/.../service/TaskService.java` | サービス |
| `task-app/src/main/java/.../controller/RoleRestController.java` | コントローラー |
| `task-app/src/main/java/.../controller/TaskRestController.java` | コントローラー |

---

## DB マイグレーション（まず実施）

```sql
-- roles テーブルに color と sort_order 列を追加
ALTER TABLE roles ADD COLUMN color VARCHAR(7) DEFAULT '#4a90d9';
ALTER TABLE roles ADD COLUMN sort_order INTEGER DEFAULT 0;

-- roles の既存データに sort_order を連番で設定
UPDATE roles SET sort_order = role_id;

-- tasks テーブルに sort_order 列を追加
ALTER TABLE tasks ADD COLUMN sort_order INTEGER DEFAULT 0;

-- tasks の既存データに sort_order を連番で設定
UPDATE tasks SET sort_order = id;
```

**database_schema.sql も同様に更新する（新規環境向け）。**

> **既存バグの修正:** `database_schema.sql` の tasks テーブルに `REFERENCES roles(id)` と書かれているが、正しくは `REFERENCES roles(role_id)`。この機会に併せて修正する。

---

## Phase 1: 起床・就寝時間を30分単位に

**DB変更:** なし（`day_notes.sleep_start / sleep_end` は `TIME` 型で既存）
**バックエンド変更:** なし

### WeeklyCalendar.vue のみ変更

1. sleep dialog の `<input type="time">` に `step="1800"` を追加
2. `saveSleepTime()` に `snapTo30()` ヘルパーを追加（手入力の端数を丸める）

```ts
snapTo30(time: string): string {
  const [h, m] = time.split(':').map(Number);
  const snapped = m < 15 ? 0 : m < 45 ? 30 : 0;
  const hour = m >= 45 ? (h + 1) % 24 : h;
  return `${hour.toString().padStart(2,'0')}:${snapped.toString().padStart(2,'0')}`;
}
// saveSleepTime() 内で両方の時間に適用
```

---

## Phase 2: ロールカラー設定 + カレンダー反映

### DB変更（上記マイグレーションに含む）
`roles` テーブルに `color VARCHAR(7) DEFAULT '#4a90d9'` 追加済み。

### バックエンド変更

**`Role.java`** — `color` フィールドを追加:
```java
private String color;
```

**`RoleDto.java`** — `color` を追加（コンストラクタ引数にも追加）

**`RoleForm.java`** — `color` を追加（POST 時に初期カラーを受け取る）

**`RoleUpdateForm.java`** — `color` を追加:
```java
private String color; // バリデーションなし（null 許容）
```

**`RoleResponse.java`** — `color` を追加（コンストラクタ引数にも追加）

**`RoleMapper.xml`** — 全クエリを更新:
- `selectAll` / `selectById`: SELECT に `color` を追加、resultMap に `<result column="color" property="color"/>` を追加
- `insert`: VALUES に `#{color}` を追加
- `update` / `updateWithExpanded`: SET に `color = #{color}` を追加

**`RoleService.java`**:
- `createRole()`: `role.setColor(roleDto.getColor())` を追加
- `updateRole()`: `color` パラメータを受け取り `existingRole.setColor(color)` を追加
- `getAllRoles()` / `updateRole()`: `RoleResponse` 生成時に `color` を渡す

**`RoleRestController.java`**:
- `createRole()`: `RoleDto` に `color` を渡す
- `updateRole()`: `updateRole(id, name, isExpanded, color)` シグネチャに変更

### フロントエンド変更

**`types/index.ts`** — `Role` に `color?: string` を追加

**`LeftSidebar.vue`**:
- emits に `'update-role-color'` を追加
- role-header 内にカラースウォッチ（丸いドット）と非表示の `<input type="color">` を追加
  ```html
  <span class="role-color-swatch"
    :style="{ backgroundColor: role.color || '#4a90d9' }"
    @click.stop="openColorPicker(role.id)"
  ></span>
  <input type="color" :ref="`colorPicker_${role.id}`"
    style="display:none" :value="role.color || '#4a90d9'"
    @change="onColorChange(role.id, $event)" />
  ```
- `openColorPicker(roleId)`, `onColorChange(roleId, event)` メソッドを追加
- **注意:** `$refs` が `v-for` 内では配列になるため `(ref as HTMLInputElement[])[0]` でアクセス

**`Dashboard.vue`**:
- `addRole()` でデフォルトカラーをパレットから循環割り当て:
  ```ts
  const ROLE_COLORS = ['#4a90d9','#e67e22','#27ae60','#8e44ad','#e74c3c','#16a085'];
  color: ROLE_COLORS[this.roles.length % ROLE_COLORS.length]
  ```
- computed `roleColorMap(): Record<string, string>` を追加:
  ```ts
  roleColorMap() {
    const map: Record<string, string> = {};
    this.roles.forEach(r => { map[r.id] = r.color || '#4a90d9'; });
    return map;
  }
  ```
- `<LeftSidebar>` に `@update-role-color="updateRoleColor"` を追加
- `<WeeklyCalendar>` に `:role-colors="roleColorMap"` を追加
- `updateRoleColor(roleId, color)` メソッドを追加

**`WeeklyCalendar.vue`**:
- props に `roleColors: Record<string, string>` を追加（default: `() => ({})`）
- `getTaskStyle(task)` に `backgroundColor` と `borderColor` を追加:
  ```ts
  const bg = this.roleColors[task.roleId] || '#4a90d9';
  return { top, height, left: '2px', right: '2px',
    backgroundColor: bg, borderColor: this.darkenColor(bg) };
  ```
- `darkenColor(hex)` ヘルパーを追加（各 RGB チャンネルを-40）
- **CSS 修正:** `.scheduled-task` の `background-color: #007bff` と `border` の固定値を削除（inline style を優先させるため）

**`roleService.ts`**:
- `updateRoleColor(id, color)` 関数を追加（`PUT /api/roles/{id}` に color を含めて送信）

---

## Phase 3: タスク名編集 + 並び替え

### DB変更（上記マイグレーションに含む）
`tasks` テーブルに `sort_order INTEGER DEFAULT 0` 追加済み。

### バックエンド変更

**新規 `TaskUpdateForm.java`**:
```java
@Getter
public class TaskUpdateForm {
    private String title;        // null 許容（変更しない場合）
    private Boolean isPermanent; // null 許容
}
```

**新規 `TaskUpdateDto.java`**:
```java
@Getter @AllArgsConstructor
public class TaskUpdateDto {
    private Integer taskId;
    private String title;
    private Boolean isPermanent;
}
```

**新規 `ReorderItem.java`**（RoleRestController / TaskRestController で共用）:
```java
@Getter
public class ReorderItem {
    private Integer id;
    private Integer sortOrder;
}
```

**`Task.java`** — `sortOrder` フィールドを追加:
```java
private Integer sortOrder;
```

**`TaskResponse.java`** — `sortOrder` を追加（フロントエンドが順序を把握できるように）

**`TaskMapper.java`** — メソッドを追加:
```java
int update(Task task);
void updateSortOrder(@Param("id") Integer id, @Param("sortOrder") Integer sortOrder);
```

**`TaskMapper.xml`** — クエリを追加:
```xml
<!-- resultMap に sort_order を追加 -->
<result property="sortOrder" column="sort_order"/>

<!-- findAll / findByRoleId の ORDER BY を変更 -->
ORDER BY sort_order ASC

<!-- UPDATE クエリを追加 -->
<update id="update" parameterType="com.example.task_app.model.Task">
    UPDATE tasks
    SET title = #{title}, is_permanent = #{isPermanent},
        updated_at = CURRENT_TIMESTAMP
    WHERE id = #{taskId}
</update>

<update id="updateSortOrder">
    UPDATE tasks SET sort_order = #{sortOrder} WHERE id = #{id}
</update>
```

**`TaskService.java`** — メソッドを追加:
```java
public void updateTask(TaskUpdateDto dto) {
    Task task = taskMapper.findById(dto.getTaskId());
    if (task == null) throw new NoSuchElementException("Task not found");
    if (dto.getTitle() != null) task.setTitle(dto.getTitle());
    if (dto.getIsPermanent() != null) task.setIsPermanent(dto.getIsPermanent());
    taskMapper.update(task);
}
public void reorderTasks(List<ReorderItem> items) {
    items.forEach(item -> taskMapper.updateSortOrder(item.getId(), item.getSortOrder()));
}
```

**`TaskRestController.java`** — エンドポイントを追加:
```java
@PutMapping("/{id}")
public ResponseEntity<Void> updateTask(
        @PathVariable Integer id,
        @RequestBody TaskUpdateForm form) {
    taskService.updateTask(new TaskUpdateDto(id, form.getTitle(), form.getIsPermanent()));
    return ResponseEntity.ok().build();
}

@PutMapping("/reorder")
public ResponseEntity<Void> reorderTasks(@RequestBody List<ReorderItem> items) {
    taskService.reorderTasks(items);
    return ResponseEntity.ok().build();
}
```
> **注意:** `PUT /api/tasks/reorder` は `PUT /api/tasks/{id}` と競合しない（`{id}` は `Integer` 型なので "reorder" を受け付けない）

### フロントエンド変更

**`LeftSidebar.vue`**:
- emits に `'update-task-title'`, `'reorder-tasks'` を追加
- data に `editingTaskId: null as string | null`, `editingTaskTitle: ''` を追加
- task-item テンプレート変更:
  - タスクタイトル部分をダブルクリックでインライン編集（ロール名編集と同パターン）
  - `:draggable="editingTaskId !== task.id"` で編集中はカレンダードラッグを無効化
  - ↑↓ ボタンを追加（カレンダードラッグと干渉しない）
- `startEditTask(task)`, `confirmEditTask(task, roleId)`, `cancelEditTask()` メソッドを追加
- `moveTask(roleId, taskId, direction: -1|1)` メソッドを追加

**`Dashboard.vue`**:
- `@update-task-title="updateTaskTitle"`, `@reorder-tasks="reorderTasks"` を追加
- `updateTaskTitle(roleId, taskId, newTitle)`: 永続タスク → 一時タスクの順に検索してタイトル更新
- `reorderTasks(roleId, reorderedTasks)`:
  - `role.tasks` = `reorderedTasks.filter(t => t.isPermanent)`
  - `temporaryTasks` の該当ロール分も順序に従って更新

**`taskService.ts`**:
- `updateTaskTitle(id, title)` 関数を追加（`PUT /api/tasks/{id}`）
- `reorderTasks(items: { id: number, sortOrder: number }[])` 関数を追加

---

## Phase 4: 永続タスク ↔ 一時タスク切り替え

**DB変更:** なし（`is_permanent` 列は既存）
**バックエンド変更:** Phase 3 の `PUT /api/tasks/{id}` で `isPermanent` を更新できるため追加変更なし

### フロントエンド変更

**`LeftSidebar.vue`**:
- emits に `'toggle-task-permanent'` を追加
- `task-type` バッジをクリッカブルに変更（`@click.stop` + `cursor:pointer`）
- バッジに CSS クラスを追加（P: 青系、T: 黄系）
- `toggleTaskPermanent(task, roleId)` メソッドを追加（confirm ダイアログ付き）

**`Dashboard.vue`**:
- `@toggle-task-permanent="toggleTaskPermanent"` を追加
- `toggleTaskPermanent(roleId, taskId, currentlyPermanent)` メソッド:
  - Permanent → Temporary: `role.tasks` から取り出し → `isPermanent = false` → `temporaryTasks` に追加
  - Temporary → Permanent: `temporaryTasks` から取り出し → `isPermanent = true` → `role.tasks` に追加

**`taskService.ts`**:
- `toggleTaskPermanent(id, isPermanent)` 関数を追加（`PUT /api/tasks/{id}` の `isPermanent` のみ更新）

---

## Phase 5: ロール自体の並び替え

### DB変更（上記マイグレーションに含む）
`roles` テーブルに `sort_order INTEGER DEFAULT 0` 追加済み。

### バックエンド変更

**`Role.java`** — `sortOrder` フィールドを追加:
```java
private Integer sortOrder;
```

**`RoleMapper.java`** — メソッドを追加:
```java
void updateSortOrder(@Param("roleId") Integer roleId, @Param("sortOrder") Integer sortOrder);
```

**`RoleMapper.xml`** — クエリを追加・更新:
```xml
<!-- resultMap に sort_order を追加 -->
<result column="sort_order" property="sortOrder"/>

<!-- selectAll の ORDER BY を変更 -->
ORDER BY sort_order ASC, role_id ASC

<!-- updateSortOrder を追加 -->
<update id="updateSortOrder">
    UPDATE roles SET sort_order = #{sortOrder} WHERE role_id = #{roleId}
</update>
```

**`RoleService.java`** — メソッドを追加:
```java
public void reorderRoles(List<ReorderItem> items) {
    items.forEach(item -> roleMapper.updateSortOrder(item.getId(), item.getSortOrder()));
}
```

**`RoleRestController.java`** — エンドポイントを追加:
```java
@PutMapping("/reorder")
public ResponseEntity<Void> reorderRoles(@RequestBody List<ReorderItem> items) {
    roleService.reorderRoles(items);
    return ResponseEntity.ok().build();
}
```
> `PUT /api/roles/reorder` は `PUT /api/roles/{id}` と競合しない（`{id}` は `Integer` 型）

### フロントエンド変更

**`LeftSidebar.vue`**:
- emits に `'reorder-roles'` を追加
- role-header に ↑↓ ボタンを追加
- `moveRole(roleId, direction: -1|1)` メソッドを追加:
  ```ts
  moveRole(roleId: string, direction: -1 | 1) {
    const roles = [...this.roles];
    const idx = roles.findIndex(r => r.id === roleId);
    const newIdx = idx + direction;
    if (newIdx < 0 || newIdx >= roles.length) return;
    [roles[idx], roles[newIdx]] = [roles[newIdx], roles[idx]];
    this.$emit('reorder-roles', roles);
  }
  ```

**`Dashboard.vue`**:
- `@reorder-roles="reorderRoles"` を追加
- `reorderRoles(newRoles: Role[])`: `this.roles = newRoles; this.saveData()`

**`roleService.ts`**:
- `reorderRoles(items: { id: number, sortOrder: number }[])` 関数を追加（`PUT /api/roles/reorder`）

---

## 共通 CSS（LeftSidebar.vue の scoped style に追加）

```css
.reorder-btn { background: none; border: none; color: #999; font-size: 11px; cursor: pointer; padding: 0 2px; }
.reorder-btn:hover { color: #333; }
.role-color-swatch { width: 14px; height: 14px; border-radius: 50%; border: 1px solid rgba(0,0,0,0.15); cursor: pointer; flex-shrink: 0; margin-right: 4px; }
.badge-permanent { background-color: #cce5ff; color: #004085; cursor: pointer; }
.badge-temporary { background-color: #fff3cd; color: #856404; cursor: pointer; }
```

---

## 注意事項（Gotchas）

1. **`$refs` in `v-for`**: Vue 3 では配列になる場合あり → `(ref as HTMLInputElement[])[0]` でアクセス
2. **CSS 優先度**: `.scheduled-task` の `background-color` 固定値を削除しないと inline style が負ける
3. **`mergedRoles` は computed コピー**: `moveTask` では直接変えず emit で Dashboard に委譲、Dashboard 側で `isPermanent` で分割保存
4. **編集中のドラッグ抑制**: `:draggable="editingTaskId !== task.id"` を設定
5. **localStorage 旧データの互換性**: `color`/`sortOrder` がない旧データは `|| デフォルト値` でフォールバック
6. **`changeWeek` との整合**: `role.tasks` は `isPermanent: true` のみ保持するルールを維持
7. **reorder と {id} の競合なし**: Spring は `Integer` 型パスパラメータに "reorder" を解釈しないため問題なし

---

## 検証手順

| Phase | 確認内容 |
|---|---|
| 1 | sleep dialog で30分刻みのみ選択可 / `getSleepInfo` の表示が :00 or :30 のみ |
| 2 | ロール横のカラードットをクリック → ピッカーで変更 → カレンダーのタスク背景色が即変わる / `GET /api/roles` のレスポンスに `color` が含まれる |
| 3 | タスクタイトルをダブルクリック → inline 編集 / ↑↓でタスク順変更 / `PUT /api/tasks/{id}` & `PUT /api/tasks/reorder` が正常応答 |
| 4 | P バッジをクリック → confirm → T に変わる / 週移動で一時タスクは消え永続タスクは残る / `PUT /api/tasks/{id}` で `isPermanent` 更新 |
| 5 | ロールヘッダの ↑↓ でロール順変更 / リロード後も `GET /api/roles` の返却順が保持 / `PUT /api/roles/reorder` が正常応答 |
