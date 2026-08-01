import { api } from './client'

export interface BotInfo {
  platform: 'qq' | 'wecom' | 'wx'
  name: string
  botId: string | null
  online: boolean
  groupCount: number
  todayMessages: number
  totalMessages: number
  lastActiveTime: string | null
}

export async function listBots(): Promise<BotInfo[]> {
  const resp = await api.get<BotInfo[]>('/bot/list')
  return resp.data
}

export interface WxQrCode {
  qrcode_img_content: string
  qrcode: string
}

export interface WxQrCodeStatus {
  status: string
  bot_token?: string
  baseurl?: string
  nickname?: string
  error?: string
}

export interface WxBotStatus {
  online: boolean
  nickname: string
}

export async function getWxQrCode(): Promise<WxQrCode> {
  const resp = await api.get<WxQrCode>('/wx/bot/qrcode')
  return resp.data
}

export async function pollWxQrCodeStatus(qrcode: string): Promise<WxQrCodeStatus> {
  const resp = await api.get<WxQrCodeStatus>('/wx/bot/qrcode/status', { params: { qrcode } })
  return resp.data
}

export async function getWxBotStatus(): Promise<WxBotStatus> {
  const resp = await api.get<WxBotStatus>('/wx/bot/status')
  return resp.data
}

export async function disconnectWxBot(): Promise<void> {
  await api.post('/wx/bot/disconnect')
}

export interface QqQrCodeResponse {
  qrcode_img?: string
  qrcode_url?: string
  error?: string
}

export interface QqLoginStatus {
  isLogin: boolean
  isOffline: boolean
  loginError?: string
  qrcode_url?: string
}

export async function getQqQrCode(): Promise<QqQrCodeResponse> {
  const resp = await api.get<QqQrCodeResponse>('/qq-bot/qrcode')
  return resp.data
}

export async function getQqQrCodeStatus(): Promise<QqLoginStatus> {
  const resp = await api.get<QqLoginStatus>('/qq-bot/qrcode/status')
  return resp.data
}

export async function refreshQqQrCode(): Promise<void> {
  await api.post('/qq-bot/qrcode/refresh')
}

export async function getQqLoginInfo(): Promise<{ online: boolean; uin?: string; nick?: string }> {
  const resp = await api.get('/qq-bot/status')
  return resp.data
}
