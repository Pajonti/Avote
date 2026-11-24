<script setup lang="ts">
import { computed } from 'vue'
import { CHAMBER_SHORT } from '../format'
import type { Chamber, ChamberStats } from '../types'

const props = defineProps<{
  modelValue: Chamber | null
  stats: ChamberStats[]
}>()
const emit = defineEmits<{ 'update:modelValue': [value: Chamber | null] }>()

const total = computed(() => props.stats.reduce((sum, s) => sum + s.memberCount, 0))
</script>

<template>
  <div class="tabs" role="group" aria-label="Chambre">
    <button type="button" class="tab" :aria-pressed="modelValue === null" @click="emit('update:modelValue', null)">
      Tous <span class="tab-count">{{ total }}</span>
    </button>
    <button v-for="s in stats" :key="s.chamber" type="button" class="tab" :aria-pressed="modelValue === s.chamber"
            @click="emit('update:modelValue', s.chamber)">
      {{ CHAMBER_SHORT[s.chamber] }} <span class="tab-count">{{ s.memberCount }}</span>
    </button>
  </div>
</template>
