const backendBaseUrl = process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";
const publicBaseUrl = process.env.KRAFT_PUBLIC_BASE_URL ?? "http://localhost";

export type WinningNumber = {
  round: number;
  drawDate: string;
  numbers: number[];
  bonusNumber: number;
  firstPrizeAmount: number;
};

export type WinningNumberList = {
  items: WinningNumber[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type RecommendationResponse = {
  recommendations: number[][];
};

type RequestInitWithNext = RequestInit & {
  next?: {
    revalidate?: number;
  };
};

async function fetchJson<T>(path: string, init?: RequestInitWithNext): Promise<T> {
  const response = await fetch(`${backendBaseUrl}${path}`, init);
  if (!response.ok) {
    throw new Error(`Backend request failed: ${path} (${response.status})`);
  }
  return response.json() as Promise<T>;
}

export function getPublicBaseUrl(): string {
  return publicBaseUrl;
}

export async function getLatestWinningNumber(): Promise<WinningNumber> {
  return fetchJson<WinningNumber>("/api/v1/rounds/latest", {
    next: { revalidate: 60 }
  });
}

export async function getRounds(page = 0, size = 20): Promise<WinningNumberList> {
  return fetchJson<WinningNumberList>(`/api/v1/rounds?page=${page}&size=${size}`, {
    next: { revalidate: 60 }
  });
}

export async function getRound(round: number): Promise<WinningNumber> {
  return fetchJson<WinningNumber>(`/api/v1/rounds/${round}`, {
    next: { revalidate: 60 }
  });
}

export type BallFrequency = { ballNumber: number; frequency: number; lastRound: number };
export type PatternBucket = { bucketKey: string; count: number };
export type CompanionPair = { ballA: number; ballB: number; coCount: number };
export type RangeDistribution = { range: string; count: number };

export type FrequencyStatsResponse = {
  totalRounds: number;
  frequencies: BallFrequency[];
};

export type PatternStatsResponse = {
  totalRounds: number;
  oddCounts: PatternBucket[];
  highCounts: PatternBucket[];
  sumBuckets: PatternBucket[];
};

export type CompanionStatsResponse = {
  totalRounds: number;
  topPairs: CompanionPair[];
};

export type AnalysisResponse = {
  numbers: number[];
  oddCount: number;
  evenCount: number;
  lowCount: number;
  highCount: number;
  sumOfNumbers: number;
  sumBucket: string;
  consecutivePairCount: number;
  rangeDistribution: RangeDistribution[];
};

export async function getFrequencyStats(): Promise<FrequencyStatsResponse> {
  return fetchJson<FrequencyStatsResponse>("/api/v1/stats/frequency", {
    next: { revalidate: 600 }
  });
}

export async function getPatternStats(): Promise<PatternStatsResponse> {
  return fetchJson<PatternStatsResponse>("/api/v1/stats/patterns", {
    next: { revalidate: 600 }
  });
}

export async function getCompanionStats(): Promise<CompanionStatsResponse> {
  return fetchJson<CompanionStatsResponse>("/api/v1/stats/companion", {
    next: { revalidate: 600 }
  });
}

export async function analyzeNumbers(numbers: number[]): Promise<AnalysisResponse> {
  const response = await fetch(`${backendBaseUrl}/api/v1/stats/analysis`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ numbers }),
    cache: "no-store"
  });
  if (!response.ok) throw new Error(`Analysis failed (${response.status})`);
  return response.json() as Promise<AnalysisResponse>;
}

export async function getOpsSummary(token: string): Promise<{
  service: string;
  timezone: string;
  status: string;
  latestRound: number | null;
  latestDrawDate: string | null;
  checkedAt: string;
  fresh: boolean;
}> {
  return fetchJson("/ops/summary", {
    headers: {
      "X-Ops-Token": token
    },
    cache: "no-store"
  });
}
