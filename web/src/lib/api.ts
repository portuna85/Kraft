const backendBaseUrl = process.env.KRAFT_BACKEND_INTERNAL_URL ?? "http://backend:8080";
const publicBaseUrl = process.env.KRAFT_PUBLIC_BASE_URL ?? "http://localhost";

export type WinningNumber = {
  round: number;
  drawDate: string;
  numbers: number[];
  bonusNumber: number;
  firstPrizeAmount: number;
  secondPrize: number;
  secondWinners: number;
  totalSales: number;
  firstAccumAmount: number;
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

export type RoundFreshness = {
  latestRound: number;
  latestDrawDate: string;
  fresh: boolean;
  checkedAt: string;
};

export type PublicIncident = {
  round: number | null;
  type: string;
  resolved: boolean;
  occurredAt: string;
};

type RequestInitWithNext = RequestInit & {
  next?: {
    revalidate?: number;
  };
};

// W-3: 백엔드 에러 코드/메시지를 보존하는 커스텀 에러
export class BackendError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly status: number
  ) {
    super(message);
    this.name = "BackendError";
  }
}

async function fetchJson<T>(path: string, init?: RequestInitWithNext): Promise<T> {
  // W-1: SSR 렌더 무한 대기 방지
  const signal = AbortSignal.timeout(5000);
  const response = await fetch(`${backendBaseUrl}${path}`, { signal, ...init });
  if (!response.ok) {
    let code = "BACKEND_ERROR";
    let message = `Backend request failed: ${path} (${response.status})`;
    try {
      const body = await response.clone().json() as { code?: string; message?: string };
      if (body.code) code = body.code;
      if (body.message) message = body.message;
    } catch {
      // 바디 파싱 실패 시 기본 메시지 유지
    }
    throw new BackendError(code, message, response.status);
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

export async function getRoundFreshness(): Promise<RoundFreshness> {
  return fetchJson<RoundFreshness>("/api/v1/rounds/freshness", {
    next: { revalidate: 60 }
  });
}

export async function getPublicIncidents(): Promise<PublicIncident[]> {
  return fetchJson<PublicIncident[]>("/api/v1/status/incidents", {
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

