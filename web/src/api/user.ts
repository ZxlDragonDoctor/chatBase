import { api } from './client'

export type UserVO = {
  id: number
  username: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  role: string
}

export type LoginResponse = {
  success: boolean
  token?: string
  user?: UserVO
  message?: string
}

export async function register(username: string, password: string, nickname?: string, email?: string, phone?: string): Promise<LoginResponse> {
  const resp = await api.post<LoginResponse>('/user/register', { username, password, nickname, email, phone })
  return resp.data
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const resp = await api.post<LoginResponse>('/user/login', { username, password })
  return resp.data
}

export async function getCurrentUser(username: string): Promise<UserVO | null> {
  try {
    const resp = await api.get<UserVO>('/user/info', { params: { username } })
    return resp.data
  } catch {
    return null
  }
}

export async function logout(): Promise<void> {
  await api.post('/user/logout')
}