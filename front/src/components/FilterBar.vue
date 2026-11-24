<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import type { GroupInfo } from '../types'

const props = defineProps<{
  q: string
  group: number | null
  groups: GroupInfo[]
}>()
const emit = defineEmits<{
  'update:q': [value: string]
  'update:group': [value: number | null]
}>()

// Saisie locale + délai : on n'interroge l'API qu'une fois la frappe terminée.
const draft = ref(props.q)
let timer: ReturnType<typeof setTimeout> | undefined

watch(() => props.q, (value) => (draft.value = value))
watch(draft, (value) => {
  clearTimeout(timer)
  timer = setTimeout(() => {
    if (value !== props.q) emit('update:q', value)
  }, 300)
})
onBeforeUnmount(() => clearTimeout(timer))

function onGroup(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('update:group', value === '' ? null : Number(value))
}
</script>

<template>
  <div class="filters">
    <label class="field field-search">
      <span class="sr-only">Rechercher un parlementaire</span>
      <input v-model="draft" type="search" placeholder="Rechercher par nom…" maxlength="100"
             autocomplete="off" />
    </label>
    <label v-if="groups.length" class="field">
      <span class="sr-only">Groupe politique</span>
      <select :value="group ?? ''" @change="onGroup">
        <option value="">Tous les groupes</option>
        <option v-for="g in groups" :key="g.id" :value="g.id">{{ g.code }} ({{ g.memberCount }})</option>
      </select>
    </label>
  </div>
</template>
