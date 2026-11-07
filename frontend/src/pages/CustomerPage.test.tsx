import { render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { makePrediction } from '../test/fixtures'
import CustomerPage from './CustomerPage'

const apiMock = vi.hoisted(() => vi.fn())
vi.mock('../api/client', () => ({ api: apiMock }))

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/clients/abc']}>
      <Routes>
        <Route path="/clients/:customerId" element={<CustomerPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('CustomerPage', () => {
  beforeEach(() => {
    apiMock.mockReset()
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
})