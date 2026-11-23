<script setup lang="ts">
import { ref, watch } from 'vue'
import { initials } from '../format'

const props = defineProps<{
  photoUrl: string | null
  firstName: string
  lastName: string
}>()

const failed = ref(false)
watch(() => props.photoUrl, () => (failed.value = false))
</script>

<template>
  <div class="avatar">
    <img
      v-if="photoUrl && !failed"
      :src="photoUrl"
      alt=""
      loading="lazy"
      referrerpolicy="no-referrer"
      @error="failed = true"
    />
    <span v-else class="avatar-initials" aria-hidden="true">{{ initials({ firstName, lastName }) }}</span>
  </div>
</template>
