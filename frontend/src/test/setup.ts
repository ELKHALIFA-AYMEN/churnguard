import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Nettoie l'écran simulé après chaque test pour qu'ils ne s'influencent pas.
afterEach(() => {
  cleanup()
})