import type {
  ApiResponse,
  WinningNumberDto,
  WinningNumberPageDto,
  NumberFrequencyDto,
  PatternStatDto,
  CompanionNumberDto,
  CombinationPrizeHistoryDto,
  RecommendResponse,
  RecommendRequest,
} from './types'

const BASE = process.env.NEXT_PUBLIC_API_BASE ?? ''

async function get<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  const json: ApiResponse<T> = await res.json()
  if (!json.success || json.data == null) throw new Error(json.error?.message ?? 'API error')
  return json.data
}

async function post<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
  const json: ApiResponse<T> = await res.json()
  if (!json.success || json.data == null) throw new Error(json.error?.message ?? 'API error')
  return json.data
}

export const api = {
  rounds: {
    latest: () => get<WinningNumberDto>('/api/v1/rounds/latest'),
    get: (round: number) => get<WinningNumberDto>(`/api/v1/rounds/${round}`),
    list: (page = 0, size = 20) =>
      get<WinningNumberPageDto>(`/api/v1/rounds?page=${page}&size=${size}`),
  },
  stats: {
    frequency: (period?: number) =>
      get<NumberFrequencyDto[]>(`/api/v1/stats/frequency${period ? `?period=${period}` : ''}`),
    patterns: () => get<PatternStatDto>('/api/v1/stats/patterns'),
    companion: (target: number) =>
      get<CompanionNumberDto[]>(`/api/v1/stats/companion?target=${target}`),
    analysis: (numbers: number[]) =>
      post<CombinationPrizeHistoryDto>('/api/v1/stats/analysis', { numbers }),
  },
  numbers: {
    recommend: (req: RecommendRequest) =>
      post<RecommendResponse>('/api/v1/numbers/recommend', req),
  },
}
