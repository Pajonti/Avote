<script setup lang="ts">
import { CHAMBER_LABELS, fullName } from '../format'
import type { ParliamentarianSummary } from '../types'
import Avatar from './Avatar.vue'
import GroupBadge from './GroupBadge.vue'

defineProps<{
  parliamentarian: ParliamentarianSummary
  to: object
  showChamber: boolean
}>()
</script>

<template>
  <RouterLink class="card" :to="to">
    <Avatar :photo-url="parliamentarian.photoUrl" :first-name="parliamentarian.firstName"
            :last-name="parliamentarian.lastName" />
    <div class="card-body">
      <h3 class="card-name">{{ fullName(parliamentarian) }}</h3>
      <GroupBadge :code="parliamentarian.groupCode" :label="parliamentarian.groupLabel"
                  :color="parliamentarian.groupColor" />
      <p v-if="parliamentarian.constituency" class="card-meta">{{ parliamentarian.constituency }}</p>
      <p v-if="showChamber" class="card-chamber">{{ CHAMBER_LABELS[parliamentarian.chamber] }}</p>
    </div>
  </RouterLink>
</template>
