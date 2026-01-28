<template>
  <div class="min-h-screen">
    <router-view />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useGameStore } from './stores/gameStore'

const gameStore = useGameStore()

onMounted(() => {
  // Initialize sound manager
})
</script>
