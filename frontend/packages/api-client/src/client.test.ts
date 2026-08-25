import { describe, it, expect, beforeEach, vi } from 'vitest'
import { api } from './index'

class LocalStorageMock {
  private store: Record<string, string> = {}
  clear() {
    this.store = {}
  }
  getItem(key: string): string | null {
    return key in this.store ? this.store[key] : null
  }
  setItem(key: string, value: string) {
    this.store[key] = String(value)
  }
  removeItem(key: string) {
    delete this.store[key]
  }
}

describe('ApiClient', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.stubGlobal('localStorage', new LocalStorageMock())
    vi.stubGlobal('window', { location: { href: '' } })
  })

  it('attaches Bearer token from localStorage', async () => {
    localStorage.setItem('accessToken', 'tok123')
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ ok: true }), { status: 200, headers: { 'Content-Type': 'application/json' } })
    )
    vi.stubGlobal('fetch', fetchMock)

    await api.get<{ ok: boolean }>('/things')

    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining('/api/things'),
      expect.objectContaining({ method: 'GET' })
    )
    const headers = fetchMock.mock.calls[0][1].headers as Headers
    expect(headers.get('Authorization')).toBe('Bearer tok123')
  })

  it('throws ApiError with status on non-ok response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(JSON.stringify({ message: 'nope' }), { status: 404 }))
    )
    await expect(api.get('/missing')).rejects.toMatchObject({ status: 404, message: 'nope' })
  })

  it('refreshes token on 401 and retries once', async () => {
    localStorage.setItem('refreshToken', 'refresh123')
    const sequence = [
      new Response('', { status: 401 }),
      new Response(JSON.stringify({ accessToken: 'newTok', refreshToken: 'newRef' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
      new Response(JSON.stringify({ ok: true }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    ]
    const fetchMock = vi.fn().mockImplementation(() => Promise.resolve(sequence.shift()!))
    vi.stubGlobal('fetch', fetchMock)

    const result = await api.get<{ ok: boolean }>('/secure')

    expect(result).toEqual({ ok: true })
    expect(localStorage.getItem('accessToken')).toBe('newTok')
    // 1 original + 1 refresh + 1 retry
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('clears tokens when refresh fails', async () => {
    localStorage.setItem('refreshToken', 'bad')
    const sequence = [
      new Response('', { status: 401 }),
      new Response('', { status: 401 }),
    ]
    vi.stubGlobal('fetch', vi.fn().mockImplementation(() => Promise.resolve(sequence.shift()!)))

    await expect(api.get('/secure')).rejects.toBeDefined()
    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
  })
})
