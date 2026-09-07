import axios from 'axios'
import { createRoot } from 'react-dom/client'
import App from './App'

// バックエンドの認証は Cookie セッションのため、すべての axios リクエストで
// Cookie の送受信を有効にする
axios.defaults.withCredentials = true

createRoot(document.getElementById('app')!).render(<App />)
