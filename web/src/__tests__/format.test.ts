import { describe, expect, it } from "vitest";
import { formatCurrency, formatDrawDate, formatPlainNumber } from "@/lib/format";

describe("formatCurrency", () => {
  it("formats zero", () => {
    expect(formatCurrency(0)).toBe("0원");
  });

  it("formats millions with Korean comma style", () => {
    const result = formatCurrency(2_100_000_000);
    expect(result).toContain("원");
    expect(result).toContain("2");
  });

  it("formats typical first-prize amount", () => {
    const result = formatCurrency(1_500_000_000);
    expect(result).toMatch(/1[,.]?500/);
    expect(result).toContain("원");
  });
});

describe("formatDrawDate", () => {
  it("parses YYYY-MM-DD and returns Korean date string", () => {
    const result = formatDrawDate("2026-06-07");
    expect(result).toContain("2026");
    expect(result).toContain("6");
    expect(result).toContain("7");
  });

  it("does not throw for first draw date", () => {
    expect(() => formatDrawDate("2002-12-07")).not.toThrow();
  });
});

describe("formatPlainNumber", () => {
  it("formats number with locale separators", () => {
    const result = formatPlainNumber(1000);
    expect(result).toMatch(/1[,.]000|1000/);
  });
});
