import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { makePage, makePrediction } from '../test/fixtures'
import DashboardPage from './DashboardPage'

const apiMock = vi.hoisted(() => vi.fn())
vi.mock('../api/client', () => ({ api: apiMock }))

function renderPage() {
  return render(
    <MemoryRouter>
      <DashboardPage />
    </MemoryRouter>,
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    apiMock.mockReset()
  })

  it('explique quoi faire quand il n’y a aucun résultat', async () => {
    apiMock.mockResolvedValue(makePage([], { totalPages: 0 }))
    renderPage()

    expect(await screen.findByText(/Aucun résultat/)).toBeInTheDocument()
    expect(screen.getByText(/page « Données »/)).toBeInTheDocument()
  })

  it('affiche le client, son risque et sa raison principale', async () => {
    apiMock.mockResolvedValue(makePage([makePrediction()]))
    renderPage()

    const link = await screen.findByRole('link', { name: '6521-YYTYI' })
    expect(link).toHaveAttribute('href', '/clients/c1')
    expect(screen.getByText('Mensuel')).toBeInTheDocument()
    expect(screen.getByText(/84\s*%/)).toBeInTheDocument()
    // La raison principale est le facteur qui AUGMENTE le plus le risque,
    // même si un facteur rassurant pèse plus lourd.
    expect(screen.getByText('Internet et prix')).toBeInTheDocument()
    expect(screen.queryByText('Services')).not.toBeInTheDocument()
  })

  it('charge la page suivante au clic', async () => {
    const page0 = makePage([makePrediction()], { totalElements: 2, totalPages: 2 })
    const page1 = makePage([makePrediction({ customerId: 'c2', externalId: '0002-AAAAA' })], {
      page: 1,
      totalElements: 2,
      totalPages: 2,
    })
    apiMock.mockImplementation(async (path: string) => (path.includes('page=1') ? page1 : page0))
    renderPage()

    await screen.findByText('Page 1 sur 2')
    expect(screen.getByRole('button', { name: 'Précédent' })).toBeDisabled()

    await userEvent.click(screen.getByRole('button', { name: 'Suivant' }))

    await screen.findByText('Page 2 sur 2')
    expect(apiMock).toHaveBeenLastCalledWith(expect.stringContaining('page=1'))
    expect(screen.getByRole('button', { name: 'Suivant' })).toBeDisabled()
  })

  it('affiche une erreur si le chargement échoue', async () => {
    apiMock.mockRejectedValue(new Error('réseau'))
    renderPage()

    expect(await screen.findByRole('alert')).toHaveTextContent('Impossible de charger')
  })
})