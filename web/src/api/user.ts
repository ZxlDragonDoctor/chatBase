import { api } from './client'

export type UserVO = {
  id: number
  username: string
  nickname: string
  avatar?: string
  email?: string
  phone?: string
  role: string
  createTime?: string
}

export type LoginResponse = {
  success: boolean
  token?: string
  user?: UserVO
  message?: string
}

export type UpdateProfileRequest = {
  nickname?: string
  email?: string
  phone?: string
}

export type ChangePasswordRequest = {
  oldPassword: string
  newPassword: string
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

export async function updateUserProfile(username: string, data: UpdateProfileRequest): Promise<LoginResponse> {
  const resp = await api.put<LoginResponse>('/user/info', null, {
    params: {
      username,
      nickname: data.nickname || undefined,
      email: data.email || undefined,
      phone: data.phone || undefined
    }
  })
  return resp.data
}

export async function changePassword(username: string, oldPassword: string, newPassword: string): Promise<LoginResponse> {
  const resp = await api.post<LoginResponse>('/user/change-password', null, {
    params: {
      username,
      oldPassword,
      newPassword
    }
  })
  return resp.data
}

export async function uploadAvatar(username: string, file: File): Promise<LoginResponse> {
  const formData = new FormData()
  formData.append('username', username)
  formData.append('file', file)
  
  const resp = await api.post<LoginResponse>('/user/avatar/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return resp.data
}

export async function logout(): Promise<void> {
  await api.post('/user/logout')
}