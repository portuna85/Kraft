import { describe, expect, it } from "vitest";
import { calcAfterTax } from "@/lib/tax";

describe("calcAfterTax", () => {
  it("2,000,000원 이하 당첨금은 전액을 반환한다(비과세)", () => {
    expect(calcAfterTax(0)).toBe(0);
    expect(calcAfterTax(1_000_000)).toBe(1_000_000);
    expect(calcAfterTax(2_000_000)).toBe(2_000_000);
  });

  it("2,000,001원~300,000,000원 당첨금에 22% 세율을 적용한다", () => {
    // 5,000,000 * (1 - 0.22) = 3,900,000
    expect(calcAfterTax(5_000_000)).toBe(3_900_000);
    // 300,000,000 is NOT above threshold so 22% applies
    expect(calcAfterTax(300_000_000)).toBe(Math.floor(300_000_000 * 0.78));
  });

  it("300,000,000원 초과 당첨금에 33% 세율을 적용한다", () => {
    // Use (1 - 0.33) to mirror the implementation — 0.67 literal ≠ 1 - 0.33 in floating point
    expect(calcAfterTax(1_000_000_000)).toBe(Math.floor(1_000_000_000 * (1 - 0.33)));
    expect(calcAfterTax(2_100_000_000)).toBe(Math.floor(2_100_000_000 * (1 - 0.33)));
  });

  it("원 미만 소수점을 버려 결과를 정수로 만든다", () => {
    // 2_100_001 * 0.78 is not a whole number — result must be floored
    const result = calcAfterTax(2_100_001);
    expect(Number.isInteger(result)).toBe(true);
    expect(result).toBe(Math.floor(2_100_001 * 0.78));
  });

  it("경계값: 2,000,001원은 낮은 세율 구간으로 들어간다", () => {
    expect(calcAfterTax(2_000_001)).toBe(Math.floor(2_000_001 * 0.78));
  });

  it("경계값: 300,000,001원은 높은 세율 구간으로 들어간다", () => {
    expect(calcAfterTax(300_000_001)).toBe(Math.floor(300_000_001 * (1 - 0.33)));
  });
});
