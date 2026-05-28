# /start — 全サービス起動手順

WeeklyCompass の開発環境を起動する手順を表示し、各サービスの状態を確認する。

## 手順

以下を順番に実行してください（それぞれ別のターミナルで）:

**ターミナル 1 — PostgreSQL**
```bash
docker start my-postgres
```

**ターミナル 2 — バックエンド**
```bash
cd task-app && ./gradlew bootRun
```

**ターミナル 3 — フロントエンド**
```bash
cd front-task-app && npm run dev
```

## 起動確認

各サービスの状態をチェックしてください:

```bash
# Docker が起動しているか確認
docker ps | grep my-postgres

# バックエンドが応答するか確認
curl -s http://localhost:8080/api/roles | head -c 100

# フロントエンドのアクセス先
open http://localhost:5173
```

## 注意

- フロントエンドは localStorage のみで動作するため、バックエンドなしでも起動可能
- バックエンドは DB が起動していないと起動に失敗する
- DB コンテナが存在しない場合は startup-guide.md の「初回作成」手順を参照
