import { describe, expect, it } from 'vitest'
import { listQuery } from './api'
import { ageFrom, initials, safeColor } from './format'

describe('format', () => {
  it('construit les initiales, accents compris', () => {
    expect(initials({ firstName: 'élodie', lastName: 'Martin' })).toBe('ÉM')
  })

  it("calcule l'âge en tenant compte de l'anniversaire", () => {
    const today = new Date('2026-06-15')
    expect(ageFrom('1980-06-16', today)).toBe(45)
    expect(ageFrom('1980-06-15', today)).toBe(46)
  })

  it('refuse les couleurs non hexadécimales', () => {
    expect(safeColor('#B5E2F9')).toBe('#B5E2F9')
    expect(safeColor('red; background:url(x)')).toBeNull()
    expect(safeColor(null)).toBeNull()
  })
})

describe('api', () => {
  it('ne transmet que les filtres renseignés', () => {
    expect(listQuery({ chamber: null, group: null, q: '  ', page: 0 })).toBe('page=0&size=24')
    expect(listQuery({ chamber: 'AN', group: 3, q: ' Bergé ', page: 2 })).toBe(
      'chamber=AN&group=3&q=Berg%C3%A9&page=2&size=24',
    )
  })
})
