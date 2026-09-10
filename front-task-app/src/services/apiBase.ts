// バックエンドAPIのオリジン。デプロイ時はフロントエンドと別ドメインで
// ホストされる想定のため、ビルド時に環境変数 VITE_API_BASE_URL で上書きできるようにする
// (未設定時はローカル開発用のデフォルトにフォールバック)。
export const API_ORIGIN = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
