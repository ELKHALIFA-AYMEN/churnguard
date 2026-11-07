import type { Page, Prediction } from '../api/types'

export function makePrediction(overrides: Partial<Prediction> = {}): Prediction {
  return {
    customerId: 'c1',
    externalId: '6521-YYTYI',
    contract: 'Month-to-month',
    tenureMonths: 1,
    monthlyCharges: 70.35,
    probability: 0.84,
    atRisk: true,
    predictedAt: '2026-11-07T10:00:00Z',
    factors: [
      { theme: 'Internet et prix', impact: 0.9, direction: 'increases' },
      { theme: 'Ancienneté', impact: 0.4, direction: 'increases' },
      { theme: 'Services', impact: 1.5, direction: 'decreases' },
    ],
    ...overrides,
  }
}

export function makePage(
  content: Prediction[],
  overrides: Partial<Page<Prediction>> = {},
): Page<Prediction> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, ...overrides }
}