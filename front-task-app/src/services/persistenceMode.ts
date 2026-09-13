// データ永続化の方式を切り替えるフラグ。
//   'local' : ブラウザの localStorage のみで完結する(バックエンド不要、ログイン画面も出さない)
//   'api'   : 従来通り Rails API + PostgreSQL に保存する(Cookieセッション認証あり)
//
// 各serviceのAPI呼び出しコードは削除せず残してあるので、接続を復活させたい場合は
// .env に VITE_PERSISTENCE_MODE=api を設定するだけでよい(未設定時は暫定的に 'local' がデフォルト)。
export type PersistenceMode = 'local' | 'api'

export const PERSISTENCE_MODE: PersistenceMode =
  import.meta.env.VITE_PERSISTENCE_MODE === 'api' ? 'api' : 'local'

export const isLocalMode = PERSISTENCE_MODE === 'local'
