# /reset-db — DB リセット

**警告: 全データが削除されます。** 開発中に DB を初期状態に戻したい場合に使用。

## 実行

```bash
# プロジェクトルートで実行
./reset-db.sh
```

## スキーマのみ適用（データを消さない場合）

テーブルが存在しない場合のみ適用（既存データは保持）:

```bash
./apply-schema.sh
```

## マイグレーション（列追加など）

スキーマ変更を既存 DB に手動適用する場合:

```bash
docker exec -it my-postgres psql -U user -d task_app
```

psql 内で ALTER TABLE を実行。

## DB 接続情報

| 項目 | 値 |
|---|---|
| Host | localhost:5432 |
| DB | task_app |
| User | user |
| Password | password |
