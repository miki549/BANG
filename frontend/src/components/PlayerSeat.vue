<template>
  <div
    class="player-seat"
    :class="{
      'current-turn': isCurrentTurn,
      'ring-2 ring-red-500 cursor-pointer hover:ring-red-400': isTargetable,
      'opacity-40 grayscale': !player.alive,
      'ring-2 ring-yellow-500 animate-pulse': isPendingAction
    }"
    :style="positionStyle"
    :data-player-id="player.id"
    @click="isTargetable && $emit('select', player)"
  >
    <!-- Character Avatar -->
    <div class="relative">
      <div class="w-16 h-16 rounded-full overflow-hidden border-2"
           :class="isCurrentTurn ? 'border-western-gold' : 'border-western-sand/30'">
        <div class="w-full h-full bg-western-leather flex items-center justify-center">
          <span class="text-2xl font-bold text-western-sand">
            {{ player.name?.charAt(0).toUpperCase() }}
          </span>
        </div>
      </div>
      
      <!-- Sheriff Badge -->
      <div v-if="player.isSheriff" 
           class="absolute -top-1 -right-1 w-6 h-6 bg-western-gold rounded-full flex items-center justify-center
                  border-2 border-western-dark text-xs">
        ⭐
      </div>

      <!-- Dead Marker -->
      <div v-if="!player.alive"
           class="absolute inset-0 flex items-center justify-center">
        <span class="text-4xl">💀</span>
      </div>
    </div>

    <!-- Player Name -->
    <div class="text-center">
      <div class="font-semibold text-western-sand text-sm truncate max-w-24">
        {{ player.name }}
      </div>
      <div class="text-xs text-western-gold/80">
        {{ player.characterName }}
      </div>
    </div>

    <!-- Health Bar -->
    <HealthBar :current="player.health" :max="player.maxHealth" size="small" />

    <!-- Cards in Hand Indicator -->
    <div class="flex items-center gap-1 text-xs text-western-sand/60">
      <span>🃏</span>
      <span>{{ player.handSize }}</span>
    </div>

    <!-- Cards in Play -->
    <div v-if="player.cardsInPlay?.length > 0" class="flex gap-1 mt-1">
      <div v-for="card in player.cardsInPlay" :key="card.id"
           class="w-6 h-8 bg-blue-400/30 rounded border border-blue-400/50 flex items-center justify-center text-xs"
           :title="card.type">
        {{ getCardMiniIcon(card.type) }}
      </div>
    </div>

    <!-- Weapon -->
    <div v-if="player.weapon" class="mt-1">
      <div class="text-xs bg-western-dark/50 px-2 py-0.5 rounded text-western-gold">
        🔫 {{ player.weapon.type?.replace('_', ' ') }}
      </div>
    </div>

    <!-- Role (only shown if visible) -->
    <div v-if="player.role" class="mt-1">
      <span class="text-xs px-2 py-0.5 rounded"
            :class="getRoleClass(player.role)">
        {{ player.role }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import HealthBar from './HealthBar.vue'

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

defineEmits(['select'])

const positionStyle = computed(() => ({
  left: `${props.position.x}%`,
  top: `${props.position.y}%`,
  transform: 'translate(-50%, -50%)'
}))

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

function getRoleClass(role) {
  const classes = {
    'SHERIFF': 'bg-yellow-500/30 text-yellow-300',
    'DEPUTY': 'bg-blue-500/30 text-blue-300',
    'OUTLAW': 'bg-red-500/30 text-red-300',
    'RENEGADE': 'bg-purple-500/30 text-purple-300'
  }
  return classes[role] || 'bg-gray-500/30 text-gray-300'
}
</script>
