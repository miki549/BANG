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
    <div class="absolute inset-0 rounded-lg overflow-hidden">
      <!-- Card Image (full card) -->
      <img 
        :src="getCardImage()" 
        :alt="getCardName()"
        class="w-full h-full object-cover"
        @error="onImageError"
      />
      
      <!-- Suit & Value Badge (bottom left) -->
      <div class="absolute bottom-0 left-1 flex items-center gap-0.5">
        <span class="text-xs font-semibold text-gray-800">
          {{ card.value }}
        </span>
        <span class="text-sm font-bold" :class="getSuitColor()">
          {{ getSuitSymbol() }}
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

function getCardImage() {
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
  const filename = imageMap[props.card.type] || 'bang.png'
  return `/images/cards/${filename}`
}

function onImageError(event) {
  // Fallback to a placeholder if image fails to load
  console.warn('Failed to load card image:', props.card.type)
  event.target.style.display = 'none'
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
