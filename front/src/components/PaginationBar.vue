<script setup lang="ts">
defineProps<{
  page: number
  totalPages: number
  totalItems: number
}>()
const emit = defineEmits<{ change: [page: number] }>()
</script>

<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="Pagination">
    <button type="button" class="btn" :disabled="page <= 0" @click="emit('change', page - 1)">Précédent</button>
    <span class="pagination-status">Page {{ page + 1 }} sur {{ totalPages }} · {{ totalItems }} résultats</span>
    <button type="button" class="btn" :disabled="page >= totalPages - 1" @click="emit('change', page + 1)">
      Suivant
    </button>
  </nav>
</template>
