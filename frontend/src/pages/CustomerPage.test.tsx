import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { makePrediction } from '../test/fixtures'
import CustomerPage from './CustomerPage'

const apiMock = vi.hoisted(() => vi.fn())
const auth = vi.hoisted(() => ({ role: 'ADMIN' }))

vi.mock('../api/client', () => ({ api: apiMock }))
vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ session: { role: auth.role }, login: vi.fn(), logout: vi.fn() }),
}))

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/clients/abc']}>
      <Routes>
        <Route path="/" element={<p>Liste des clients</p>} />
        <Route path="/clients/:customerId" element={<CustomerPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

/** Réponses par défaut : fiche, export et suppression. */
function stubApi() {
  apiMock.mockImplementation(async (path: string, init?: RequestInit) => {
    if (init?.method === 'DELETE') return undefined
    if (path.endsWith('/export')) return { customer: {}, prediction: null }
    return makePrediction()
  })
}

describe('CustomerPage', () => {
  beforeEach(() => {
    apiMock.mockReset()
    auth.role = 'ADMIN'
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('répartit les facteurs entre ce qui augmente le risque et ce qui rassure', async () => {
    apiMock.mockResolvedValue(makePrediction())
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Client 6521-YYTYI' })).toBeInTheDocument()
    expect(apiMock).toHaveBeenCalledWith('/api/predictions/customers/abc')

    const increases = screen.getByRole('heading', { name: 'Ce qui augmente le risque' }).parentElement!
    const reassuring = screen.getByRole('heading', { name: 'Ce qui rassure' }).parentElement!

    expect(within(increases).getByText('Internet et prix')).toBeInTheDocument()
    expect(within(increases).getByText('Ancienneté')).toBeInTheDocument()
    expect(within(reassuring).getByText('Services')).toBeInTheDocument()
    expect(within(reassuring).queryByText('Internet et prix')).not.toBeInTheDocument()
  })

  it('affiche un message quand rien ne rassure', async () => {
    apiMock.mockResolvedValue(
      makePrediction({
        factors: [
          { theme: 'Contrat', impact: 0.8, direction: 'increases' },
          { theme: 'Ancienneté', impact: 0.4, direction: 'increases' },
        ],
      }),
    )
    renderPage()

    expect(await screen.findByText('Aucun facteur ne rassure.')).toBeInTheDocument()
  })

  it('indique quand le client est sous le seuil de décision', async () => {
    apiMock.mockResolvedValue(makePrediction({ probability: 0.1, atRisk: false }))
    renderPage()

    expect(await screen.findByText(/sous le seuil de décision/)).toBeInTheDocument()
    expect(screen.getByText(/10\s*%/)).toBeInTheDocument()
  })

  it('affiche une erreur claire si le client est introuvable', async () => {
    apiMock.mockRejectedValue(new Error('404'))
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Client introuvable ou inaccessible.')
  })

  it('montre les actions sur les données personnelles à un administrateur', async () => {
    stubApi()
    renderPage()

    expect(await screen.findByRole('button', { name: 'Exporter les données' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Supprimer ce client' })).toBeInTheDocument()
  })

  it('cache ces actions aux autres rôles', async () => {
    auth.role = 'VIEWER'
    stubApi()
    renderPage()

    await screen.findByRole('heading', { name: 'Client 6521-YYTYI' })
    expect(screen.queryByRole('button', { name: 'Exporter les données' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Supprimer ce client' })).not.toBeInTheDocument()
  })

  it('télécharge les données du client en JSON', async () => {
    stubApi()
    const createUrl = vi.fn(() => 'blob:test')
    URL.createObjectURL = createUrl
    URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Exporter les données' }))

    expect(apiMock).toHaveBeenCalledWith('/api/customers/abc/export')
    expect(createUrl).toHaveBeenCalledTimes(1)
  })

  it('demande une confirmation avant de supprimer, puis revient à la liste', async () => {
    stubApi()
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Supprimer ce client' }))

    expect(screen.getByText(/Cette suppression est définitive/)).toBeInTheDocument()
    expect(apiMock).not.toHaveBeenCalledWith('/api/customers/abc', expect.anything())

    await userEvent.click(screen.getByRole('button', { name: 'Confirmer la suppression' }))

    expect(apiMock).toHaveBeenCalledWith('/api/customers/abc', { method: 'DELETE' })
    expect(await screen.findByText('Liste des clients')).toBeInTheDocument()
  })

  it('ne supprime rien quand on annule', async () => {
    stubApi()
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Supprimer ce client' }))
    await userEvent.click(screen.getByRole('button', { name: 'Annuler' }))

    expect(apiMock).not.toHaveBeenCalledWith('/api/customers/abc', expect.anything())
    expect(screen.getByRole('button', { name: 'Supprimer ce client' })).toBeInTheDocument()
  })
})