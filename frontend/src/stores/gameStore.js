import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useWebSocket } from '../composables/useWebSocket'
import { useAnimationQueue } from '../composables/useAnimationQueue'
import { useSoundManager } from '../composables/useSoundManager'

export const useGameStore = defineStore('game', () => {
  const { connect, disconnect, send, subscribeToRoom, connected } = useWebSocket()
  const { queueAnimation, processQueue, isProcessing } = useAnimationQueue()
  const { playSound } = useSoundManager()

  // State
  const playerId = ref(null)
  const playerName = ref('')
  const roomId = ref(null)
  const room = ref(null)
  const gameState = ref(null)
  const events = ref([])
  const isHost = ref(false)
  const error = ref(null)

  // Computed
  const currentPlayer = computed(() => {
    if (!gameState.value || !playerId.value) return null
    return gameState.value.players?.find(p => p.id === playerId.value)
  })

  const isMyTurn = computed(() => {
    return gameState.value?.currentPlayerId === playerId.value
  })

  const phase = computed(() => gameState.value?.phase)

  const otherPlayers = computed(() => {
    if (!gameState.value || !playerId.value) return []
    const allPlayers = gameState.value.players || []
    const myIndex = allPlayers.findIndex(p => p.id === playerId.value)
    
    if (myIndex === -1) return allPlayers.filter(p => p.id !== playerId.value)

    const reordered = []
    // Start from the player AFTER me, and wrap around
    for (let i = 1; i < allPlayers.length; i++) {
      const index = (myIndex + i) % allPlayers.length
      reordered.push(allPlayers[index])
    }
    return reordered
  })

  const needsToRespond = computed(() => {
    return gameState.value?.pendingActionPlayerId === playerId.value
  })

  // Actions
  async function connectToServer() {
    await connect()
    const storedRoomId = sessionStorage.getItem('bang_roomId')
    const storedPlayerId = sessionStorage.getItem('bang_playerId')
    const storedPlayerName = sessionStorage.getItem('bang_playerName')
    
    if (storedRoomId && storedPlayerId) {
      console.log('Found stored session, attempting reconnect...')
      roomId.value = storedRoomId
      playerId.value = storedPlayerId
      if (storedPlayerName) {
        playerName.value = storedPlayerName
      }
      
      // Resubscribe to room topics
      subscribeToRoom(storedRoomId, storedPlayerId)

      send('/app/room/reconnect', {
        type: 'JOIN', // Reconnect is a type of join
        roomId: storedRoomId,
        playerId: storedPlayerId
      })
    }
  }

  function createRoom(name, playerNameValue) {
    playerName.value = playerNameValue
    send('/app/room/create', {
      type: 'CREATE',
      roomName: name,
      playerName: playerNameValue
    })
  }

  function joinRoom(roomIdValue, playerNameValue) {
    playerName.value = playerNameValue
    send('/app/room/join', {
      type: 'JOIN',
      roomId: roomIdValue,
      playerName: playerNameValue
    })
  }

  function leaveRoom() {
    send('/app/room/leave', {})
    roomId.value = null
    room.value = null
    gameState.value = null
    isHost.value = false
    sessionStorage.removeItem('bang_roomId')
    sessionStorage.removeItem('bang_playerId')
    sessionStorage.removeItem('bang_playerName')
  }

  function setReady(ready) {
    send('/app/room/ready', { ready })
  }

  function startGame() {
    send('/app/room/start', {})
  }

  function requestGameState() {
    console.log('Requesting game state...')
    send('/app/game/state', {})
  }

  function drawCards() {
    send('/app/game/draw', {})
    playSound('draw')
  }

  function playCard(cardId, targetPlayerId = null, targetCardId = null) {
    send('/app/game/play', {
      cardId,
      targetPlayerId,
      targetCardId
    })
    playSound('play_card')
  }

  function passTurn() {
    send('/app/game/pass', {})
  }

  function kickPlayer(targetPlayerId) {
    send('/app/room/kick', {
      playerId: targetPlayerId
    })
  }

  function discardCard(cardId) {
    send('/app/game/discard', { cardId })
  }

  function respondToAction(cardId = null, accept = true) {
    send('/app/game/respond', {
      type: accept ? 'RESPOND' : 'PASS',
      cardId
    })
  }

  function useAbility(abilityId) {
    send('/app/game/useAbility', {
      cardId: abilityId
    })
  }

  function pickGeneralStoreCard(cardId) {
    send('/app/game/pickGeneralStore', {
      cardId
    })
    playSound('draw')
  }

  // Message handlers
  function handleLobbyMessage(message) {
    console.log('handleLobbyMessage:', message.type, message)
    
    switch (message.type) {
      case 'ROOM_CREATED':
      case 'ROOM_JOINED':
        roomId.value = message.roomId
        playerId.value = message.playerId
        room.value = message.payload
        isHost.value = message.payload?.hostId === message.playerId
        
        // Persist session
        sessionStorage.setItem('bang_roomId', message.roomId)
        sessionStorage.setItem('bang_playerId', message.playerId)
        if (playerName.value) {
          sessionStorage.setItem('bang_playerName', playerName.value)
        }
        
        console.log('Room state updated:', { roomId: roomId.value, playerId: playerId.value, isHost: isHost.value })
        break
      case 'ROOM_UPDATE':
        room.value = message.payload
        isHost.value = room.value?.hostId === playerId.value
        console.log('Room updated, players:', room.value?.players?.length)
        break
      case 'ROOM_LEFT':
        roomId.value = null
        room.value = null
        isHost.value = false
        break
      case 'GAME_STARTED':
        console.log('Game started, requesting state...')
        requestGameState()
        break
      case 'ROOM_KICKED':
        roomId.value = null
        room.value = null
        isHost.value = false
        sessionStorage.removeItem('bang_roomId')
        sessionStorage.removeItem('bang_playerId')
        sessionStorage.removeItem('bang_playerName')
        error.value = "You have been kicked from the room."
        break
      case 'ERROR':
        error.value = message.payload
        console.error('Lobby error:', message.payload)
        break
    }
  }

  function handleGameMessage(message) {
    console.log('Game message received:', message)
    
    if (message.type === 'ERROR') {
      error.value = message.payload
      console.error('Game error:', message.payload)
      return
    }

    // Queue animation before state update
    const oldState = gameState.value
    
    // Defer state update to sync with animations
    queueAnimation({
      type: 'STATE_UPDATE',
      updateFn: () => {
        gameState.value = message
      }
    })

    console.log('Game state updated (queued):', {
      phase: message.phase,
      currentPlayerId: message.currentPlayerId,
      players: message.players?.length,
      myPlayerId: playerId.value
    })

    // Process differences for animations
    if (oldState && message.phase !== oldState.phase) {
      // Phase changed
    }
  }

  function handleGameEvent(event) {
    events.value.push(event)

    // Queue animation based on event type
    queueAnimation(event)

    // Play appropriate sound
    switch (event.type) {
      case 'CARD_PLAYED':
        if (event.cardType === 'BANG') {
          playSound('gunshot')
        } else {
          playSound('play_card')
        }
        break
      case 'PLAYER_DAMAGED':
        playSound('damage')
        break
      case 'PLAYER_ELIMINATED':
        playSound('death')
        break
      case 'CARD_DRAWN':
        playSound('draw')
        break
    }

    // Keep only last 50 events
    if (events.value.length > 50) {
      events.value.shift()
    }
  }

  function clearError() {
    error.value = null
  }

  return {
    // State
    playerId,
    playerName,
    roomId,
    room,
    gameState,
    events,
    isHost,
    error,
    connected,
    isProcessing,

    // Computed
    currentPlayer,
    isMyTurn,
    phase,
    otherPlayers,
    needsToRespond,

    // Actions
    connectToServer,
    createRoom,
    joinRoom,
    leaveRoom,
    setReady,
    startGame,
    requestGameState,
    drawCards,
    playCard,
    passTurn,
    kickPlayer,
    discardCard,
    respondToAction,
    useAbility,
    pickGeneralStoreCard,
    handleLobbyMessage,
    handleGameMessage,
    handleGameEvent,
    clearError
  }
})
