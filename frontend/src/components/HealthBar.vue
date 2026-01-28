<template>
  <div class="health-bar" :class="{ 'gap-0.5': size === 'small', 'gap-1': size === 'normal' }">
    <div
      v-for="i in max"
      :key="i"
      class="health-bullet"
      :class="[
        i <= current ? 'full' : 'empty',
        sizeClasses
      ]"
    >
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  current: {
    type: Number,
    required: true
  },
  max: {
    type: Number,
    required: true
  },
  size: {
    type: String,
    default: 'normal',
    validator: (value) => ['small', 'normal', 'large'].includes(value)
  }
})

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'small':
      return 'w-3 h-3'
    case 'large':
      return 'w-6 h-6'
    default:
      return 'w-4 h-4'
  }
})
</script>
