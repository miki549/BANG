<template>
  <div
    class="card-game relative cursor-pointer select-none"
    :class="{
      'ring-2 ring-western-gold scale-110 -translate-y-4': selected,
      'opacity-50 cursor-not-allowed': disabled,
      'hover:-translate-y-5 hover:scale-110 hover:z-50': !disabled && playable,
      'grayscale': !playable && !disabled
    }"
    @click="!disabled && $emit('click', card)"
  >
    <!-- Card Face -->
    <div class="absolute inset-0 rounded-lg overflow-hidden flex flex-col">
      <!-- Card Header -->
      <div class="bg-western-brown/80 px-2 py-1 text-center border-b border-western-dark">
        <span class="text-xs font-bold text-western-sand truncate">
          {{ getCardName() }}
        </span>
      </div>

      <!-- Card Body -->
      <div class="flex-1 flex items-center justify-center p-2"
           :class="getCardBackgroundClass()">
        <!-- Card Symbol/Icon -->
        <div class="text-center">
          <div class="text-3xl mb-1">{{ getCardIcon() }}</div>
          <div v-if="card.type?.includes('SCHOFIELD') || card.type?.includes('REMINGTON') || 
                      card.type?.includes('WINCHESTER') || card.type?.includes('REV_CARABINE') ||
                      card.type?.includes('VOLCANIC')"
               class="text-xs text-western-dark font-bold">
            Range: {{ getWeaponRange() }}
          </div>
        </div>
      </div>

      <!-- Card Footer - Suit & Value -->
      <div class="bg-western-dark/60 px-2 py-1 flex justify-between items-center">
        <span class="text-sm font-bold" :class="getSuitColor()">
          {{ getSuitSymbol() }}
        </span>
        <span class="text-xs text-western-sand">
          {{ card.value }}
        </span>
      </div>
    </div>

    <!-- Selection Glow -->
    <div v-if="selected" 
         class="absolute inset-0 rounded-lg pointer-events-none"
         style="box-shadow: 0 0 20px 5px rgba(218, 165, 32, 0.6);">
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  card: {
    type: Object,
    required: true
  },
  selected: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  playable: {
    type: Boolean,
    default: true
  }
})

defineEmits(['click'])

function getCardName() {
  const names = {
    'BANG': 'BANG!',
    'MISSED': 'Missed!',
    'BEER': 'Beer',
    'SALOON': 'Saloon',
    'STAGECOACH': 'Stagecoach',
    'WELLS_FARGO': 'Wells Fargo',
    'PANIC': 'Panic!',
    'CAT_BALOU': 'Cat Balou',
    'DUEL': 'Duel',
    'GATLING': 'Gatling',
    'INDIANS': 'Indians!',
    'GENERAL_STORE': 'General Store',
    'BARREL': 'Barrel',
    'MUSTANG': 'Mustang',
    'SCOPE': 'Scope',
    'JAIL': 'Jail',
    'DYNAMITE': 'Dynamite',
    'VOLCANIC': 'Volcanic',
    'SCHOFIELD': 'Schofield',
    'REMINGTON': 'Remington',
    'REV_CARABINE': 'Carabine',
    'WINCHESTER': 'Winchester'
  }
  return names[props.card.type] || props.card.type
}

function getCardIcon() {
  const icons = {
    'BANG': '💥',
    'MISSED': '🎯',
    'BEER': '🍺',
    'SALOON': '🏠',
    'STAGECOACH': '🐎',
    'WELLS_FARGO': '📦',
    'PANIC': '😱',
    'CAT_BALOU': '🐱',
    'DUEL': '⚔️',
    'GATLING': '🔫',
    'INDIANS': '🪶',
    'GENERAL_STORE': '🏪',
    'BARREL': '🛢️',
    'MUSTANG': '🐴',
    'SCOPE': '🔭',
    'JAIL': '⛓️',
    'DYNAMITE': '🧨',
    'VOLCANIC': '🌋',
    'SCHOFIELD': '🔫',
    'REMINGTON': '🔫',
    'REV_CARABINE': '🔫',
    'WINCHESTER': '🔫'
  }
  return icons[props.card.type] || '🃏'
}

function getCardBackgroundClass() {
  if (props.card.color === 'BLUE') {
    return 'bg-gradient-to-b from-blue-200 to-blue-300'
  }
  return 'bg-gradient-to-b from-amber-100 to-amber-200'
}

function getSuitSymbol() {
  const suits = {
    'HEARTS': '♥',
    'DIAMONDS': '♦',
    'CLUBS': '♣',
    'SPADES': '♠'
  }
  return suits[props.card.suit] || '?'
}

function getSuitColor() {
  if (props.card.suit === 'HEARTS' || props.card.suit === 'DIAMONDS') {
    return 'text-red-500'
  }
  return 'text-gray-800'
}

function getWeaponRange() {
  const ranges = {
    'VOLCANIC': 1,
    'SCHOFIELD': 2,
    'REMINGTON': 3,
    'REV_CARABINE': 4,
    'WINCHESTER': 5
  }
  return ranges[props.card.type] || 1
}
</script>
