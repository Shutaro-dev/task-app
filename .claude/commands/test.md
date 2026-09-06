# /test — バックエンドテスト実行

Rails のテストスイートを実行する。`task_app_test` DB（PostgreSQL）を使用する。

## 実行

```bash
cd task-app && bin/rails test
```

## テスト対象

| テストファイル | 内容 |
|---|---|
| `test/controllers/api/roles_controller_test.rb` | ロール API の CRUD・color・reorder・バリデーション |
| `test/controllers/api/tasks_controller_test.rb` | タスク API の CRUD・reorder・バリデーション |

## 個別実行

```bash
bin/rails test test/controllers/api/roles_controller_test.rb
```
