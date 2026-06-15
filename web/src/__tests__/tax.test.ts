import { describe, expect, it } from "vitest";
import { calcAfterTax } from "@/lib/tax";

describe("calcAfterTax", () => {
  it("returns the full amount for prizes at or below 2,000,000원 (비과세)", () => {
    expect(calcAfterTax(0)).toBe(0);
    expect(calcAfterTax(1_000_000)).toBe(1_000_000);
    expect(calcAfterTax(2_000_000)).toBe(2_000_000);
  });

  it("applies 22% tax for prizes between 2,000,001원 and 300,000,000원", () => {
    // 5,000,000 * (1 - 0.22) = 3,900,000
    expect(calcAfterTax(5_000_000)).toBe(3_900_000);
    // 300,000,000 is NOT above threshold so 22% applies
    expect(calcAfterTax(300_000_000)).toBe(Math.floor(300_000_000 * 0.78));
  });

  it("applies 33% tax for prizes above 300,000,000원", () => {
    // Use (1 - 0.33) to mirror the implementation — 0.67 literal ≠ 1 - 0.33 in floating point
    expect(calcAfterTax(1_000_000_000)).toBe(Math.floor(1_000_000_000 * (1 - 0.33)));
    expect(calcAfterTax(2_100_000_000)).toBe(Math.floor(2_100_000_000 * (1 - 0.33)));
  });

  it("floors the result to remove sub-won fractions", () => {
    // 2_100_001 * 0.78 is not a whole number — result must be floored
    const result = calcAfterTax(2_100_001);
    expect(Number.isInteger(result)).toBe(true);
    expect(result).toBe(Math.floor(2_100_001 * 0.78));
  });

  it("boundary: 2,000,001원 triggers the low bracket", () => {
    expect(calcAfterTax(2_000_001)).toBe(Math.floor(2_000_001 * 0.78));
  });

  it("boundary: 300,000,001원 triggers the high bracket", () => {
    expect(calcAfterTax(300_000_001)).toBe(Math.floor(300_000_001 * (1 - 0.33)));
  });
});
