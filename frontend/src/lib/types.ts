export interface ApiResponse<T> {
  success: boolean
  data: T | null
  error: { code: string; message: string } | null
}

export interface WinningNumberDto {
  round: number
  drawDate: string
  numbers: number[]
  bonusNumber: number
  firstPrize: number
  firstWinners: number
  totalSales: number
  secondPrize: number
  secondWinners: number
  fetchedAt: string | null
}

export interface WinningNumberPageDto {
  content: WinningNumberDto[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface NumberFrequencyDto {
  number: number
  count: number
  rate: number
}

export interface OddEvenStatDto {
  oddCount: number
  evenCount: number
  drawCount: number
  percent: number
  maxDrawCount: number
  theoreticalPercent: number
}

export interface SumRangeStatDto {
  rangeStart: number
  rangeEnd: number
  drawCount: number
  percent: number
  maxDrawCount: number
  theoreticalPercent: number
}

export interface PatternStatDto {
  oddEvenStats: OddEvenStatDto[]
  sumRangeStats: SumRangeStatDto[]
  totalDraws: number
}

export interface CompanionNumberDto {
  number: number
  count: number
  percent: number
  rank: number
}

export interface CombinationPrizeHitDto {
  round: number
  drawDate: string
}

export interface CombinationPrizeHistoryDto {
  numbers: number[]
  firstPrizeCount: number
  secondPrizeCount: number
  firstPrizeHits: CombinationPrizeHitDto[]
  secondPrizeHits: CombinationPrizeHitDto[]
}

export interface CombinationDto {
  numbers: number[]
}

export interface RecommendResponse {
  combinations: CombinationDto[]
}

export interface RecommendRequest {
  count: number
  oddCount?: number | null
  sumMin?: number | null
  sumMax?: number | null
  disabledRules?: string[]
}

export interface RuleDto {
  name: string
  reason: string
}

export interface SavedNumbersDto {
  id: number
  numbers: number[]
  label: string | null
  savedAt: string
}

export interface DataChangeLogEntry {
  round: number
  status: 'SUCCESS' | 'FAILED' | 'SKIPPED' | 'NOT_DRAWN'
  statusLabel: string
  statusCssClass: string
  fetchedAtFormatted: string
}

export interface ServiceStatusDto {
  latestRound: number | null
  latestDrawDate: string | null
  expectedRound: number
  upToDate: boolean
  roundsBehind: number
  recentLogs: DataChangeLogEntry[]
  appVersion: string | null
  buildTime: string | null
}

