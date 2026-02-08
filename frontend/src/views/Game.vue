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
      <div class="game-center absolute top-[50%] left-[50%] transform -translate-x-1/2 -translate-y-1/2
                  flex items-center justify-center gap-8">
        <!-- Draw Pile -->
        <div class="deck-pile" data-deck-pile>
          <div class="card-game card-back flex items-center justify-center">
            <span class="text-western-sand font-bold">{{ drawPileSize }}</span>
          </div>
        </div>

        <!-- Discard Pile -->
        <div class="relative discard-pile-container" data-discard-pile>
          <div v-if="topDiscardCard" class="relative">
            <CardComponent :card="topDiscardCard" :disabled="true" />
          </div>
          <div v-else class="card-game opacity-30 flex items-center justify-center">
            <span class="text-western-dark/50">Discard</span>
          </div>
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
        @inspect="handleInspect"
      />

      <!-- Action Prompt -->
      <div v-if="needsToRespond && !isProcessing && phase !== 'GENERAL_STORE_PHASE'"
           class="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-40
                  bg-western-dark/95 border-2 border-western-gold rounded-xl p-6 text-center max-w-md">
        <h3 class="text-xl font-bold text-western-gold mb-4">
          {{ getActionPrompt() }}
        </h3>
        <p class="text-western-sand mb-4">
          Play a {{ requiredCardType }} card or take the hit!
        </p>
        <div class="flex flex-col gap-2 items-center">
            <div class="flex justify-center gap-4">
              <button @click="takeHit" class="btn-danger">
                Take Hit
              </button>
            </div>
            
            <div v-if="canUseBarrel || canUseJourdonnais" class="flex justify-center gap-4 mt-2">
                <button v-if="canUseBarrel" @click="useBarrel" class="btn-western flex items-center gap-2 px-3 py-1 text-sm bg-blue-700 hover:bg-blue-600 border-blue-400">
                    <img src="/images/cards/barrel.png" class="w-6 h-6 object-contain" />
                    Use Barrel
                </button>
                <button v-if="canUseJourdonnais" @click="useJourdonnais" class="btn-western flex items-center gap-2 px-3 py-1 text-sm bg-purple-700 hover:bg-purple-600 border-purple-400">
                    🤠 Use Jourdonnais
                </button>
            </div>
        </div>
      </div>

      <!-- General Store Overlay -->
      <div v-if="phase === 'GENERAL_STORE_PHASE'"
           class="fixed inset-0 bg-black/60 z-50 flex flex-col items-center justify-center pointer-events-none">
           <div class="bg-western-dark/95 border-4 border-western-gold rounded-2xl p-8 max-w-4xl pointer-events-auto">
                <h3 class="text-3xl font-western text-western-gold mb-8 text-center">
                    General Store
                </h3>
                <p v-if="needsToRespond" class="text-xl text-green-400 text-center mb-6 font-bold animate-pulse">
                    Your turn to pick a card!
                </p>
                <p v-else class="text-xl text-western-sand text-center mb-6">
                    Waiting for {{ gameState?.players?.find(p => p.id === gameState?.pendingActionPlayerId)?.name }} to pick...
                </p>

                <div class="flex justify-center gap-6 flex-wrap">
                    <div v-for="card in gameState?.generalStoreCards"
                         :key="card.id"
                         class="transform transition-transform hover:scale-110 cursor-pointer"
                         @click="handleGeneralStorePick(card)">
                        <CardComponent :card="card" class="w-32 h-48 shadow-2xl" :class="{'ring-4 ring-green-500': needsToRespond}" />
                    </div>
                </div>
           </div>
      </div>

      <!-- Current Player Area (Bottom) -->
      <div class="absolute bottom-0 left-0 right-0 p-2 flex flex-col items-center justify-end z-30
                  bg-gradient-to-t from-black/90 via-black/70 to-transparent pt-12">
        
        <div class="flex items-end gap-6 w-full max-w-7xl justify-center">
            <!-- My Player Board -->
            <div class="player-board relative transform-none w-auto flex-shrink-0 mb-24 ml-80"
                 :class="{ 'current-turn': isMyTurn, 'pending-action': needsToRespond }"
                 :data-player-id="currentPlayer?.id">
              
              <div class="relative bg-stone-900/95 border-2 border-amber-800 rounded-lg shadow-xl overflow-visible w-auto flex flex-col">
                  <!-- Header: Name & Stats -->
                  <div class="flex justify-between items-center px-2 py-1 bg-black/40 border-b border-amber-800/30">
                    <div class="text-left font-bold text-western-sand text-shadow-sm truncate text-sm">
                      {{ currentPlayer?.name || 'You' }}
                    </div>
                    <div class="flex items-center gap-2">
                       <!-- Health -->
                       <div class="flex flex-wrap justify-center gap-0.4">
                          <div v-for="i in (currentPlayer?.maxHealth || 4)" :key="i"
                               class="w-6 h-6 rounded-full">
                               <img src="/images/common/hp.png" alt="HP"
                                    class="w-full h-full object-contain rotate-45 drop-shadow-sm transition-all duration-500"
                                    :class="{
                                      'opacity-30 grayscale': i > (currentPlayer?.health || 0) && !flashingIndices.has(i) && !flashingGreenIndices.has(i),
                                      'animate-flash-bullet': flashingIndices.has(i),
                                      'animate-flash-green-bullet': flashingGreenIndices.has(i)
                                    }" />
                          </div>
                       </div>
                       <!-- Hand Size -->
                       <div class="flex items-center gap-2 text-western-sand/90 text-lg font-bold">
                          <img src="/images/common/deck.png" alt="Cards" class="w-6 h-6 object-contain" />
                          <span class="font-mono">{{ currentPlayer?.handSize || 0 }}</span>
                       </div>
                    </div>
                  </div>

                  <!-- Slots Row -->
                  <div class="flex h-32">
                    <!-- LEFT: Role Card -->
                    <div class="w-24 h-full border-r border-amber-800/50 bg-stone-800 relative flex-shrink-0 z-10 hover:z-50">
                       <div class="w-full h-full overflow-visible relative transition-transform duration-300 hover:scale-125 cursor-help origin-center shadow-lg"
                            @wheel="handleCardWheel($event, { imageSrc: getRoleImage(currentPlayer.role), title: currentPlayer.role })">
                          <img
                             v-if="currentPlayer?.role"
                             :src="getRoleImage(currentPlayer.role)"
                             :alt="currentPlayer.role"
                             class="w-full h-full object-contain p-1"
                          />
                          <!-- Hidden Role Card -->
                          <div v-else class="w-full h-full flex items-center justify-center p-1">
                            <div class="h-full aspect-[2/3] rounded bg-stone-800 border-2 border-stone-600 flex items-center justify-center relative shadow-sm">
                                <div class="absolute inset-0 pattern-diagonal-stripes opacity-30"></div>
                                <span class="text-stone-500 text-4xl font-bold z-10">?</span>
                            </div>
                          </div>
                          <div v-if="currentPlayer?.isSheriff" class="absolute top-1 right-1 text-xl drop-shadow-md z-20" title="Sheriff">⭐</div>
                       </div>
                    </div>

                    <!-- CENTER: Character -->
                    <div class="w-24 h-full relative flex flex-col bg-stone-800 overflow-visible group border-r border-amber-800/50 z-10 hover:z-50">
                       <div class="w-full h-full relative p-1 transition-transform duration-300 group-hover:scale-125 cursor-help origin-center shadow-lg"
                            @wheel="handleCardWheel($event, { imageSrc: getCharacterImage(currentPlayer.characterName), title: currentPlayer.characterName })">
                           <img
                             v-if="currentPlayer?.characterName"
                             :src="getCharacterImage(currentPlayer.characterName)"
                             :alt="currentPlayer.characterName"
                             class="w-full h-full object-contain opacity-80 group-hover:opacity-100 transition-opacity duration-300"
                           />
                           <div v-else class="w-full h-full flex items-center justify-center text-2xl">🤠</div>
                       </div>
                    </div>

                    <!-- RIGHT: Blue Cards -->
                    <div class="w-24 h-full border-l border-amber-800/50 bg-stone-800/50 relative flex-shrink-0 flex flex-col overflow-visible z-20 group/stack" data-blue-cards>
                        <div class="relative w-full h-full flex flex-col items-center justify-start">
                           <div class="flex flex-col items-center -space-y-24 group-hover/stack:-space-y-16 transition-all duration-300 w-full">
                              <div v-for="(card, index) in currentTableCards" :key="card.id || index"
                                   class="w-full h-32 flex-shrink-0 relative group/card flex justify-center"
                                   :style="{ zIndex: index }"
                                   @mouseenter="playSound('card_select')">
                                 <div class="h-full aspect-[2/3] rounded overflow-hidden shadow-md relative transition-all duration-300 ease-out group-hover/card:!z-[100] group-hover/card:scale-[1.5] group-hover/card:-translate-x-24 group-hover/card:translate-y-4 origin-center cursor-help delay-75 group-hover/card:delay-0"
                                      :class="{ 'opacity-0': animatingCardIds.has(card.id) }"
                                      :title="card.type"
                                      @wheel="handleCardWheel($event, { imageSrc: getCardImage(card.type), title: card.type, suit: card.suit, value: card.value })">
                                   <img :src="getCardImage(card.type)" class="w-full h-full object-contain" />
                                   
                                   <!-- Suit & Value Badge -->
                                   <div class="absolute bottom-0 left-1 flex items-center gap-0.5 z-10">
                                     <span class="text-xs font-semibold text-gray-800">
                                       {{ card.value }}
                                     </span>
                                     <span class="text-sm font-bold" :class="getSuitColor(card.suit)">
                                       {{ getSuitSymbol(card.suit) }}
                                     </span>
                                   </div>
                                 </div>
                               </div>
                           </div>
                        </div>
                    </div>
                  </div>
              </div>
            </div>

            <!-- Hand Cards (Fan Layout) -->
            <div class="flex-1 flex justify-center items-end mb-20 overflow-visible pb-12 perspective-1000">
              <div class="relative flex justify-center items-end w-full h-40">
                <div v-for="(card, index) in myHand"
                     :key="card.id"
                     class="absolute transition-all duration-300 transform-gpu origin-bottom-center"
                     :style="getFanStyle(index, myHand.length, selectedCard?.id === card.id)"
                     @mouseenter="handleCardHover(index)"
                     @mouseleave="hoveredCardIndex = -1"
                     @click="handleCardClick(card)"
                     @wheel="handleCardWheel($event, { imageSrc: getCardImage(card.type), title: card.type, suit: card.suit, value: card.value })">
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

    <!-- Inspection Overlay -->
    <div v-if="inspectedCard"
         class="fixed inset-0 bg-black/70 flex items-center justify-center z-[150]"
         @wheel="handleOverlayWheel"
         @click="inspectedCard = null">
      <div class="relative transform transition-all duration-300 scale-150 inline-block">
         <img :src="inspectedCard.imageSrc"
              :alt="inspectedCard.title"
              class="max-h-[80vh] max-w-[80vw] object-contain rounded-xl shadow-2xl drop-shadow-2xl" />
         
         <!-- Inspected Card Badge -->
         <div v-if="inspectedCard.suit && inspectedCard.value"
              class="absolute bottom-2 left-3 flex items-center gap-0.5 z-10">
            <span class="text-3xl font-semibold text-gray-800 drop-shadow-md">
              {{ inspectedCard.value }}
            </span>
            <span class="text-4xl font-bold drop-shadow-md" :class="getSuitColor(inspectedCard.suit)">
              {{ getSuitSymbol(inspectedCard.suit) }}
            </span>
         </div>
      </div>
    </div>

    <!-- Target Card Selection Overlay -->
    <div v-if="selectingTargetCardPlayerId"
         class="fixed inset-0 bg-black/80 flex items-center justify-center z-[160] p-8">
      <div class="bg-western-dark border-4 border-western-gold rounded-2xl p-8 max-w-4xl w-full flex flex-col items-center">
        <h2 class="font-western text-3xl text-western-gold mb-8">
          {{ selectedCard?.type === 'PANIC' ? 'Steal from' : 'Discard from' }} {{ targetPlayerForSelection?.name }}
        </h2>

        <div class="flex gap-12 items-start justify-center flex-wrap">
           <!-- Hand Option -->
           <div v-if="targetPlayerForSelection?.handSize > 0"
                class="flex flex-col items-center gap-4 cursor-pointer group"
                @click="handleTargetCardSelection(null)"> <!-- null means random/hand -->
              <div class="w-32 h-48 card-game card-back shadow-xl group-hover:scale-105 transition-transform border-4 border-transparent group-hover:border-western-gold/50 rounded-lg">
                 <!-- Badge showing count -->
                 <div class="absolute top-[-10px] right-[-10px] bg-red-600 text-white font-bold w-8 h-8 rounded-full flex items-center justify-center border-2 border-white shadow-md">
                   {{ targetPlayerForSelection?.handSize }}
                 </div>
              </div>
              <span class="text-western-sand text-xl font-bold group-hover:text-western-gold">Hand (Random)</span>
           </div>

           <!-- In Play Options -->
           <div v-if="targetPlayerForSelection?.cardsInPlay?.length > 0 || targetPlayerForSelection?.weapon"
                class="flex flex-col items-center gap-4">
              <div class="flex gap-4 flex-wrap justify-center">
                 <!-- Weapon -->
                 <div v-if="targetPlayerForSelection?.weapon"
                      class="relative cursor-pointer group"
                      @click="handleTargetCardSelection(targetPlayerForSelection.weapon.id)">
                     <CardComponent :card="{ ...targetPlayerForSelection.weapon, isWeapon: true }" class="w-32 h-48 transform group-hover:scale-105 transition-transform !shadow-xl hover:ring-4 ring-western-gold/50 rounded-lg" />
                     <span class="absolute -bottom-8 left-0 right-0 text-center text-western-sand font-bold group-hover:text-western-gold">Weapon</span>
                 </div>
                 
                 <!-- Other In Play Cards -->
                 <div v-for="card in targetPlayerForSelection.cardsInPlay" :key="card.id"
                      class="relative cursor-pointer group"
                      @click="handleTargetCardSelection(card.id)">
                     <CardComponent :card="card" class="w-32 h-48 transform group-hover:scale-105 transition-transform !shadow-xl hover:ring-4 ring-western-gold/50 rounded-lg" />
                 </div>
              </div>
              <span class="text-western-sand text-xl font-bold mt-2">Cards in Play</span>
           </div>
        </div>

        <button @click="cancelTargetCardSelection" class="btn-danger mt-12 px-8 py-2">
           Cancel
        </button>
      </div>
    </div>

    <!-- Game Over Overlay -->
    <div v-if="isGameOver"
         class="fixed inset-0 bg-black/80 flex items-center justify-center z-[200]">
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
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useGameStore } from '../stores/gameStore'
import { useAnimationQueue } from '../composables/useAnimationQueue'
import { useSoundManager } from '../composables/useSoundManager'
import CardComponent from '../components/CardComponent.vue'
import PlayerSeat from '../components/PlayerSeat.vue'
import HealthBar from '../components/HealthBar.vue'

const route = useRoute()
const router = useRouter()
const gameStore = useGameStore()
const { animatingCardIds } = useAnimationQueue()
const { playSound } = useSoundManager()

const selectedCard = ref(null)
const selectedTarget = ref(null)
const hoveredCardIndex = ref(-1)
const inspectedCard = ref(null)
const selectingTargetCardPlayerId = ref(null)
const flashingIndices = ref(new Set())
const flashingGreenIndices = ref(new Set())

const roomId = computed(() => route.params.roomId)
const gameState = computed(() => gameStore.gameState)
const currentPlayer = computed(() => gameStore.currentPlayer)
const myHand = computed(() => currentPlayer.value?.hand || [])
const otherPlayers = computed(() => gameStore.otherPlayers)
const isMyTurn = computed(() => gameStore.isMyTurn)
const phase = computed(() => gameStore.phase)
const needsToRespond = computed(() => gameStore.needsToRespond)
const isProcessing = computed(() => gameStore.isProcessing)
const drawPileSize = computed(() => gameState.value?.drawPileSize || 0)
const topDiscardCard = computed(() => gameState.value?.topDiscardCard)
const isGameOver = computed(() => gameState.value?.phase === 'GAME_OVER')

const targetPlayerForSelection = computed(() => {
  if (!selectingTargetCardPlayerId.value) return null
  return gameState.value?.players?.find(p => p.id === selectingTargetCardPlayerId.value)
})

const currentTableCards = computed(() => {
  const player = currentPlayer.value
  if (!player) return []
  const cards = []
  if (player.weapon) {
    cards.push({ ...player.weapon, isWeapon: true })
  }
  if (player.cardsInPlay) {
    cards.push(...player.cardsInPlay)
  }
  return cards
})

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
    'GENERAL_STORE_PHASE': 'General Store',
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

const usedAbilities = computed(() => gameState.value?.usedReactionAbilities || [])

const canUseBarrel = computed(() => {
    // Check if player has Barrel in play
    const hasBarrel = currentPlayer.value?.cardsInPlay?.some(c => c.type === 'BARREL')
    if (!hasBarrel) return false
    
    // Check if not used
    const barrelCard = currentPlayer.value?.cardsInPlay?.find(c => c.type === 'BARREL')
    if (usedAbilities.value.includes(barrelCard?.id)) return false
    
    // Check if valid action type (only BANG and GATLING)
    const actionType = gameState.value?.pendingActionType
    if (actionType !== 'BANG' && actionType !== 'GATLING') return false
    
    return true
})

const canUseJourdonnais = computed(() => {
    if (currentPlayer.value?.characterName !== 'Jourdonnais') return false
    if (usedAbilities.value.includes('JOURDONNAIS')) return false
    
    const actionType = gameState.value?.pendingActionType
    if (actionType !== 'BANG' && actionType !== 'GATLING') return false
    
    return true
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

function handleCardWheel(event, cardData) {
  if (!cardData.imageSrc) return
  if (event.deltaY < 0) { // Scroll UP
    event.preventDefault()
    inspectedCard.value = cardData
  }
}

function handleOverlayWheel(event) {
  if (event.deltaY > 0) { // Scroll DOWN
    event.preventDefault()
    inspectedCard.value = null
  }
}

function handleInspect(cardData) {
  inspectedCard.value = cardData
}

function handleGameMessage(event) {
  gameStore.handleGameMessage(event.detail)
}

function handleGameEvent(event) {
  gameStore.handleGameEvent(event.detail)
}

watch(() => currentPlayer.value?.health, (newVal, oldVal) => {
  if (oldVal !== undefined && newVal !== undefined) {
    if (oldVal > newVal) {
      for (let i = newVal + 1; i <= oldVal; i++) {
        flashingIndices.value.add(i)
        setTimeout(() => {
          flashingIndices.value.delete(i)
        }, 3500)
      }
    } else if (newVal > oldVal) {
      for (let i = oldVal + 1; i <= newVal; i++) {
        flashingGreenIndices.value.add(i)
        setTimeout(() => {
          flashingGreenIndices.value.delete(i)
        }, 3500)
      }
    }
  }
})

function getPlayerPosition(index, total) {
  // Elliptical distribution for up to 7 players (6 opponents)
  // We want to distribute them from Left-Bottom (160deg) to Right-Bottom (380deg)
  // going clockwise through Top (270deg).
  
  const xRadius = 35; // Reduced to keep inside screen
  const yRadius = 50; // Consistent
  const centerX = 50;
  const centerY = 60; // Slightly adjusted

  const startAngle = 180;
  const endAngle = 360;
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

function handleCardHover(index) {
  if (hoveredCardIndex.value !== index) {
    hoveredCardIndex.value = index
    playSound('card_select')
  }
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
  
  const type = selectedCard.value.type
  if (type === 'PANIC' || type === 'CAT_BALOU') {
    const hasHand = (player.handSize || 0) > 0
    const hasInPlay = (player.cardsInPlay?.length || 0) > 0 || !!player.weapon
    
    // If player has cards on table OR (has cards in hand AND on table), we must ask user to choose
    // If player has ONLY cards in hand, we can default to random hand (or show UI for consistency, but consistency is better)
    // Actually, user requirement says "choose from hand OR board". So always show UI if there's a choice.
    // If only hand, random is the only option, but maybe show UI to confirm "Steal from Hand"?
    // Let's show UI if there are any cards at all, to be safe and explicit.
    
    if (hasHand || hasInPlay) {
       selectingTargetCardPlayerId.value = player.id
       return
    }
  }

  gameStore.playCard(selectedCard.value.id, player.id)
  selectedCard.value = null
}

function handleTargetCardSelection(targetCardId) {
  if (!selectedCard.value || !selectingTargetCardPlayerId.value) return
  
  gameStore.playCard(selectedCard.value.id, selectingTargetCardPlayerId.value, targetCardId)
  
  // Cleanup
  selectedCard.value = null
  selectingTargetCardPlayerId.value = null
}

function cancelTargetCardSelection() {
  selectingTargetCardPlayerId.value = null
  selectedCard.value = null
}

function playSelectedCard(targetId = null) {
  if (!selectedCard.value) return
  gameStore.playCard(selectedCard.value.id, targetId)
  selectedCard.value = null
}

function cancelSelection() {
  selectedCard.value = null
  selectingTargetCardPlayerId.value = null
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

function useBarrel() {
    const barrelCard = currentPlayer.value?.cardsInPlay?.find(c => c.type === 'BARREL')
    if (barrelCard) {
        gameStore.useAbility(barrelCard.id)
    }
}

function useJourdonnais() {
    gameStore.useAbility('JOURDONNAIS')
}

function handleGeneralStorePick(card) {
    if (!needsToRespond.value) return;
    gameStore.pickGeneralStoreCard(card.id);
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

function getSuitSymbol(suit) {
  const suits = {
    'HEARTS': '♥',
    'DIAMONDS': '♦',
    'CLUBS': '♣',
    'SPADES': '♠'
  }
  return suits[suit] || '?'
}

function getSuitColor(suit) {
  if (suit === 'HEARTS' || suit === 'DIAMONDS') {
    return 'text-red-500'
  }
  return 'text-gray-800'
}

function getCardImage(type) {
  const imageMap = {
    'BANG': 'bang.png',
    'MISSED': 'missed.png',
    'BEER': 'beer.png',
    'SALOON': 'saloon.png',
    'STAGECOACH': 'stagecoach.png',
    'WELLS_FARGO': 'wellsfargo.png',
    'PANIC': 'panic.png',
    'CAT_BALOU': 'catbalou.png',
    'DUEL': 'duel.png',
    'GATLING': 'gatling.png',
    'INDIANS': 'indians.png',
    'GENERAL_STORE': 'generalstore.png',
    'BARREL': 'barrel.png',
    'MUSTANG': 'mustang.png',
    'SCOPE': 'scope.png',
    'JAIL': 'jail.png',
    'DYNAMITE': 'dinamite.png',
    'VOLCANIC': 'volcanic.png',
    'SCHOFIELD': 'schofield.png',
    'REMINGTON': 'remington.png',
    'REV_CARABINE': 'carabine.png',
    'WINCHESTER': 'winchester.png'
  }
  const filename = imageMap[type] || 'bang.png'
  return `/images/cards/${filename}`
}

function getWeaponRange(type) {
  const ranges = {
    'VOLCANIC': 1,
    'SCHOFIELD': 2,
    'REMINGTON': 3,
    'REV_CARABINE': 4,
    'WINCHESTER': 5
  }
  return ranges[type] || 1
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

<style scoped>
.font-western {
  font-family: 'Rye', serif;
}

.animate-flash-bullet {
  animation: flashBullet 1s infinite;
}

.animate-flash-green-bullet {
  animation: flashGreenBullet 1s infinite;
}

@keyframes flashBullet {
  0%, 100% {
    filter: sepia(1) saturate(10) hue-rotate(-50deg) drop-shadow(0 0 5px red);
    opacity: 1;
  }
  50% {
    filter: grayscale(1);
    opacity: 0.3;
  }
}

@keyframes flashGreenBullet {
  0%, 100% {
    filter: sepia(1) saturate(5) hue-rotate(60deg) drop-shadow(0 0 5px green);
    opacity: 1;
  }
  50% {
    filter: grayscale(1);
    opacity: 0.3;
  }
}
</style>
