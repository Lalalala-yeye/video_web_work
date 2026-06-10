import { get, postParams } from '@/network/request'

export function openMessageRoom(peerId) {
  return postParams('/message/room/open', { peerId })
}

export function fetchMessageRoom(roomId, page = 1, size = 50) {
  return get('/message/room/get', { roomId, page, size })
}

export function sendMessage(roomId, content) {
  return postParams('/message/send', { roomId, content })
}
