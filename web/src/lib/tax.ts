// 복권 기타소득세 구간 — 복권 및 복권기금법 시행령 제8조의2 / 소득세법 제129조
// 3억 초과: 33% (기타소득세 30% + 지방소득세 3%)
// 200만 초과 3억 이하: 22% (기타소득세 20% + 지방소득세 2%)
// 200만 이하: 비과세
const HIGH_PRIZE_THRESHOLD = 300_000_000;
const LOW_PRIZE_THRESHOLD = 2_000_000;
const HIGH_TAX_RATE = 0.33;
const LOW_TAX_RATE = 0.22;

export function calcAfterTax(amount: number): number {
  if (amount > HIGH_PRIZE_THRESHOLD) return Math.floor(amount * (1 - HIGH_TAX_RATE));
  if (amount > LOW_PRIZE_THRESHOLD) return Math.floor(amount * (1 - LOW_TAX_RATE));
  return amount;
}
