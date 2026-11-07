import { useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { api, ApiError } from '../api/client'
import type { ImportReport, RunReport } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { canWrite } from '../auth/roles'

const MAX_BYTES = 10 * 1024 * 1024

function messageOf(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 0) return 'Serveur injoignable.'
    if (error.status === 403) return "Votre rôle ne permet pas cette action."
    if (error.status === 409) return 'Un calcul est déjà en cours, réessayez dans un instant.'
    if (error.status === 503) return 'Le service de prédiction est indisponible. Réessayez plus tard.'
    if (error.status === 400) return error.message
  }
  return "L'opération a échoué."
}

export default function DataPage() {
  const { session } = useAuth()
  const [file, setFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | null>(null)
  const [importing, setImporting] = useState(false)
  const [importReport, setImportReport] = useState<ImportReport | null>(null)
  const [importError, setImportError] = useState<string | null>(null)
  const [running, setRunning] = useState(false)
  const [runReport, setRunReport] = useState<RunReport | null>(null)
  const [runError, setRunError] = useState<string | null>(null)

  if (!session || !canWrite(session.role)) {
    return (
      <main className="page">
        <h1>Données</h1>
        <p className="muted">
          Votre rôle permet de consulter les résultats, mais pas d'importer des données
          ni de lancer les calculs. Contactez un administrateur si besoin.
        </p>
      </main>
    )
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0] ?? null
    setImportReport(null)
    setImportError(null)
    setFile(null)
    setFileError(null)
    if (!selected) return

    if (!selected.name.toLowerCase().endsWith('.csv')) {
      setFileError('Seuls les fichiers .csv sont acceptés.')
    } else if (selected.size === 0) {
      setFileError('Le fichier est vide.')
    } else if (selected.size > MAX_BYTES) {
      setFileError('Le fichier dépasse 10 Mo.')
    } else {
      setFile(selected)
    }
  }

  async function handleImport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!file) return
    setImporting(true)
    setImportError(null)
    setImportReport(null)
    try {
      const form = new FormData()
      form.append('file', file)
      setImportReport(await api<ImportReport>('/api/customers/import', { method: 'POST', body: form }))
    } catch (e) {
      setImportError(messageOf(e))
    } finally {
      setImporting(false)
    }
  }

  async function handleRun() {
    setRunning(true)
    setRunError(null)
    setRunReport(null)
    try {
      setRunReport(await api<RunReport>('/api/predictions/run', { method: 'POST' }))
    } catch (e) {
      setRunError(messageOf(e))
    } finally {
      setRunning(false)
    }
  }

  return (
    <main className="page">
      <h1>Données</h1>

      <section className="panel">
        <h2>1. Importer vos clients</h2>
        <p className="muted">
          Fichier CSV au format du jeu « Telco Customer Churn » (10 Mo maximum). Un client déjà
          importé est mis à jour, jamais dupliqué.
        </p>
        <form onSubmit={handleImport}>
          <label htmlFor="file">Fichier CSV</label>
          <input id="file" type="file" accept=".csv,text/csv" onChange={handleFileChange} />
          {fileError && <p className="error" role="alert">{fileError}</p>}
          <button type="submit" disabled={!file || importing}>
            {importing ? 'Import en cours…' : 'Importer'}
          </button>
        </form>

        {importError && <p className="error" role="alert">{importError}</p>}
        {importReport && (
          <div className="report" role="status">
            <div className="stats">
              <Stat label="Lignes lues" value={importReport.totalRows} />
              <Stat label="Créés" value={importReport.created} />
              <Stat label="Mis à jour" value={importReport.updated} />
              <Stat label="Rejetés" value={importReport.rejected} />
            </div>
            {importReport.errors.length > 0 && (
              <>
                <h3>Lignes rejetées</h3>
                <table>
                  <thead>
                    <tr><th>Ligne</th><th>Raison</th></tr>
                  </thead>
                  <tbody>
                    {importReport.errors.map((item) => (
                      <tr key={`${item.line}-${item.message}`}>
                        <td>{item.line}</td>
                        <td>{item.message}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </>
            )}
          </div>
        )}
      </section>

      <section className="panel">
        <h2>2. Calculer les risques de départ</h2>
        <p className="muted">
          Chaque client reçoit une probabilité de départ et les raisons principales. Relancer le
          calcul met à jour les résultats existants.
        </p>
        <button type="button" onClick={handleRun} disabled={running}>
          {running ? 'Calcul en cours…' : 'Lancer le calcul'}
        </button>

        {runError && <p className="error" role="alert">{runError}</p>}
        {runReport && (
          <div className="report" role="status">
            <div className="stats">
              <Stat label="Clients analysés" value={runReport.customers} />
              <Stat label="Nouveaux résultats" value={runReport.created} />
              <Stat label="Mis à jour" value={runReport.updated} />
              <Stat label="À risque" value={runReport.atRisk} />
            </div>
          </div>
        )}
      </section>
    </main>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="stat">
      <span className="stat-value">{value.toLocaleString('fr-FR')}</span>
      <span className="muted">{label}</span>
    </div>
  )
}