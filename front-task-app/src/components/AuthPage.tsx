import { useState } from 'react';
import type { FormEvent } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { extractErrorMessage } from '../services/authService';
import styles from './AuthPage.module.css';

type Mode = 'login' | 'signup';

function AuthPage() {
  const { login, signup } = useAuth();
  const [mode, setMode] = useState<Mode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [passwordConfirmation, setPasswordConfirmation] = useState('');
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const switchMode = (nextMode: Mode) => {
    setMode(nextMode);
    setError(null);
    setPassword('');
    setPasswordConfirmation('');
  };

  // ブラウザ標準のフォームバリデーション(input要素のフォーカスが外れた瞬間などに
  // ポップアップで出る)は入力途中でも割り込んで表示され唐突なので noValidate で止め、
  // 送信時にのみ自前のエラー表示(styles.error)でまとめて出す
  const validate = (): string | null => {
    if (!email.trim()) return 'メールアドレスを入力してください。';
    if (mode === 'signup') {
      if (password.length < 8) return 'パスワードは8文字以上で入力してください。';
      if (password !== passwordConfirmation) return 'パスワード(確認)が一致しません。';
    } else if (!password) {
      return 'パスワードを入力してください。';
    }
    return null;
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError(null);
    setIsSubmitting(true);
    try {
      if (mode === 'login') {
        await login(email, password);
      } else {
        await signup(email, password, passwordConfirmation, name || undefined);
      }
    } catch (err) {
      setError(extractErrorMessage(err, mode === 'login'
        ? 'ログインに失敗しました。'
        : 'サインアップに失敗しました。'));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.card}>
        <div className={styles.brand}>
          <i className="bi bi-compass"></i>
          <span>WeeklyCompass</span>
        </div>

        <div className={styles.tabs}>
          <button
            type="button"
            className={`${styles.tab} ${mode === 'login' ? styles.tabActive : ''}`}
            onClick={() => switchMode('login')}
          >
            ログイン
          </button>
          <button
            type="button"
            className={`${styles.tab} ${mode === 'signup' ? styles.tabActive : ''}`}
            onClick={() => switchMode('signup')}
          >
            新規登録
          </button>
        </div>

        <form onSubmit={handleSubmit} className={styles.form} noValidate>
          {mode === 'signup' && (
            <label className={styles.field}>
              <span>名前(任意)</span>
              <input
                type="text"
                value={name}
                onChange={e => setName(e.target.value)}
                autoComplete="name"
              />
            </label>
          )}

          <label className={styles.field}>
            <span>メールアドレス</span>
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              autoComplete="email"
              required
            />
          </label>

          <label className={styles.field}>
            <span>パスワード</span>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
              minLength={mode === 'signup' ? 8 : undefined}
              required
            />
          </label>

          {mode === 'signup' && (
            <label className={styles.field}>
              <span>パスワード(確認)</span>
              <input
                type="password"
                value={passwordConfirmation}
                onChange={e => setPasswordConfirmation(e.target.value)}
                autoComplete="new-password"
                minLength={8}
                required
              />
            </label>
          )}

          {error && <p className={styles.error}>{error}</p>}

          <button type="submit" className={styles.submitBtn} disabled={isSubmitting}>
            {isSubmitting ? '処理中...' : mode === 'login' ? 'ログイン' : 'アカウントを作成'}
          </button>
        </form>
      </div>
    </div>
  );
}

export default AuthPage;
