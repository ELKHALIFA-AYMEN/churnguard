import { useEffect, useState } from 'react'
import { api } from '../api/client'
import type { Page, Prediction } from '../api/types'

const PAGE_SIZE = 20

const CONTRACT_LABELS: Record<string, string> = {
  'Month-to-month': 'Mensuel',
  'One year': '1 an',
  'Two year': '2 ans',
}

const percent = new Intl.NumberFormat('fr-FR', { style: 'percent', maximumFractionDigits: 0 })
const amount = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/** La raison qui pèse le plus parmi celles qui AUGMENTENT le risque. */
function mainReason(prediction: Prediction): string {
  const reasons = prediction.factors
    .filter((factor) => factor.direction === 'increases')
    .sort((a, b) => Math.abs(b.impact) - Math.abs(a.impact))
  return reasons[0]?.theme ?? '—'
}

interface LoadState {
  page: number
  data: Page<Prediction> | null
  error: string | null
}

export default function DashboardPage() {
  const [page, setPage] = useState(0)
  const [state, setState] = useState<LoadState>({ page: -1, data: null, error: null })

  useEffect(() => {
    let cancelled = false
    api<Page<Prediction>>(`/api/predictions?atRiskOnly=true&page=${page}&size=${PAGE_SIZE}`)
      .then((data) => {
        if (!cancelled) setState({ page, data, error: null })
      })
      .catch(() => {
        if (!cancelled) setState({ page, data: null, error: 'Impossible de charger les résultats.' })
      })
    return () => {
      cancelled = true
    }
  }, [page])

  const loading = state.page !== page
  const data = state.data

  return (
    <main className="page">
      <h1>Clients à risque</h1>
      <p className="muted">
        Clients dont la probabilité de départ dépasse le seuil de décision, du plus au moins risqué.
      </p>

      {state.error && <p className="error" role="alert">{state.error}</p>}
      {loading && !state.error && <p className="muted" role="status">Chargement…</p>}

      {data && data.totalElements === 0 && (
        <div className="panel">
          <p>Aucun résultat pour le moment.</p>
          <p className="muted">
            Importez vos clients puis lancez le calcul depuis la page « Données ».
          </p>
        </div>
      )}

      {data && data.totalElements > 0 && (
        <div className="panel">
          <p className="muted">
            <strong>{data.totalElements.toLocaleString('fr-FR')}</strong> clients à risque
          </p>
          <table>
            <caption className="sr-only">Clients à risque triés par probabilité de départ</caption>
            <thead>
              <tr>
                <th>Client</th>
                <th>Contrat</th>
                <th>Ancienneté</th>
                <th>Facture mensuelle</th>
                <th>Risque de départ</th>
                <th>Raison principale</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((prediction) => (
                <tr key={prediction.customerId}>
                  <td>{prediction.externalId}</td>
                  <td>{CONTRACT_LABELS[prediction.contract] ?? prediction.contract}</td>
                  <td>{prediction.tenureMonths} mois</td>
                  <td>{amount.format(prediction.monthlyCharges)}</td>
                  <td>
                    <div className="risk">
                      <span className="risk-value">{percent.format(prediction.probability)}</span>
                      <span className="risk-track" aria-hidden="true">
                        <span
                          className="risk-fill"
                          style={{ width: `${Math.round(prediction.probability * 100)}%` }}
                        />
                      </span>
                    </div>
                  </td>
                  <td>{mainReason(prediction)}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <nav className="pager" aria-label="Pagination">
            <button
              type="button"
              className="secondary"
              onClick={() => setPage((p) => Math.max(p - 1, 0))}
              disabled={page === 0 || loading}
            >
              Précédent
            </button>
            <span className="muted">
              Page {data.page + 1} sur {data.totalPages}
            </span>
            <button
              type="button"
              className="secondary"
              onClick={() => setPage((p) => p + 1)}
              disabled={page + 1 >= data.totalPages || loading}
            >
              Suivant
            </button>
          </nav>
        </div>
      )}
    </main>
  )
}