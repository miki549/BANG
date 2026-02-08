<template>
  <div class="min-h-screen">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useGameStore } from './stores/gameStore'
import { useSoundManager } from './composables/useSoundManager'

const gameStore = useGameStore()
const { initSounds } = useSoundManager()

// Initialize sounds immediately to ensure they are available for child components
initSounds()

onMounted(() => {
})
</script>
