import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/client'
import type { Prediction } from '../api/types'

const CONTRACT_LABELS: Record<string, string> = {
  'Month-to-month': 'Mensuel',
  'One year': '1 an',
  'Two year': '2 ans',
}

const percent = new Intl.NumberFormat('fr-FR', { style: 'percent', maximumFractionDigits: 0 })
const amount = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

interface LoadState {
  id: string
  data: Prediction | null
  error: string | null
}

export default function CustomerPage() {
  const { customerId = '' } = useParams()
  const [state, setState] = useState<LoadState>({ id: '', data: null, error: null })

  useEffect(() => {
    let cancelled = false
    api<Prediction>(`/api/predictions/customers/${encodeURIComponent(customerId)}`)
      .then((data) => {
        if (!cancelled) setState({ id: customerId, data, error: null })
      })
      .catch(() => {
        if (!cancelled) setState({ id: customerId, data: null, error: 'Client introuvable ou inaccessible.' })
      })
    return () => {
      cancelled = true
    }
  }, [customerId])

  const loading = state.id !== customerId
  const prediction = state.data

  const sorted = prediction
    ? [...prediction.factors].sort((a, b) => Math.abs(b.impact) - Math.abs(a.impact))
    : []
  const increases = sorted.filter((factor) => factor.direction === 'increases')
  const decreases = sorted.filter((factor) => factor.direction === 'decreases')
  const maxImpact = Math.max(...sorted.map((factor) => Math.abs(factor.impact)), 0.0001)

  function renderFactors(factors: typeof sorted, empty: string) {
    if (factors.length === 0) return <p className="muted">{empty}</p>
    return (
      <ul className="factors">
        {factors.map((factor) => (
          <li key={factor.theme}>
            <span className="factor-name">{factor.theme}</span>
            <span className="risk-track" aria-hidden="true">
              <span
                className="risk-fill"
                style={{
                  width: `${Math.round((Math.abs(factor.impact) / maxImpact) * 100)}%`,
                  background: factor.direction === 'increases' ? 'var(--danger)' : 'var(--ok, #2e7d32)',
                }}
              />
            </span>
          </li>
        ))}
      </ul>
    )
  }

  return (
    <main className="page">
      <p><Link to="/">← Retour aux clients à risque</Link></p>

      {loading && !state.error && <p className="muted" role="status">Chargement…</p>}
      {!loading && state.error && <p className="error" role="alert">{state.error}</p>}

      {!loading && prediction && (
        <>
          <h1>Client {prediction.externalId}</h1>

          <div className="panel">
            <p>
              Risque de départ : <strong>{percent.format(prediction.probability)}</strong>{' '}
              {prediction.atRisk ? '(client à risque)' : '(sous le seuil de décision)'}
            </p>
            <p className="muted">
              Contrat {CONTRACT_LABELS[prediction.contract] ?? prediction.contract} ·{' '}
              {prediction.tenureMonths} mois d'ancienneté · facture mensuelle{' '}
              {amount.format(prediction.monthlyCharges)}
            </p>
          </div>

          <div className="panel">
            <h2>Ce qui augmente le risque</h2>
            {renderFactors(increases, 'Aucun facteur n’augmente le risque.')}
          </div>

          <div className="panel">
            <h2>Ce qui rassure</h2>
            {renderFactors(decreases, 'Aucun facteur ne rassure.')}
          </div>

          <p className="muted">
            Ces thèmes résument l'influence des caractéristiques du client sur le score du modèle.
            Ce sont des indications, pas des certitudes.
          </p>
        </>
      )}
    </main>
  )
}