# /test — バックエンドテスト実行

Spring Boot のテストスイートを実行する。H2 インメモリ DB を使用するため PostgreSQL 不要。

## 実行

```bash
cd task-app && ./gradlew test
```

## テスト対象

| テストクラス | 内容 |
|---|---|
| `RoleServiceTest` | ロールの CRUD・color 保存・reorder |
| `TaskServiceTest` | タスクの CRUD・reorder |
| `RoleRestControllerTest` | ロール API エンドポイント |
| `TaskRestControllerTest` | タスク API エンドポイント |

## レポート確認

```bash
open task-app/build/reports/tests/test/index.html
```
