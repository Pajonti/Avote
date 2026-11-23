import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Chamber, Filters } from '../types'

const CHAMBERS: Chamber[] = ['AN', 'SENAT', 'EP']

function first(value: unknown): string | null {
  const v = Array.isArray(value) ? value[0] : value
  return typeof v === 'string' ? v : null
}

/** Les filtres vivent dans l'URL : la grille est partageable et le bouton retour fonctionne. */
export function useFilters() {
  const route = useRoute()
  const router = useRouter()

  const filters = computed<Filters>(() => {
    const chamber = first(route.query.chamber)
    const group = Number(first(route.query.group))
    const page = Number(first(route.query.page))
    return {
      chamber: CHAMBERS.includes(chamber as Chamber) ? (chamber as Chamber) : null,
      group: Number.isInteger(group) && group > 0 ? group : null,
      q: first(route.query.q) ?? '',
      page: Number.isInteger(page) && page > 0 ? page : 0,
    }
  })

  function toQuery(f: Filters): Record<string, string> {
    const query: Record<string, string> = {}
    if (f.chamber) query.chamber = f.chamber
    if (f.group !== null) query.group = String(f.group)
    if (f.q.trim()) query.q = f.q.trim()
    if (f.page > 0) query.page = String(f.page)
    return query
  }

  /** Change un filtre ; tout changement autre que la page revient à la première page. */
  function update(patch: Partial<Filters>, { replace = false } = {}) {
    const next: Filters = { ...filters.value, ...patch }
    if (!('page' in patch)) next.page = 0
    if ('chamber' in patch && patch.chamber !== filters.value.chamber && !('group' in patch)) next.group = null
    const target = { path: '/', query: toQuery(next) }
    return replace ? router.replace(target) : router.push(target)
  }

  function detailLocation(id: number) {
    return { name: 'detail', params: { id }, query: toQuery(filters.value) }
  }

  function closeDetailLocation() {
    return { path: '/', query: toQuery(filters.value) }
  }

  return { filters, update, detailLocation, closeDetailLocation }
}
