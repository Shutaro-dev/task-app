import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import type { User } from '../types';
import * as authService from '../services/authService';

interface AuthContextValue {
  user: User | null;
  isLoading: boolean;
  // サインアップ直後の1回だけ true になる。オンボーディングツアーの起動判定に使い、
  // Dashboard 側で表示したら consumeJustSignedUp() で false に戻す(リロードや再ログインでは true にならない)
  justSignedUp: boolean;
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, passwordConfirmation: string, name?: string) => Promise<void>;
  logout: () => Promise<void>;
  consumeJustSignedUp: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [justSignedUp, setJustSignedUp] = useState(false);

  useEffect(() => {
    let cancelled = false;
    authService.fetchCurrentUser()
      .then(currentUser => { if (!cancelled) setUser(currentUser); })
      .catch(() => { if (!cancelled) setUser(null); })
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const loggedInUser = await authService.login({ email, password });
    setUser(loggedInUser);
  }, []);

  const signup = useCallback(async (email: string, password: string, passwordConfirmation: string, name?: string) => {
    const newUser = await authService.signup({ email, password, passwordConfirmation, name });
    setUser(newUser);
    setJustSignedUp(true);
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    setUser(null);
    setJustSignedUp(false);
  }, []);

  const consumeJustSignedUp = useCallback(() => setJustSignedUp(false), []);

  return (
    <AuthContext.Provider value={{ user, isLoading, justSignedUp, login, signup, logout, consumeJustSignedUp }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within an AuthProvider');
  return context;
}
