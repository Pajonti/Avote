import type {
  ChamberStats,
  Filters,
  GroupInfo,
  PageResponse,
  ParliamentarianDetail,
  ParliamentarianSummary,
} from './types'

export const PAGE_SIZE = 24

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
  }
}

async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(path, { signal, headers: { Accept: 'application/json' } })
  if (!response.ok) {
    throw new ApiError(response.status, `Erreur ${response.status}`)
  }
  return (await response.json()) as T
}

export function listQuery(filters: Filters): string {
  const params = new URLSearchParams()
  if (filters.chamber) params.set('chamber', filters.chamber)
  if (filters.group !== null) params.set('group', String(filters.group))
  if (filters.q.trim()) params.set('q', filters.q.trim())
  params.set('page', String(filters.page))
  params.set('size', String(PAGE_SIZE))
  return params.toString()
}

export const api = {
  list: (filters: Filters, signal?: AbortSignal) =>
    getJson<PageResponse<ParliamentarianSummary>>(`/api/v1/parliamentarians?${listQuery(filters)}`, signal),
  detail: (id: number, signal?: AbortSignal) =>
    getJson<ParliamentarianDetail>(`/api/v1/parliamentarians/${id}`, signal),
  groups: (chamber: string, signal?: AbortSignal) =>
    getJson<GroupInfo[]>(`/api/v1/groups?chamber=${encodeURIComponent(chamber)}`, signal),
  chambers: (signal?: AbortSignal) => getJson<ChamberStats[]>('/api/v1/chambers', signal),
}
