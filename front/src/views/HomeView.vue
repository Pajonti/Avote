<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { api, listQuery } from '../api'
import ChamberTabs from '../components/ChamberTabs.vue'
import FilterBar from '../components/FilterBar.vue'
import PaginationBar from '../components/PaginationBar.vue'
import ParliamentarianCard from '../components/ParliamentarianCard.vue'
import { useFilters } from '../composables/useFilters'
import type { ChamberStats, GroupInfo, PageResponse, ParliamentarianSummary } from '../types'

const { filters, update, detailLocation } = useFilters()

const result = ref<PageResponse<ParliamentarianSummary> | null>(null)
const stats = ref<ChamberStats[]>([])
const groups = ref<GroupInfo[]>([])
const loading = ref(true)
const failed = ref(false)
let listController: AbortController | undefined
let groupsController: AbortController | undefined

async function loadList() {
  listController?.abort()
  listController = new AbortController()
  loading.value = true
  failed.value = false
  try {
    result.value = await api.list(filters.value, listController.signal)
    loading.value = false
  } catch (e) {
    if ((e as Error).name === 'AbortError') return
    failed.value = true
    loading.value = false
  }
}

async function loadGroups() {
  groupsController?.abort()
  const chamber = filters.value.chamber
  if (!chamber) {
    groups.value = []
    return
  }
  groupsController = new AbortController()
  try {
    groups.value = await api.groups(chamber, groupsController.signal)
  } catch {
    groups.value = [] // le filtre par groupe est facultatif
  }
}

api.chambers().then((s) => (stats.value = s)).catch(() => undefined)

// Clé sérialisée : ouvrir une fiche (même filtres) ne doit pas recharger la grille.
watch(() => listQuery(filters.value), loadList, { immediate: true })
watch(() => filters.value.chamber, loadGroups, { immediate: true })
onBeforeUnmount(() => {
  listController?.abort()
  groupsController?.abort()
})

const empty = computed(() => !loading.value && !failed.value && result.value?.items.length === 0)
</script>

<template>
  <header class="site-header">
    <div class="container">
      <h1 class="brand">Avote</h1>
      <p class="tagline">La vie citoyenne aux parlements : Assemblée nationale, Sénat et Parlement européen.</p>
    </div>
  </header>

  <main id="contenu" class="container">
    <ChamberTabs :model-value="filters.chamber" :stats="stats" @update:model-value="(c) => update({ chamber: c })" />
    <FilterBar :q="filters.q" :group="filters.group" :groups="groups"
               @update:q="(q) => update({ q }, { replace: true })" @update:group="(g) => update({ group: g })" />

    <div v-if="failed" class="state" role="alert">
      <p>Impossible de charger les parlementaires.</p>
      <button type="button" class="btn" @click="loadList">Réessayer</button>
    </div>
    <p v-else-if="empty" class="state">Aucun parlementaire ne correspond à ces critères.</p>

    <ul v-else class="grid" :class="{ 'is-loading': loading }" :aria-busy="loading">
      <li v-for="p in result?.items ?? []" :key="p.id">
        <ParliamentarianCard :parliamentarian="p" :to="detailLocation(p.id)" :show-chamber="filters.chamber === null" />
      </li>
    </ul>

    <PaginationBar v-if="result" :page="result.page" :total-pages="result.totalPages"
                   :total-items="result.totalItems" @change="(page) => update({ page })" />
  </main>

  <footer class="container site-footer">
    Données publiques : data.assemblee-nationale.fr, data.senat.fr, data.europarl.europa.eu.
  </footer>

  <RouterView />
</template>
