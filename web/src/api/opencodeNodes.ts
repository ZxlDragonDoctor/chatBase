import { api } from './client'
import type { OpencodeNode, PairingCode } from '../types/opencodeNode'

export async function createPairingCode(): Promise<PairingCode> {
  const response = await api.post<PairingCode>('/opencode/nodes/pairing-codes')
  return response.data
}

export async function fetchOpencodeNodes(): Promise<OpencodeNode[]> {
  const response = await api.get<OpencodeNode[]>('/opencode/nodes')
  return response.data
}

export async function approveOpencodeNode(id: number): Promise<void> {
  await api.post(`/opencode/nodes/${id}/approve`)
}

export async function rejectOpencodeNode(id: number): Promise<void> {
  await api.post(`/opencode/nodes/${id}/reject`)
}

export async function renameOpencodeNode(id: number, name: string): Promise<void> {
  await api.put(`/opencode/nodes/${id}/name`, { name })
}

export async function revokeOpencodeNode(id: number): Promise<void> {
  await api.delete(`/opencode/nodes/${id}`)
}

export async function bindConversationNode(conversationId: number, nodeId: number): Promise<void> {
  await api.put(`/console/conversations/${conversationId}/opencode-node`, { nodeId })
}

export async function unbindConversationNode(conversationId: number): Promise<void> {
  await api.delete(`/console/conversations/${conversationId}/opencode-node`)
}
