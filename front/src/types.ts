export type Chamber = 'AN' | 'SENAT' | 'EP'

export interface PageResponse<T> {
  items: T[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface ParliamentarianSummary {
  id: number
  chamber: Chamber
  civility: string | null
  firstName: string
  lastName: string
  photoUrl: string | null
  groupId: number | null
  groupCode: string | null
  groupLabel: string | null
  groupColor: string | null
  constituency: string | null
}

export interface ParliamentarianDetail {
  id: number
  chamber: Chamber
  civility: string | null
  firstName: string
  lastName: string
  gender: 'M' | 'F' | null
  birthDate: string | null
  profession: string | null
  photoUrl: string | null
  email: string | null
  externalUrl: string | null
  hatvpUrl: string | null
  groupId: number | null
  groupCode: string | null
  groupLabel: string | null
  groupColor: string | null
  constituency: string | null
  mandateStart: string | null
}

export interface GroupInfo {
  id: number
  chamber: Chamber
  code: string
  label: string
  color: string | null
  memberCount: number
}

export interface ChamberStats {
  chamber: Chamber
  label: string
  memberCount: number
}

export interface Filters {
  chamber: Chamber | null
  group: number | null
  q: string
  page: number
}
