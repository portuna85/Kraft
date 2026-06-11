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
  hits: number
  rate: number
}

export interface SumRangeStatDto {
  label: string
  min: number
  max: number
  hits: number
  rate: number
}

export interface PatternStatDto {
  oddEvenStats: OddEvenStatDto[]
  sumRangeStats: SumRangeStatDto[]
  totalDraws: number
}

export interface CompanionNumberDto {
  number: int
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
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
type int = number
