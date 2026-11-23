<script setup lang="ts">
import { computed } from 'vue'
import { safeColor } from '../format'

const props = defineProps<{
  code: string | null
  label: string | null
  color: string | null
}>()

const dot = computed(() => safeColor(props.color))
</script>

<template>
  <span v-if="code" class="group-badge" :title="label ?? code">
    <span class="group-dot" :style="dot ? { backgroundColor: dot } : undefined" aria-hidden="true"></span>
    {{ code }}
  </span>
</template>
