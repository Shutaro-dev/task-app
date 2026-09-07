import axios from 'axios'
import type { User } from '../types'

const BASE = 'http://localhost:8080/api'

export interface SignupInput {
  email: string
  password: string
  passwordConfirmation: string
  name?: string
}

export interface LoginInput {
  email: string
  password: string
}

// バックエンドは Cookie セッションで認証状態を保持するため、
// 各リクエストで Cookie を送受信できるようにする (main.tsx で axios.defaults.withCredentials = true も設定)
export async function fetchCurrentUser(): Promise<User | null> {
  try {
    const { data } = await axios.get<User>(`${BASE}/session`, { withCredentials: true })
    return data
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 401) return null
    throw error
  }
}

export async function login({ email, password }: LoginInput): Promise<User> {
  const { data } = await axios.post<User>(`${BASE}/session`, { email, password }, { withCredentials: true })
  return data
}

export async function signup(input: SignupInput): Promise<User> {
  const { data } = await axios.post<User>(`${BASE}/users`, input, { withCredentials: true })
  return data
}

export async function logout(): Promise<void> {
  await axios.delete(`${BASE}/session`, { withCredentials: true })
}

// axios のエラーレスポンスからサーバーのエラーメッセージを取り出す共通ヘルパー
export function extractErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error) && typeof error.response?.data?.error === 'string') {
    return error.response.data.error
  }
  return fallback
}
