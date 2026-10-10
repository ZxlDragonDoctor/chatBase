export function getCurrentUser(): string | null {
  return localStorage.getItem('chatbase_user')
}

/**
 * 稳定的会话归属 userId。
 * 固定使用 chatbase_user_id（匿名 uuid 或登录用户名），
 * 不要用 chatbase_user（显示名/昵称），否则改昵称后历史会话会丢失。
 */
export function getOrCreateUserId(): string {
  // 优先使用登录用户名（稳定），其次匿名 uuid
  const username = localStorage.getItem('chatbase_original_username')
  if (username && username.trim()) return username

  const key = 'chatbase_user_id'
  const existing = localStorage.getItem(key)
  if (existing && existing.trim()) return existing

  const uuid = typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(36).slice(2, 11)}`

  const id = `web-${uuid}`
  localStorage.setItem(key, id)
  return id
}

