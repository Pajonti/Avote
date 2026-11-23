<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api'
import { useFilters } from '../composables/useFilters'
import { ageFrom, CHAMBER_LABELS, formatDate, fullName } from '../format'
import type { ParliamentarianDetail } from '../types'
import Avatar from './Avatar.vue'
import GroupBadge from './GroupBadge.vue'

const route = useRoute()
const router = useRouter()
const { closeDetailLocation } = useFilters()

const detail = ref<ParliamentarianDetail | null>(null)
const status = ref<'loading' | 'ready' | 'notfound' | 'error'>('loading')
const closeButton = ref<HTMLButtonElement | null>(null)
let controller: AbortController | undefined

const id = computed(() => Number(route.params.id))

async function load() {
  controller?.abort()
  controller = new AbortController()
  status.value = 'loading'
  try {
    detail.value = await api.detail(id.value, controller.signal)
    status.value = 'ready'
  } catch (e) {
    if ((e as Error).name === 'AbortError') return
    status.value = (e as { status?: number }).status === 404 ? 'notfound' : 'error'
  }
}

function close() {
  router.push(closeDetailLocation())
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') close()
}

watch(id, load, { immediate: true })
onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  closeButton.value?.focus()
})
onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  controller?.abort()
})

const birth = computed(() => {
  const d = detail.value?.birthDate
  return d ? `${formatDate(d)} (${ageFrom(d)} ans)` : null
})
</script>

<template>
  <div class="overlay" @click.self="close">
    <section class="panel" role="dialog" aria-modal="true" aria-labelledby="detail-title">
      <button ref="closeButton" type="button" class="panel-close" aria-label="Fermer la fiche" @click="close">
        ×
      </button>

      <p v-if="status === 'loading'" class="state" role="status">Chargement…</p>
      <p v-else-if="status === 'notfound'" class="state">Cette fiche n'existe pas ou n'est plus active.</p>
      <div v-else-if="status === 'error'" class="state" role="alert">
        <p>Impossible de charger la fiche.</p>
        <button type="button" class="btn" @click="load">Réessayer</button>
      </div>

      <template v-else-if="detail">
        <header class="panel-head">
          <Avatar :photo-url="detail.photoUrl" :first-name="detail.firstName" :last-name="detail.lastName" />
          <div>
            <h2 id="detail-title">{{ fullName(detail) }}</h2>
            <p class="panel-chamber">{{ CHAMBER_LABELS[detail.chamber] }}</p>
            <GroupBadge :code="detail.groupCode" :label="detail.groupLabel" :color="detail.groupColor" />
          </div>
        </header>

        <dl class="facts">
          <template v-if="detail.groupLabel">
            <dt>Groupe politique</dt>
            <dd>{{ detail.groupLabel }}</dd>
          </template>
          <template v-if="detail.constituency">
            <dt>{{ detail.chamber === 'EP' ? 'Pays' : 'Circonscription' }}</dt>
            <dd>{{ detail.constituency }}</dd>
          </template>
          <template v-if="detail.mandateStart">
            <dt>Mandat depuis le</dt>
            <dd>{{ formatDate(detail.mandateStart) }}</dd>
          </template>
          <template v-if="birth">
            <dt>Naissance</dt>
            <dd>{{ birth }}</dd>
          </template>
          <template v-if="detail.profession">
            <dt>Profession</dt>
            <dd>{{ detail.profession }}</dd>
          </template>
          <template v-if="detail.email">
            <dt>Courriel</dt>
            <dd><a :href="`mailto:${detail.email}`">{{ detail.email }}</a></dd>
          </template>
        </dl>

        <ul class="links">
          <li v-if="detail.externalUrl">
            <a :href="detail.externalUrl" target="_blank" rel="noopener noreferrer">Fiche officielle</a>
          </li>
          <li v-if="detail.hatvpUrl">
            <a :href="detail.hatvpUrl" target="_blank" rel="noopener noreferrer">Déclarations (HATVP)</a>
          </li>
        </ul>
      </template>
    </section>
  </div>
</template>
