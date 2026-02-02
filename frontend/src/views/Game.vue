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

      <!-- Other Players (Elliptical Layout) -->
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
      <div class="absolute bottom-0 left-0 right-0 p-2 flex flex-col items-center justify-end z-30
                  bg-gradient-to-t from-black/90 via-black/70 to-transparent pt-12">
        
        <div class="flex items-end gap-6 w-full max-w-7xl justify-center">
            <!-- My Player Board -->
            <div class="player-board relative transform-none w-[340px] h-32 flex-shrink-0 mb-4"
                 :class="{ 'current-turn': isMyTurn }">
                <!-- Left: Character & Role -->
                <div class="w-20 h-full border-r border-amber-800/50 bg-stone-800 relative flex-shrink-0">
                  <!-- Character -->
                  <div class="absolute inset-1 bg-stone-700 rounded border border-stone-600 overflow-hidden">
                       <img
                         v-if="currentPlayer?.characterName"
                         :src="getCharacterImage(currentPlayer.characterName)"
                         :alt="currentPlayer.characterName"
                         class="w-full h-full object-cover"
                       />
                       <div v-else class="w-full h-full flex items-center justify-center text-3xl">🤠</div>
                  </div>
                  
                  <!-- Role Overlay (Small) or separate? -->
                  <!-- In the main dashboard, maybe we want the role separate like in PlayerSeat? -->
                  <!-- But the dashboard structure here is different. Let's try to fit it. -->
                  <!-- Actually, let's use a separate slot for Role if space allows, or overlay. -->
                  <div class="absolute -bottom-2 -right-2 w-10 h-14 rounded border border-stone-600 overflow-hidden shadow-md z-20">
                      <img
                         v-if="currentPlayer?.role"
                         :src="getRoleImage(currentPlayer.role)"
                         :alt="currentPlayer.role"
                         class="w-full h-full object-cover"
                      />
                  </div>
                </div>

                <!-- Middle: Stats -->
                <div class="board-section-middle">
                  <div class="flex flex-col">
                      <div class="player-name text-xl">{{ currentPlayer?.name || 'You' }}</div>
                      <div class="text-xs text-western-gold/80 truncate">{{ currentPlayer?.characterName }}</div>
                      <div class="text-[10px] text-western-sand/50 truncate max-w-[140px]" :title="currentPlayer?.characterAbility">
                        {{ currentPlayer?.characterAbility }}
                      </div>
                  </div>

                  <div class="health-container mt-2">
                     <div v-for="i in (currentPlayer?.maxHealth || 4)" :key="i"
                          class="bullet-slot w-3 h-5"
                          :class="{ 'full': i <= (currentPlayer?.health || 0) }">
                     </div>
                  </div>
                </div>

                <!-- Right: Equipment -->
                <div class="board-section-right w-24">
                   <!-- Weapon -->
                   <div class="equipment-slot h-10" :title="currentPlayer?.weapon ? currentPlayer.weapon.name : 'Colt .45'">
                      <div v-if="currentPlayer?.weapon" class="card-mini weapon-card text-xs">{{ currentPlayer.weapon.type?.replace('_', ' ') }}</div>
                      <div v-else class="card-mini text-gray-500 opacity-50 text-xl">🔫</div>
                   </div>
                   
                   <!-- Blue Cards -->
                   <div class="flex-1 grid grid-cols-2 gap-1 overflow-hidden content-start">
                      <div v-for="card in currentPlayer?.cardsInPlay" :key="card.id"
                           class="equipment-slot h-8"
                           :title="card.type">
                         <div class="card-mini blue-card text-xs">{{ getCardMiniIcon(card.type) }}</div>
                      </div>
                   </div>
                </div>
            </div>

            <!-- Hand Cards (Fan Layout) -->
            <div class="flex-1 flex justify-center items-end -mb-4 overflow-visible pb-12 perspective-1000">
              <div class="relative flex justify-center items-end w-full h-40">
                <div v-for="(card, index) in myHand"
                     :key="card.id"
                     class="absolute transition-all duration-300 transform-gpu origin-bottom-center"
                     :style="getFanStyle(index, myHand.length, selectedCard?.id === card.id)"
                     @mouseenter="hoveredCardIndex = index"
                     @mouseleave="hoveredCardIndex = -1"
                     @click="handleCardClick(card)">
                  <CardComponent
                    :card="card"
                    :selected="selectedCard?.id === card.id"
                    :playable="isCardPlayable(card)"
                    :style="{ transform: 'none' }"
                    class="shadow-2xl !transition-none"
                  />
                </div>
              </div>
            </div>
            
            <!-- Actions -->
            <div class="flex flex-col gap-2 mb-8 min-w-[120px]">
              <button
                v-if="isMyTurn && phase === 'DRAW_PHASE'"
                @click="drawCards"
                class="btn-western w-full"
              >
                Draw
              </button>

              <button
                v-if="isMyTurn && phase === 'PLAY_PHASE'"
                @click="passTurn"
                class="btn-western w-full"
              >
                End Turn
              </button>

              <button
                v-if="selectedCard && needsTarget"
                @click="cancelSelection"
                class="btn-danger w-full"
              >
                Cancel
              </button>
            </div>
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
const hoveredCardIndex = ref(-1)

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

onMounted(async () => {
  window.addEventListener('game-message', handleGameMessage)
  window.addEventListener('game-event', handleGameEvent)
  console.log('Game.vue mounted, connecting...')
  
  await gameStore.connectToServer()
  
  // If we have state from reconnect, request game state
  if (gameStore.roomId && gameStore.playerId) {
    console.log('Connected and session restored, requesting game state...')
    gameStore.requestGameState()
  }

  // Retry logic
  setTimeout(() => {
    if (!gameState.value && gameStore.connected) {
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
  // Elliptical distribution for up to 7 players (6 opponents)
  // We want to distribute them from Left-Bottom (160deg) to Right-Bottom (380deg)
  // going clockwise through Top (270deg).
  
  const xRadius = 38; // Reduced to prevent overflow
  const yRadius = 35; // Slightly reduced
  const centerX = 50;
  const centerY = 40; // Moved up slightly to give more room for player dashboard

  const startAngle = 160;
  const endAngle = 380;
  const range = endAngle - startAngle;
  
  let angle;
  if (total <= 1) {
    angle = 270; // Top
  } else {
    // Distribute evenly along the arc
    const step = range / (total - 1);
    angle = startAngle + (step * index);
  }
  
  // Convert to radians
  const rad = angle * (Math.PI / 180);
  
  const x = centerX + xRadius * Math.cos(rad);
  const y = centerY + yRadius * Math.sin(rad);
  
  return { x, y };
}

function getCardMiniIcon(type) {
  const icons = {
    'BARREL': '🛢️',
    'MUSTANG': '🐴',
    'SCOPE': '🔭',
    'JAIL': '⛓️',
    'DYNAMITE': '🧨'
  }
  return icons[type] || '📄'
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

function getCharacterImage(name) {
  if (!name) return '';
  const cleanName = name.replace(/[^a-zA-Z]/g, '').toLowerCase();
  return `/images/characters/${cleanName}.png`;
}

function getRoleImage(role) {
  if (!role) return '';
  return `/images/roles/${role.toLowerCase()}.png`;
}

function getFanStyle(index, total, isSelected) {
  if (total === 0) return {};
  
  // Calculate angle
  const maxAngle = 30; // Reduced angle for easier selection
  const angleStep = Math.min(maxAngle / (total - 1 || 1), 5); // Max 5 degrees per card
  const startAngle = -((total - 1) * angleStep) / 2;
  let angle = startAngle + (index * angleStep);
  
  // Calculate offset (arch effect)
  let yOffset = Math.abs(angle) * 1.5;
  let xOffset = (index - (total - 1) / 2) * 45; // Increased base spacing
  let scale = 1;
  let zIndex = index;
  let yTrans = 0;

  // Hover Spread Effect
  if (hoveredCardIndex.value !== -1) {
    const diff = index - hoveredCardIndex.value;
    const spreadAmount = 60; // Constant gap size

    if (diff === 0) {
       // Current hovered card
       scale = 1.3;
       zIndex = 100;
       yTrans = -60;
       yOffset = 0; // Flatten arch for hovered
       angle = 0; // Straighten
    } else if (diff < 0) {
       // Shift ALL left neighbors equally to preserve their relative overlap
       xOffset -= spreadAmount;
    } else {
       // Shift ALL right neighbors equally
       xOffset += spreadAmount;
    }
  }

  if (isSelected) {
      yTrans -= 40;
      scale = Math.max(scale, 1.1);
  }

  return {
    transform: `translateX(${xOffset}px) translateY(${yOffset + yTrans}px) rotate(${angle}deg) scale(${scale})`,
    zIndex: zIndex
  };
}
</script>
