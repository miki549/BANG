import { ref } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'

const client = ref(null)
const connected = ref(false)
const sessionId = ref(null)
const playerId = ref(null)

export function useWebSocket() {
  function connect() {
    return new Promise((resolve, reject) => {
      const stompClient = new Client({
        webSocketFactory: () => new SockJS('/ws'), // Use relative path to allow proxying
        connectHeaders: {},
        debug: (str) => {
          if (str.includes('CONNECTED') || str.includes('ERROR')) {
            console.log('STOMP: ' + str)
          }
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000
      })

      stompClient.onConnect = (frame) => {
        connected.value = true
        console.log('Connected to WebSocket')

        // Subscribe to user-specific queues
        stompClient.subscribe('/user/queue/lobby', (message) => {
          const data = JSON.parse(message.body)
          window.dispatchEvent(new CustomEvent('lobby-message', { detail: data }))
        })

        stompClient.subscribe('/user/queue/game', (message) => {
          const data = JSON.parse(message.body)
          window.dispatchEvent(new CustomEvent('game-message', { detail: data }))
        })

        resolve(stompClient)
      }

      stompClient.onStompError = (frame) => {
        console.error('STOMP error:', frame)
        connected.value = false
        reject(new Error(frame.headers?.message || 'WebSocket error'))
      }

      stompClient.onWebSocketClose = () => {
        connected.value = false
        console.log('WebSocket closed')
      }

      stompClient.activate()
      client.value = stompClient
    })
  }

  function disconnect() {
    if (client.value) {
      client.value.deactivate()
      client.value = null
      connected.value = false
    }
  }

  function send(destination, body) {
    if (client.value && connected.value) {
      client.value.publish({
        destination,
        body: JSON.stringify(body)
      })
    } else {
      console.warn('WebSocket not connected')
    }
  }

  function subscribeToRoom(roomId, playerIdValue) {
    if (!client.value || !connected.value) return

    playerId.value = playerIdValue

    // Subscribe to room updates
    client.value.subscribe(`/topic/room/${roomId}`, (message) => {
      const data = JSON.parse(message.body)
      window.dispatchEvent(new CustomEvent('room-message', { detail: data }))
    })

    // Subscribe to game events
    client.value.subscribe(`/topic/room/${roomId}/events`, (message) => {
      const data = JSON.parse(message.body)
      window.dispatchEvent(new CustomEvent('game-event', { detail: data }))
    })

    // Subscribe to game state updates
    client.value.subscribe(`/topic/room/${roomId}/state`, (message) => {
      const data = JSON.parse(message.body)
      window.dispatchEvent(new CustomEvent('game-state', { detail: data }))
    })

    // Subscribe to personalized player state
    if (playerIdValue) {
      client.value.subscribe(`/topic/room/${roomId}/player/${playerIdValue}`, (message) => {
        const data = JSON.parse(message.body)
        window.dispatchEvent(new CustomEvent('game-message', { detail: data }))
      })
    }
  }

  return {
    connect,
    disconnect,
    send,
    subscribeToRoom,
    connected,
    sessionId,
    playerId
  }
}
