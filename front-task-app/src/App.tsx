import { AuthProvider, useAuth } from './contexts/AuthContext'
import Dashboard from './components/Dashboard'
import AuthPage from './components/AuthPage'
import styles from './App.module.css'
import './index.css'

function AppContent() {
  const { user, isLoading, logout } = useAuth()

  if (isLoading) {
    return <div className={styles.loadingScreen}>読み込み中...</div>
  }

  if (!user) {
    return <AuthPage />
  }

  return (
    // key を user.id にすることで、別アカウントへの切り替え時に Dashboard を
    // 再マウントしてローカルステートを作り直し、前のユーザーのデータが残らないようにする
    <Dashboard
      key={user.id}
      storageKey={`fourth-gen-time-management:${user.id}`}
      userLabel={user.name || user.email}
      onLogout={logout}
    />
  )
}

function App() {
  return (
    <div id="app">
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </div>
  )
}

export default App
