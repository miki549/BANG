<template>
  <div class="min-h-screen flex flex-col items-center justify-center p-8">
    <!-- Room Info -->
    <div class="text-center mb-8">
      <h1 class="font-western text-5xl text-western-gold mb-2">{{ room?.name || 'Game Lobby' }}</h1>
      <div class="flex items-center justify-center gap-2 text-western-sand/80">
        <span>Room Code:</span>
        <span class="font-mono text-2xl text-western-gold bg-western-dark/50 px-4 py-1 rounded">
          {{ roomId }}
        </span>
        <button 
          @click="copyRoomCode" 
          class="text-western-gold hover:text-yellow-400 transition-colors"
          title="Copy room code"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
            <path d="M8 3a1 1 0 011-1h2a1 1 0 110 2H9a1 1 0 01-1-1z" />
            <path d="M6 3a2 2 0 00-2 2v11a2 2 0 002 2h8a2 2 0 002-2V5a2 2 0 00-2-2 3 3 0 01-3 3H9a3 3 0 01-3-3z" />
          </svg>
        </button>
      </div>
    </div>

    <!-- Players List -->
    <div class="w-full max-w-2xl bg-western-dark/40 rounded-xl border border-western-gold/20 p-6 mb-8">
      <h2 class="text-xl font-semibold text-western-gold mb-4">
        Players ({{ players.length }}/7)
      </h2>
      
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div
          v-for="player in players"
          :key="player.id"
          class="flex flex-col items-center p-4 rounded-lg transition-all"
          :class="[
            player.isHost ? 'bg-western-gold/20 border border-western-gold' : 'bg-western-dark/30 border border-western-sand/20',
            player.ready ? 'ring-2 ring-green-500' : ''
          ]"
        >
          <!-- Avatar Placeholder -->
          <div class="w-16 h-16 rounded-full bg-western-leather flex items-center justify-center mb-2">
            <span class="text-2xl font-bold text-western-sand">
              {{ player.name.charAt(0).toUpperCase() }}
            </span>
          </div>
          
          <span class="font-semibold text-western-sand truncate max-w-full">
            {{ player.name }}
          </span>
          
          <div class="flex items-center gap-2 mt-1">
            <span v-if="player.isHost" class="text-xs text-western-gold">HOST</span>
            <span 
              v-if="player.ready" 
              class="text-xs text-green-400"
            >
              READY
            </span>
          </div>
        </div>

        <!-- Empty Slots -->
        <div
          v-for="i in Math.max(0, 4 - players.length)"
          :key="'empty-' + i"
          class="flex flex-col items-center p-4 rounded-lg bg-western-dark/20 border border-dashed border-western-sand/20"
        >
          <div class="w-16 h-16 rounded-full bg-western-dark/50 flex items-center justify-center mb-2">
            <span class="text-2xl text-western-sand/30">?</span>
          </div>
          <span class="text-western-sand/30">Waiting...</span>
        </div>
      </div>

      <!-- Player Count Info -->
      <div class="mt-4 text-center text-western-sand/60 text-sm">
        <span v-if="players.length < 4">Need at least {{ 4 - players.length }} more player(s) to start</span>
        <span v-else-if="players.length >= 4">Ready to start!</span>
      </div>
    </div>

    <!-- Actions -->
    <div class="flex flex-col sm:flex-row gap-4">
      <button
        v-if="!isCurrentPlayerReady"
        @click="toggleReady"
        class="btn-western"
      >
        Ready Up
      </button>
      <button
        v-else
        @click="toggleReady"
        class="btn-danger"
      >
        Not Ready
      </button>

      <button
        v-if="isHost"
        @click="startGame"
        :disabled="!canStart"
        class="btn-western disabled:opacity-50 disabled:cursor-not-allowed"
      >
        Start Game
      </button>

      <button
        @click="leaveRoom"
        class="px-6 py-3 bg-transparent border-2 border-western-sand/50 text-western-sand rounded-lg
               hover:border-western-sand hover:bg-western-sand/10 transition-all"
      >
        Leave Room
      </button>
    </div>

    <!-- Error Display -->
    <div v-if="error" class="mt-6 bg-red-900/50 border border-red-500 p-4 rounded-lg text-center">
      <p class="text-red-300">{{ error }}</p>
      <button @click="gameStore.clearError()" class="text-red-400 underline mt-2">
        Dismiss
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import { useWebSocket } from '../composables/useWebSocket'

const { subscribeToRoom } = useWebSocket()

const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()

const roomId = computed(() => gameStore.roomId || route.params.roomId)
const room = computed(() => gameStore.room)
const players = computed(() => room.value?.players || [])
const isHost = computed(() => gameStore.isHost)
const error = computed(() => gameStore.error)

const currentPlayer = computed(() => {
  return players.value.find(p => p.id === gameStore.playerId)
})

const isCurrentPlayerReady = computed(() => currentPlayer.value?.ready || false)

const canStart = computed(() => {
  return players.value.length >= 4 && 
         players.value.length <= 7 && 
         players.value.every(p => p.ready)
})

onMounted(async () => {
  window.addEventListener('room-message', handleRoomMessage)
  window.addEventListener('lobby-message', handleLobbyMessage)
  
  // Handle page refresh - check if we have room state
  if (!room.value || !gameStore.playerId) {
    // Try to recover from session storage
    const storedRoomId = sessionStorage.getItem('roomId')
    const storedPlayerId = sessionStorage.getItem('playerId')
    const storedPlayerName = sessionStorage.getItem('playerName')
    
    if (storedRoomId && storedPlayerName && storedRoomId === route.params.roomId) {
      console.log('Attempting to rejoin room after refresh...')
      // Need to reconnect WebSocket and rejoin
      await gameStore.connectToServer()
      
      // Wait a moment for connection to establish
      setTimeout(() => {
        gameStore.joinRoom(storedRoomId, storedPlayerName)
      }, 500)
    } else {
      // No valid session, redirect to home
      console.log('No valid session found, redirecting to home')
      sessionStorage.clear()
      router.push('/')
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('room-message', handleRoomMessage)
  window.removeEventListener('lobby-message', handleLobbyMessage)
})

watch(() => gameStore.gameState, (newState) => {
  if (newState) {
    router.push(`/game/${roomId.value}`)
  }
})

function handleRoomMessage(event) {
  const message = event.detail
  gameStore.handleLobbyMessage(message)

  if (message.type === 'GAME_STARTED') {
    router.push(`/game/${roomId.value}`)
  }
}

function handleLobbyMessage(event) {
  const message = event.detail
  console.log('Lobby received lobby message:', message.type)
  gameStore.handleLobbyMessage(message)
  
  // Handle rejoin after page refresh
  if (message.type === 'ROOM_JOINED') {
    sessionStorage.setItem('roomId', message.roomId)
    sessionStorage.setItem('playerId', message.playerId)
    subscribeToRoom(message.roomId, message.playerId)
  }
}

function toggleReady() {
  gameStore.setReady(!isCurrentPlayerReady.value)
}

function startGame() {
  if (canStart.value) {
    gameStore.startGame()
  }
}

function leaveRoom() {
  sessionStorage.clear()
  gameStore.leaveRoom()
  router.push('/')
}

function copyRoomCode() {
  navigator.clipboard.writeText(roomId.value)
}
</script>
