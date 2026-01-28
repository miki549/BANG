<template>
  <div class="min-h-screen flex flex-col items-center justify-center p-8">
    <!-- Title -->
    <div class="text-center mb-12">
      <h1 class="font-western text-7xl text-western-gold mb-4 drop-shadow-lg">
        BANG!
      </h1>
      <p class="text-xl text-western-sand/80">The Wild West Card Game</p>
    </div>

    <!-- Main Menu -->
    <div class="w-full max-w-md space-y-6">
      <!-- Player Name Input -->
      <div>
        <label class="block text-western-sand mb-2 font-semibold">Your Name</label>
        <input
          v-model="playerName"
          type="text"
          placeholder="Enter your cowboy name..."
          class="input-western"
          maxlength="20"
        />
      </div>

      <!-- Create Room -->
      <div class="bg-western-dark/40 p-6 rounded-xl border border-western-gold/20">
        <h2 class="text-xl font-semibold text-western-gold mb-4">Create a Room</h2>
        <input
          v-model="roomName"
          type="text"
          placeholder="Room name..."
          class="input-western mb-4"
          maxlength="30"
        />
        <button
          @click="handleCreateRoom"
          :disabled="!canCreate"
          class="btn-western w-full disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Create Room
        </button>
      </div>

      <!-- Join Room -->
      <div class="bg-western-dark/40 p-6 rounded-xl border border-western-gold/20">
        <h2 class="text-xl font-semibold text-western-gold mb-4">Join a Room</h2>
        <input
          v-model="joinRoomId"
          type="text"
          placeholder="Enter Room Code..."
          class="input-western mb-4 uppercase"
          maxlength="6"
          @input="joinRoomId = joinRoomId.toUpperCase()"
        />
        <button
          @click="handleJoinRoom"
          :disabled="!canJoin"
          class="btn-western w-full disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Join Room
        </button>
      </div>

      <!-- Error Display -->
      <div v-if="error" class="bg-red-900/50 border border-red-500 p-4 rounded-lg text-center">
        <p class="text-red-300">{{ error }}</p>
        <button @click="gameStore.clearError()" class="text-red-400 underline mt-2">
          Dismiss
        </button>
      </div>
    </div>

    <!-- Footer -->
    <div class="mt-12 text-western-sand/50 text-sm">
      <p>4-7 Players • Based on the classic card game</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import { useWebSocket } from '../composables/useWebSocket'

const router = useRouter()
const gameStore = useGameStore()
const { subscribeToRoom } = useWebSocket()

const playerName = ref('')
const roomName = ref('')
const joinRoomId = ref('')

const canCreate = computed(() => playerName.value.trim().length >= 2)
const canJoin = computed(() => playerName.value.trim().length >= 2 && joinRoomId.value.length === 6)
const error = computed(() => gameStore.error)

onMounted(async () => {
  // Check if we have stored session data
  const storedRoomId = sessionStorage.getItem('roomId')
  const storedPlayerId = sessionStorage.getItem('playerId')
  const storedPlayerName = sessionStorage.getItem('playerName')
  
  await gameStore.connectToServer()
  window.addEventListener('lobby-message', handleLobbyMessage)
  window.addEventListener('room-message', handleRoomMessage)
})

onUnmounted(() => {
  window.removeEventListener('lobby-message', handleLobbyMessage)
  window.removeEventListener('room-message', handleRoomMessage)
})

function handleLobbyMessage(event) {
  const message = event.detail
  console.log('Home received lobby message:', message.type)
  gameStore.handleLobbyMessage(message)

  if (message.type === 'ROOM_CREATED' || message.type === 'ROOM_JOINED') {
    // Store session data for refresh recovery
    sessionStorage.setItem('roomId', message.roomId)
    sessionStorage.setItem('playerId', message.playerId)
    sessionStorage.setItem('playerName', playerName.value)
    
    subscribeToRoom(message.roomId, message.playerId)
    router.push(`/lobby/${message.roomId}`)
  }
}

function handleRoomMessage(event) {
  const message = event.detail
  gameStore.handleLobbyMessage(message)
}

function handleCreateRoom() {
  if (!canCreate.value) return
  gameStore.createRoom(roomName.value || 'Game Room', playerName.value.trim())
}

function handleJoinRoom() {
  if (!canJoin.value) return
  gameStore.joinRoom(joinRoomId.value, playerName.value.trim())
}
</script>
