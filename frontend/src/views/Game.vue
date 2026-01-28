<template>
  <div class="min-h-screen flex flex-col overflow-hidden">
    <!-- Game Header -->
    <header class="bg-western-dark/80 border-b border-western-gold/30 px-4 py-2 flex justify-between items-center">
      <div class="flex items-center gap-4">
        <h1 class="font-western text-2xl text-western-gold">BANG!</h1>
        <span class="text-western-sand/60">Room: {{ roomId }}</span>
      </div>
      
      <div class="flex items-center gap-4">
        <div class="text-western-sand">
          <span class="text-western-gold">{{ currentPlayerName }}</span>'s turn
        </div>
        <div class="px-3 py-1 rounded bg-western-dark/50 text-western-sand text-sm">
          {{ phaseDisplay }}
        </div>
      </div>
    </header>

    <!-- Game Table -->
    <main class="flex-1 relative game-table">
      <!-- Center Area -->
      <div class="game-center absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 
                  flex items-center justify-center gap-8">
        <!-- Draw Pile -->
        <div class="deck-pile">
          <div class="card-game card-back flex items-center justify-center">
            <span class="text-western-sand font-bold">{{ drawPileSize }}</span>
          </div>
        </div>

        <!-- Discard Pile -->
        <div v-if="topDiscardCard" class="relative">
          <CardComponent :card="topDiscardCard" :disabled="true" />
        </div>
        <div v-else class="card-game opacity-30 flex items-center justify-center">
          <span class="text-western-dark/50">Discard</span>
        </div>
      </div>

      <!-- Other Players (Circular Layout) -->
      <PlayerSeat
        v-for="(player, index) in otherPlayers"
        :key="player.id"
        :player="player"
        :position="getPlayerPosition(index, otherPlayers.length)"
        :isCurrentTurn="player.id === gameState?.currentPlayerId"
        :isTargetable="canTargetPlayer(player)"
        :isPendingAction="player.id === gameState?.pendingActionPlayerId"
        @select="selectTarget(player)"
      />

      <!-- Action Prompt -->
      <div v-if="needsToRespond" 
           class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-40
                  bg-western-dark/95 border-2 border-western-gold rounded-xl p-6 text-center max-w-md">
        <h3 class="text-xl font-bold text-western-gold mb-4">
          {{ getActionPrompt() }}
        </h3>
        <p class="text-western-sand mb-4">
          Play a {{ requiredCardType }} card or take the hit!
        </p>
        <div class="flex justify-center gap-4">
          <button @click="takeHit" class="btn-danger">
            Take Hit
          </button>
        </div>
      </div>

      <!-- Current Player Area (Bottom) -->
      <div class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-western-dark via-western-dark/80 to-transparent pt-16 pb-4">
        <!-- Player Info -->
        <div class="flex justify-center mb-4">
          <div class="flex items-center gap-6 bg-western-dark/60 rounded-xl px-6 py-3 border border-western-gold/30">
            <!-- Character Info -->
            <div class="text-center">
              <div class="text-western-gold font-semibold">{{ currentPlayer?.characterName }}</div>
              <div class="text-western-sand/60 text-xs max-w-xs truncate">
                {{ currentPlayer?.characterAbility }}
              </div>
            </div>

            <!-- Divider -->
            <div class="w-px h-10 bg-western-gold/30"></div>

            <!-- Health -->
            <div class="flex items-center gap-2">
              <span class="text-western-sand/60 text-sm">HP:</span>
              <HealthBar :current="currentPlayer?.health || 0" :max="currentPlayer?.maxHealth || 4" />
            </div>

            <!-- Divider -->
            <div class="w-px h-10 bg-western-gold/30"></div>

            <!-- Role (only visible to self) -->
            <div class="text-center">
              <div class="text-xs text-western-sand/60">Role</div>
              <div class="font-semibold" :class="getRoleColor(currentPlayer?.role)">
                {{ currentPlayer?.role || '???' }}
              </div>
            </div>
          </div>
        </div>

        <!-- Hand -->
        <div class="flex justify-center">
          <div class="flex items-end justify-center gap-1 px-4 overflow-x-auto max-w-full">
            <CardComponent
              v-for="card in myHand"
              :key="card.id"
              :card="card"
              :selected="selectedCard?.id === card.id"
              :playable="isCardPlayable(card)"
              @click="handleCardClick(card)"
            />
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="flex justify-center gap-4 mt-4">
          <button
            v-if="isMyTurn && phase === 'DRAW_PHASE'"
            @click="drawCards"
            class="btn-western"
          >
            Draw Cards
          </button>

          <button
            v-if="isMyTurn && phase === 'PLAY_PHASE'"
            @click="passTurn"
            class="btn-western"
          >
            End Turn
          </button>

          <button
            v-if="selectedCard && needsTarget"
            @click="cancelSelection"
            class="btn-danger"
          >
            Cancel
          </button>
        </div>
      </div>
    </main>

    <!-- Game Over Overlay -->
    <div v-if="isGameOver" 
         class="fixed inset-0 bg-black/80 flex items-center justify-center z-50">
      <div class="bg-western-dark border-4 border-western-gold rounded-2xl p-12 text-center">
        <h2 class="font-western text-5xl text-western-gold mb-4">Game Over!</h2>
        <p class="text-2xl text-western-sand mb-2">
          {{ getWinnerText() }}
        </p>
        <button @click="returnToLobby" class="btn-western mt-8">
          Return to Menu
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import CardComponent from '../components/CardComponent.vue'
import PlayerSeat from '../components/PlayerSeat.vue'
import HealthBar from '../components/HealthBar.vue'

const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()

const selectedCard = ref(null)
const selectedTarget = ref(null)

const roomId = computed(() => route.params.roomId)
const gameState = computed(() => gameStore.gameState)
const currentPlayer = computed(() => gameStore.currentPlayer)
const myHand = computed(() => currentPlayer.value?.hand || [])
const otherPlayers = computed(() => gameStore.otherPlayers)
const isMyTurn = computed(() => gameStore.isMyTurn)
const phase = computed(() => gameStore.phase)
const needsToRespond = computed(() => gameStore.needsToRespond)
const drawPileSize = computed(() => gameState.value?.drawPileSize || 0)
const topDiscardCard = computed(() => gameState.value?.topDiscardCard)
const isGameOver = computed(() => gameState.value?.phase === 'GAME_OVER')

const currentPlayerName = computed(() => {
  const player = gameState.value?.players?.find(p => p.id === gameState.value?.currentPlayerId)
  return player?.name || 'Unknown'
})

const phaseDisplay = computed(() => {
  const phases = {
    'DRAW_PHASE': 'Draw Phase',
    'PLAY_PHASE': 'Play Phase',
    'DISCARD_PHASE': 'Discard Phase',
    'REACTION_PHASE': 'Reaction!',
    'GAME_OVER': 'Game Over'
  }
  return phases[phase.value] || phase.value
})

const needsTarget = computed(() => {
  if (!selectedCard.value) return false
  const type = selectedCard.value.type
  return ['BANG', 'DUEL', 'PANIC', 'CAT_BALOU', 'JAIL'].includes(type)
})

const requiredCardType = computed(() => {
  const actionType = gameState.value?.pendingActionType
  if (actionType === 'BANG' || actionType === 'GATLING') return 'Missed!'
  if (actionType === 'INDIANS' || actionType === 'DUEL') return 'BANG!'
  return 'response'
})

onMounted(() => {
  window.addEventListener('game-message', handleGameMessage)
  window.addEventListener('game-event', handleGameEvent)
  console.log('Game.vue mounted, requesting game state...')
  gameStore.requestGameState()
  
  // Retry after a short delay if no state received
  setTimeout(() => {
    if (!gameState.value) {
      console.log('No game state received, retrying...')
      gameStore.requestGameState()
    }
  }, 1000)
})

onUnmounted(() => {
  window.removeEventListener('game-message', handleGameMessage)
  window.removeEventListener('game-event', handleGameEvent)
})

function handleGameMessage(event) {
  gameStore.handleGameMessage(event.detail)
}

function handleGameEvent(event) {
  gameStore.handleGameEvent(event.detail)
}

function getPlayerPosition(index, total) {
  // Distribute players in a semi-circle at the top
  const angleSpread = 160 // degrees
  const startAngle = -90 - (angleSpread / 2)
  const angleStep = angleSpread / (total + 1)
  const angle = startAngle + (angleStep * (index + 1))
  
  const radiusX = 38 // % from center
  const radiusY = 35 // % from center
  
  const x = 50 + radiusX * Math.cos(angle * Math.PI / 180)
  const y = 45 + radiusY * Math.sin(angle * Math.PI / 180)
  
  return { x, y }
}

function isCardPlayable(card) {
  if (!isMyTurn.value && !needsToRespond.value) return false
  
  if (needsToRespond.value) {
    const actionType = gameState.value?.pendingActionType
    if (actionType === 'BANG' || actionType === 'GATLING') {
      return card.type === 'MISSED' || (currentPlayer.value?.characterName === 'Calamity Janet' && card.type === 'BANG')
    }
    if (actionType === 'INDIANS' || actionType === 'DUEL') {
      return card.type === 'BANG' || (currentPlayer.value?.characterName === 'Calamity Janet' && card.type === 'MISSED')
    }
    return false
  }

  if (phase.value === 'DISCARD_PHASE') return true
  if (phase.value !== 'PLAY_PHASE') return false
  
  return true
}

function canTargetPlayer(player) {
  if (!selectedCard.value || !needsTarget.value) return false
  if (!player.alive) return false
  
  // For BANG, check weapon range
  if (selectedCard.value.type === 'BANG') {
    const myPlayer = currentPlayer.value
    // Simplified range check - actual distance calculation is on backend
    return true
  }
  
  // For Panic, must be distance 1
  if (selectedCard.value.type === 'PANIC') {
    return true // Backend validates
  }
  
  return true
}

function handleCardClick(card) {
  if (needsToRespond.value && isCardPlayable(card)) {
    // Respond to action with this card
    gameStore.respondToAction(card.id, true)
    return
  }

  if (phase.value === 'DISCARD_PHASE') {
    gameStore.discardCard(card.id)
    return
  }

  if (!isCardPlayable(card)) return

  if (selectedCard.value?.id === card.id) {
    // Deselect
    selectedCard.value = null
    return
  }

  selectedCard.value = card

  // If card doesn't need target, play immediately
  if (!needsTarget.value) {
    playSelectedCard()
  }
}

function selectTarget(player) {
  if (!selectedCard.value || !canTargetPlayer(player)) return
  
  gameStore.playCard(selectedCard.value.id, player.id)
  selectedCard.value = null
}

function playSelectedCard(targetId = null) {
  if (!selectedCard.value) return
  gameStore.playCard(selectedCard.value.id, targetId)
  selectedCard.value = null
}

function cancelSelection() {
  selectedCard.value = null
}

function drawCards() {
  gameStore.drawCards()
}

function passTurn() {
  gameStore.passTurn()
}

function takeHit() {
  gameStore.respondToAction(null, false)
}

function getActionPrompt() {
  const actionType = gameState.value?.pendingActionType
  const sourceName = gameState.value?.players?.find(
    p => p.id === gameState.value?.pendingActionSourcePlayerId
  )?.name || 'Someone'

  if (actionType === 'BANG') return `${sourceName} shot at you!`
  if (actionType === 'DUEL') return `${sourceName} challenged you to a duel!`
  if (actionType === 'INDIANS') return `Indians are attacking!`
  if (actionType === 'GATLING') return `Gatling gun attack!`
  return 'You must respond!'
}

function getRoleColor(role) {
  const colors = {
    'SHERIFF': 'text-yellow-400',
    'DEPUTY': 'text-blue-400',
    'OUTLAW': 'text-red-400',
    'RENEGADE': 'text-purple-400'
  }
  return colors[role] || 'text-western-sand'
}

function getWinnerText() {
  const team = gameState.value?.winningTeam
  if (team === 'SHERIFF') return 'The Sheriff and Deputies win!'
  if (team === 'OUTLAW') return 'The Outlaws win!'
  if (team === 'RENEGADE') return 'The Renegade wins!'
  return 'Game Over'
}

function returnToLobby() {
  router.push('/')
}
</script>
