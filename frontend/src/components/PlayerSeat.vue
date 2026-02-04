<template>
  <div
    class="player-board absolute select-none transition-all duration-300 hover:z-[60]"
    :class="{
      'z-30': isCurrentTurn || isTargetable,
      'z-10': !isCurrentTurn && !isTargetable,
      'current-turn': isCurrentTurn,
      'pending-action': isPendingAction,
      'scale-105': isCurrentTurn,
      'grayscale opacity-70': !player.alive
    }"
    :style="positionStyle"
    :data-player-id="player.id"
    @click="isTargetable && $emit('select', player)"
  >
    <!-- Target Overlay -->
    <div v-if="isTargetable"
         class="absolute -inset-2 border-4 border-red-500 rounded-lg animate-pulse z-50 pointer-events-none shadow-[0_0_20px_rgba(239,68,68,0.6)]">
    </div>

    <!-- Main Board Area -->
    <div class="relative bg-stone-900/95 border-2 border-amber-800 rounded-lg shadow-xl overflow-visible w-auto flex flex-col">
      
      <!-- Header: Name & Stats -->
      <div class="flex justify-between items-center px-2 py-1 bg-black/40 border-b border-amber-800/30">
        <!-- Name (Left) -->
        <div class="text-left font-bold text-western-sand text-shadow-sm truncate text-sm">
          {{ player.name }}
        </div>
        
        <!-- Stats (Right) -->
        <div class="flex items-center gap-2">
           <!-- Health -->
           <div class="flex flex-wrap justify-center gap-0.5">
              <div v-for="i in player.maxHealth" :key="i"
                   class="w-6 h-6 rounded-full">
                   <img src="/images/common/hp.png" alt="HP"
                        class="w-full h-full object-contain rotate-45 drop-shadow-sm transition-all duration-500"
                        :class="{
                          'opacity-30 grayscale': i > player.health && !flashingIndices.has(i) && !flashingGreenIndices.has(i),
                          'animate-flash-bullet': flashingIndices.has(i),
                          'animate-flash-green-bullet': flashingGreenIndices.has(i)
                        }" />
              </div>
           </div>
           
           <!-- Hand Size -->
           <div class="flex items-center gap-1 text-western-sand/90 text-sm font-bold">
              <img src="/images/common/deck.png" alt="Cards" class="w-6 h-6 object-contain" />
              <span class="font-mono text-base">{{ player.handSize }}</span>
           </div>
        </div>
      </div>

      <!-- Slots Row -->
      <div class="flex h-32">
        <!-- LEFT: Role Card -->
        <div class="w-24 h-full border-r border-amber-800/50 bg-stone-800 relative flex-shrink-0 z-10 hover:z-50">
           <div class="w-full h-full overflow-visible relative transition-transform duration-300 hover:scale-125 cursor-help origin-center shadow-lg"
                @wheel="handleWheel($event, { imageSrc: getRoleImage(player.role), title: player.role })">
              <img
                 v-if="player.role"
                 :src="getRoleImage(player.role)"
                 :alt="player.role"
                 class="w-full h-full object-contain p-1"
              />
              <!-- Hidden Role Card -->
              <div v-else class="w-full h-full flex items-center justify-center p-1">
                <div class="h-full aspect-[2/3] rounded bg-stone-800 border-2 border-stone-600 flex items-center justify-center relative shadow-sm">
                    <div class="absolute inset-0 pattern-diagonal-stripes opacity-30"></div>
                    <span class="text-stone-500 text-4xl font-bold z-10">?</span>
                </div>
              </div>
              
              <!-- Sheriff Badge Overlay -->
               <div v-if="player.isSheriff" class="absolute top-1 right-1 text-xl drop-shadow-md z-20" title="Sheriff">⭐</div>
           </div>
        </div>

        <!-- CENTER: Character (Shrunk) -->
        <div class="w-24 h-full relative flex flex-col bg-stone-800 overflow-visible group border-r border-amber-800/50 z-10 hover:z-50">
           <!-- Character Image -->
           <div class="w-full h-full relative p-1 transition-transform duration-300 group-hover:scale-125 cursor-help origin-center shadow-lg"
                @wheel="handleWheel($event, { imageSrc: getCharacterImage(player.characterName), title: player.characterName })">
               <img
                 v-if="player.characterName"
                 :src="getCharacterImage(player.characterName)"
                 :alt="player.characterName"
                 class="w-full h-full object-contain opacity-80 group-hover:opacity-100 transition-opacity duration-300"
               />
               <div v-else class="w-full h-full flex items-center justify-center text-2xl">🤠</div>
           </div>
        </div>

        <!-- RIGHT: Blue Cards (Vertical Stack) -->
        <div class="w-24 h-full border-l border-amber-800/50 bg-stone-800/50 relative flex-shrink-0 flex flex-col overflow-visible z-20 group/stack">
            <div class="relative w-full h-full flex flex-col items-center justify-start">
               <div class="flex flex-col items-center -space-y-24 group-hover/stack:-space-y-16 transition-all duration-300 w-full">
                  <div v-for="(card, index) in tableCards" :key="card.id || index"
                       class="w-full h-32 flex-shrink-0 relative group/card flex justify-center"
                       :style="{ zIndex: index }">
                       
                     <div class="h-full aspect-[2/3] rounded overflow-hidden shadow-md relative transition-all duration-300 ease-out group-hover/card:!z-[100] group-hover/card:scale-[1.5] group-hover/card:-translate-x-24 group-hover/card:translate-y-4 origin-center cursor-help delay-75 group-hover/card:delay-0"
                          :class="{ 'opacity-0': animatingCardIds.has(card.id) }"
                          :title="card.type"
                          @wheel="handleWheel($event, { imageSrc: getCardImage(card.type), title: card.type, suit: card.suit, value: card.value })">
                       
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

                       <!-- Tooltip -->
                       <div class="absolute right-full mr-2 top-1/2 -translate-y-1/2 hidden group-hover/card:block bg-black/90 text-white text-[0.6rem] px-2 py-1 rounded whitespace-nowrap z-[110] border border-western-gold/50 shadow-lg pointer-events-none">
                           {{ card.type }}
                       </div>
                     </div>
                  </div>
               </div>
            </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useAnimationQueue } from '../composables/useAnimationQueue'

const { animatingCardIds } = useAnimationQueue()

const props = defineProps({
  player: {
    type: Object,
    required: true
  },
  position: {
    type: Object,
    required: true
  },
  isCurrentTurn: {
    type: Boolean,
    default: false
  },
  isTargetable: {
    type: Boolean,
    default: false
  },
  isPendingAction: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select', 'inspect'])

const flashingIndices = ref(new Set())
const flashingGreenIndices = ref(new Set())

watch(() => props.player.health, (newVal, oldVal) => {
  if (oldVal > newVal) {
    for (let i = newVal + 1; i <= oldVal; i++) {
      flashingIndices.value.add(i)
      setTimeout(() => {
        flashingIndices.value.delete(i)
      }, 3000)
    }
  } else if (newVal > oldVal) {
    for (let i = oldVal + 1; i <= newVal; i++) {
      flashingGreenIndices.value.add(i)
      setTimeout(() => {
        flashingGreenIndices.value.delete(i)
      }, 3000)
    }
  }
})

const positionStyle = computed(() => ({
  left: `${props.position.x}%`,
  top: `${props.position.y}%`,
  transform: 'translate(-50%, -50%)'
}))

const tableCards = computed(() => {
  const cards = []
  if (props.player.weapon) {
    cards.push({ ...props.player.weapon, isWeapon: true })
  }
  if (props.player.cardsInPlay) {
    cards.push(...props.player.cardsInPlay)
  }
  return cards
})

function getCardMiniIcon(type) {
  const icons = {
    'BARREL': '🛢️',
    'MUSTANG': '🐴',
    'SCOPE': '🔭',
    'JAIL': '⛓️',
    'DYNAMITE': '🧨',
    'VOLCANIC': '🔫',
    'SCHOFIELD': '🔫',
    'REMINGTON': '🔫',
    'REV_CARABINE': '🔫',
    'WINCHESTER': '🔫'
  }
  return icons[type] || '📄'
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

function getRoleBgClass(role) {
  // Not used anymore for background, but keeping function if needed elsewhere
  const classes = {
    'SHERIFF': 'bg-yellow-900/80 text-yellow-200 border-yellow-600',
    'DEPUTY': 'bg-blue-900/80 text-blue-200 border-blue-600',
    'OUTLAW': 'bg-red-900/80 text-red-200 border-red-600',
    'RENEGADE': 'bg-purple-900/80 text-purple-200 border-purple-600'
  }
  return classes[role] || 'bg-stone-700 text-stone-300'
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

function handleWheel(event, data) {
  if (!data.imageSrc) return
  if (event.deltaY < 0) { // Scroll UP
    event.preventDefault()
    emit('inspect', data)
  }
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
</script>

<style scoped>
.font-western {
  font-family: 'Rye', serif; /* Assuming this font is loaded globally or available */
}
.pattern-diagonal-stripes {
  background-image: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 5px,
    rgba(255, 255, 255, 0.05) 5px,
    rgba(255, 255, 255, 0.05) 10px
  );
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
