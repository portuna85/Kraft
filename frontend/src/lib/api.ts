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
  ServiceStatusDto,
  RuleDto,
  SavedNumbersDto,
} from './types'

const BASE = process.env.NEXT_PUBLIC_API_BASE ?? ''

async function get<T>(path: string, headers?: Record<string, string>): Promise<T> {
  const res = await fetch(`${BASE}${path}`, { headers })
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`
    try {
      const json: ApiResponse<never> = await res.json()
      if (json.error?.message) message = json.error.message
    } catch { /* JSON 파싱 실패 — HTTP 상태 메시지 사용 */ }
    throw new Error(message)
  }
  const json: ApiResponse<T> = await res.json()
  if (!json.success || json.data == null) throw new Error(json.error?.message ?? 'API error')
  return json.data
}

async function post<T>(path: string, body: unknown, headers?: Record<string, string>): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`
    try {
      const json: ApiResponse<never> = await res.json()
      if (json.error?.message) message = json.error.message
    } catch { /* JSON 파싱 실패 — HTTP 상태 메시지 사용 */ }
    throw new Error(message)
  }
  const json: ApiResponse<T> = await res.json()
  if (!json.success || json.data == null) throw new Error(json.error?.message ?? 'API error')
  return json.data
}

async function del(path: string, headers?: Record<string, string>): Promise<void> {
  const res = await fetch(`${BASE}${path}`, { method: 'DELETE', headers })
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`
    try {
      const json: ApiResponse<never> = await res.json()
      if (json.error?.message) message = json.error.message
    } catch { /* JSON 파싱 실패 — HTTP 상태 메시지 사용 */ }
    throw new Error(message)
  }
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
    rules: () => get<RuleDto[]>('/api/v1/numbers/recommend/rules'),
  },
  saved: {
    list: (deviceToken: string) =>
      get<SavedNumbersDto[]>('/api/v1/saved', { 'X-Device-Token': deviceToken }),
    save: (deviceToken: string, numbers: number[], label?: string) =>
      post<SavedNumbersDto>('/api/v1/saved', { numbers, label: label ?? null }, { 'X-Device-Token': deviceToken }),
    delete: (id: number, deviceToken: string) =>
      del(`/api/v1/saved/${id}`, { 'X-Device-Token': deviceToken }),
  },
  service: {
    status: () => get<ServiceStatusDto>('/api/v1/status'),
  },
}
