export interface RowError {
  line: number
  message: string
}

export interface ImportReport {
  totalRows: number
  created: number
  updated: number
  rejected: number
  errors: RowError[]
}

export interface RunReport {
  customers: number
  created: number
  updated: number
  atRisk: number
}

export interface Factor {
  theme: string
  impact: number
  direction: 'increases' | 'decreases'
}

export interface Prediction {
  customerId: string
  externalId: string
  contract: string
  tenureMonths: number
  monthlyCharges: number
  probability: number
  atRisk: boolean
  predictedAt: string
  factors: Factor[]
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}