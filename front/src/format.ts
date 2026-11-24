import type { Chamber } from './types'

export const CHAMBER_LABELS: Record<Chamber, string> = {
  AN: 'Assemblée nationale',
  SENAT: 'Sénat',
  EP: 'Parlement européen',
}

export const CHAMBER_SHORT: Record<Chamber, string> = {
  AN: 'Députés',
  SENAT: 'Sénateurs',
  EP: 'Eurodéputés',
}

export function fullName(p: { firstName: string; lastName: string }): string {
  return `${p.firstName} ${p.lastName}`
}

export function initials(p: { firstName: string; lastName: string }): string {
  const first = Array.from(p.firstName.trim())[0] ?? ''
  const last = Array.from(p.lastName.trim())[0] ?? ''
  return (first + last).toUpperCase()
}

export function ageFrom(isoDate: string, today: Date = new Date()): number {
  const birth = new Date(isoDate)
  let age = today.getFullYear() - birth.getFullYear()
  const beforeBirthday =
    today.getMonth() < birth.getMonth() ||
    (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate())
  if (beforeBirthday) age -= 1
  return age
}

export function formatDate(isoDate: string): string {
  return new Intl.DateTimeFormat('fr-FR', { dateStyle: 'long' }).format(new Date(isoDate))
}

/** Accepte uniquement une couleur hexadécimale (la valeur vient d'une API externe). */
export function safeColor(color: string | null): string | null {
  return color && /^#[0-9a-fA-F]{3,8}$/.test(color) ? color : null
}
