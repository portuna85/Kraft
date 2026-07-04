import type { AnalysisResponse } from "@/lib/api";

// 고번호 경계값 — 백엔드 WinningStatisticsCacheService.analyze와 동일 (23 이상이 고번호)
const HIGH_NUMBER_THRESHOLD = 23;

// 합계 구간 경계값 — 백엔드 common/lotto/SumBuckets.bucketOf와 동일
function sumBucketOf(sum: number): string {
  if (sum < 66) return "21-65";
  if (sum < 111) return "66-110";
  if (sum < 156) return "111-155";
  if (sum < 201) return "156-200";
  return "201-255";
}

// 번호 범위 구간 5개 — 백엔드 rangeDistribution 로직과 동일
const RANGE_BUCKETS: { max: number; label: string }[] = [
  { max: 9, label: "1-9" },
  { max: 19, label: "10-19" },
  { max: 29, label: "20-29" },
  { max: 39, label: "30-39" },
  { max: 45, label: "40-45" }
];

export function analyzeNumbers(rawNumbers: number[]): AnalysisResponse {
  const numbers = [...rawNumbers].sort((a, b) => a - b);
  const oddCount = numbers.filter((n) => n % 2 !== 0).length;
  const evenCount = numbers.length - oddCount;
  const highCount = numbers.filter((n) => n >= HIGH_NUMBER_THRESHOLD).length;
  const lowCount = numbers.length - highCount;
  const sumOfNumbers = numbers.reduce((acc, n) => acc + n, 0);
  const sumBucket = sumBucketOf(sumOfNumbers);

  let consecutivePairCount = 0;
  for (let i = 0; i < numbers.length - 1; i++) {
    if (numbers[i + 1] - numbers[i] === 1) consecutivePairCount++;
  }

  let previousMax = 0;
  const rangeDistribution = RANGE_BUCKETS.map(({ max, label }) => {
    const count = numbers.filter((n) => n > previousMax && n <= max).length;
    previousMax = max;
    return { range: label, count };
  });

  return {
    numbers,
    oddCount,
    evenCount,
    lowCount,
    highCount,
    sumOfNumbers,
    sumBucket,
    consecutivePairCount,
    rangeDistribution
  };
}
