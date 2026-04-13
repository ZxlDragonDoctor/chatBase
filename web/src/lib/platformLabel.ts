/** 与库表 platform 字段对齐：qq / wx（企微） */
export function platformLabel(platform: string | null | undefined): string {
  const p = (platform || '').toLowerCase()
  if (p === 'qq') return 'QQ 群'
  if (p === 'wx' || p === 'wecom') return '企微群聊'
  return platform || '未知'
}
