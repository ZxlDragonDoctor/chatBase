export type OpencodeNode = {
  id: number
  name: string
  platform: string | null
  agentVersion: string | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED' | string
  online: boolean
  capabilitiesPending: boolean
  lastSeenAt: string | null
  capabilities: string[]
  approvedCapabilities: string[]
}

export type PairingCode = {
  code: string
  expiresAt: string
}
