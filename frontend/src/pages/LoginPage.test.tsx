import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import LoginPage from './LoginPage'

const loginMock = vi.hoisted(() => vi.fn())

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ session: null, login: loginMock, logout: vi.fn() }),
}))

async function submit() {
  render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  )
  await userEvent.type(screen.getByLabelText('E-mail'), 'jean@example.com')
  await userEvent.type(screen.getByLabelText('Mot de passe'), 'secret-123')
  await userEvent.click(screen.getByRole('button', { name: 'Se connecter' }))
}

describe('LoginPage', () => {
  beforeEach(() => {
    loginMock.mockReset()
  })

  it('envoie l’e-mail et le mot de passe saisis', async () => {
    loginMock.mockResolvedValue(undefined)
    await submit()

    expect(loginMock).toHaveBeenCalledWith('jean@example.com', 'secret-123')
  })

  it('affiche un message uniforme pour de mauvais identifiants', async () => {
    loginMock.mockRejectedValue(new ApiError(401, 'Unauthorized'))
    await submit()

    expect(await screen.findByRole('alert')).toHaveTextContent('Identifiant ou mot de passe incorrect.')
  })

  it('explique qu’il faut patienter quand la limite est atteinte', async () => {
    loginMock.mockRejectedValue(new ApiError(429, 'Too Many Requests'))
    await submit()

    expect(await screen.findByRole('alert')).toHaveTextContent('Trop de tentatives')
  })
})