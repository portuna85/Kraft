export function calcAfterTax(amount: number): number {
  if (amount > 300_000_000) return Math.floor(amount * (1 - 0.33));
  if (amount > 2_000_000) return Math.floor(amount * (1 - 0.22));
  return amount;
}
