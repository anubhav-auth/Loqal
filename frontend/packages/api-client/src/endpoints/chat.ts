import { api } from "../client"
import type { ChatMessage } from "../types"

export const chatApi = {
  history(roomId: string): Promise<ChatMessage[]> {
    return api.get<ChatMessage[]>(`/communication/chat/${roomId}/messages`)
  },

  postMessage(roomId: string, content: string): Promise<ChatMessage> {
    return api.post<ChatMessage>("/communication/chat/messages", {
      roomId,
      content,
    })
  },
}
