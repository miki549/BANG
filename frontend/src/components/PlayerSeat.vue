<template>
  <div
    class="player-board absolute select-none transition-all duration-300 hover:z-[60]"
    :class="{
      'z-30': isCurrentTurn || isTargetable,
      'z-10': !isCurrentTurn && !isTargetable,
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

    <!-- Turn Indicator -->
    <div v-if="isCurrentTurn" 
         class="absolute -inset-2 border-4 border-western-gold rounded-lg z-0 shadow-[0_0_20px_rgba(218,165,32,0.4)]">
    </div>

    <!-- Pending Action Indicator -->
    <div v-if="isPendingAction" 
         class="absolute -inset-2 border-4 border-yellow-500 rounded-lg animate-pulse z-50 pointer-events-none">
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
           <div class="flex flex-wrap justify-center gap-1">
              <div v-for="i in player.maxHealth" :key="i"
                   class="w-2 h-2 rounded-full border border-stone-900 shadow-sm transition-colors duration-300"
                   :class="i <= player.health ? 'bg-red-600' : 'bg-stone-700'">
              </div>
           </div>
           
           <!-- Hand Size -->
           <div class="flex items-center gap-1 text-western-sand/90 text-[0.65rem] font-bold">
              <span>🃏</span>
              <span class="font-mono">{{ player.handSize }}</span>
           </div>
        </div>
      </div>

      <!-- Slots Row -->
      <div class="flex h-32">
        <!-- LEFT: Role Card -->
        <div class="w-24 h-full border-r border-amber-800/50 bg-stone-800 relative flex-shrink-0 z-10 hover:z-50">
           <div class="w-full h-full overflow-visible relative transition-transform duration-300 hover:scale-125 cursor-help origin-center shadow-lg"
                :class="player.role ? '' : 'bg-stone-700 pattern-diagonal-stripes'"
                @wheel="handleWheel($event, { imageSrc: getRoleImage(player.role), title: player.role })">
              <img
                 v-if="player.role"
                 :src="getRoleImage(player.role)"
                 :alt="player.role"
                 class="w-full h-full object-contain p-1"
              />
              <div v-else class="w-full h-full flex items-center justify-center text-stone-500 text-xs">?</div>
              
              <!-- Sheriff Badge Overlay -->
               <div v-if="player.isSheriff" class="absolute top-1 right-1 text-xl drop-shadow-md" title="Sheriff">⭐</div>
           </div>
        </div>

        <!-- CENTER: Character (Shrunk) -->
        <div class="w-24 h-full relative flex flex-col bg-stone-800 overflow-visible group border-r border-amber-800/50 z-10 hover:z-50">
           <!-- Character Image -->
           <div class="w-full h-full relative p-1 transition-transform duration-300 group-hover:scale-125 cursor-help origin-center shadow-lg bg-stone-800"
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
                       class="w-full h-32 flex-shrink-0 relative group/card"
                       :style="{ zIndex: index }">
                       
                     <div class="w-full h-full rounded overflow-hidden shadow-md relative transition-all duration-300 ease-out group-hover/card:!z-[100] group-hover/card:scale-[1.5] group-hover/card:-translate-x-24 group-hover/card:translate-y-4 origin-center cursor-help delay-75 group-hover/card:delay-0"
                          :title="card.type"
                          @wheel="handleWheel($event, { imageSrc: getCardImage(card.type), title: card.type })">
                       
                       <img :src="getCardImage(card.type)" class="w-full h-full object-contain" />
                       
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
import { computed } from 'vue'

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
  if (event.deltaY < 0) { // Scroll UP
    event.preventDefault()
    emit('inspect', data)
  }
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
</style>
