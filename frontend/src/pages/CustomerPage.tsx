import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { isAdmin } from '../auth/roles'
import type { Prediction } from '../api/types'

const CONTRACT_LABELS: Record<string, string> = {
  'Month-to-month': 'Mensuel',
  'One year': '1 an',
  'Two year': '2 ans',
}

const percent = new Intl.NumberFormat('fr-FR', { style: 'percent', maximumFractionDigits: 0 })
const amount = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

/** Garde uniquement des caractères sûrs pour un nom de fichier. */
function safeName(value: string): string {
  return value.replace(/[^A-Za-z0-9_-]/g, '_')
}

function downloadJson(filename: string, data: unknown) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

interface LoadState {
  id: string
  data: Prediction | null
  error: string | null
}

export default function CustomerPage() {
  const { customerId = '' } = useParams()
  const { session } = useAuth()
  const navigate = useNavigate()
  const [state, setState] = useState<LoadState>({ id: '', data: null, error: null })
  const [confirming, setConfirming] = useState(false)
  const [busy, setBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

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

  async function exportData() {
    setBusy(true)
    setActionError(null)
    try {
      const data = await api<unknown>(`/api/customers/${encodeURIComponent(customerId)}/export`)
      downloadJson(`client-${safeName(prediction?.externalId ?? customerId)}.json`, data)
    } catch {
      setActionError('L’export a échoué.')
    } finally {
      setBusy(false)
    }
  }

  async function deleteCustomer() {
    setBusy(true)
    setActionError(null)
    try {
      await api<void>(`/api/customers/${encodeURIComponent(customerId)}`, { method: 'DELETE' })
      navigate('/')
    } catch {
      setActionError('La suppression a échoué.')
      setBusy(false)
    }
  }

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

          {session && isAdmin(session.role) && (
            <div className="panel">
              <h2>Données personnelles</h2>
              <p className="muted">
                Pour répondre à une demande du client : obtenir une copie de ses données, ou les effacer.
              </p>

              {actionError && <p className="error" role="alert">{actionError}</p>}

              {!confirming && (
                <div className="actions">
                  <button type="button" className="secondary" onClick={exportData} disabled={busy}>
                    Exporter les données
                  </button>
                  <button type="button" className="danger" onClick={() => setConfirming(true)} disabled={busy}>
                    Supprimer ce client
                  </button>
                </div>
              )}

              {confirming && (
                <div role="alertdialog" aria-labelledby="confirm-title">
                  <p id="confirm-title">
                    <strong>Cette suppression est définitive</strong> : le client et sa prédiction seront effacés.
                  </p>
                  <div className="actions">
                    <button type="button" className="danger" onClick={deleteCustomer} disabled={busy}>
                      Confirmer la suppression
                    </button>
                    <button type="button" className="secondary" onClick={() => setConfirming(false)} disabled={busy}>
                      Annuler
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          <p className="muted">
            Ces thèmes résument l'influence des caractéristiques du client sur le score du modèle.
            Ce sont des indications, pas des certitudes.
          </p>
        </>
      )}
    </main>
  )
}