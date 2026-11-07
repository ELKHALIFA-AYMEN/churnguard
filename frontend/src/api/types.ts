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