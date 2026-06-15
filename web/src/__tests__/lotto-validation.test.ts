import { describe, expect, it } from "vitest";
import { validateLottoNumbers, parseExcludedNumbers } from "@/lib/lotto-validation";

describe("validateLottoNumbers", () => {
  it("accepts a valid 6-number set", () => {
    const result = validateLottoNumbers([1, 7, 15, 23, 38, 45]);
    expect(result.ok).toBe(true);
    if (result.ok) expect(result.numbers).toEqual([1, 7, 15, 23, 38, 45]);
  });

  it("rejects fewer than 6 numbers", () => {
    const result = validateLottoNumbers([1, 2, 3, 4, 5]);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/6개/);
  });

  it("rejects more than 6 numbers", () => {
    const result = validateLottoNumbers([1, 2, 3, 4, 5, 6, 7]);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/6개/);
  });

  it("rejects a number below 1", () => {
    const result = validateLottoNumbers([0, 2, 3, 4, 5, 6]);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/1에서 45/);
  });

  it("rejects a number above 45", () => {
    const result = validateLottoNumbers([1, 2, 3, 4, 5, 46]);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/1에서 45/);
  });

  it("rejects duplicate numbers", () => {
    const result = validateLottoNumbers([1, 2, 3, 4, 5, 5]);
    expect(result.ok).toBe(false);
    if (!result.ok) expect(result.message).toMatch(/중복/);
  });

  it("accepts boundary numbers 1 and 45", () => {
    const result = validateLottoNumbers([1, 2, 3, 4, 5, 45]);
    expect(result.ok).toBe(true);
  });

  it("accepts string-encoded numbers", () => {
    const result = validateLottoNumbers(["1", "7", "15", "23", "38", "45"]);
    expect(result.ok).toBe(true);
  });
});

describe("parseExcludedNumbers", () => {
  it("parses a comma-separated list of valid numbers", () => {
    const { valid, ignored } = parseExcludedNumbers("1, 10, 45");
    expect(valid).toEqual([1, 10, 45]);
    expect(ignored).toEqual([]);
  });

  it("reports out-of-range values in ignored", () => {
    const { valid, ignored } = parseExcludedNumbers("0, 5, 46");
    expect(valid).toEqual([5]);
    expect(ignored).toEqual(["0", "46"]);
  });

  it("reports non-numeric tokens in ignored", () => {
    const { valid, ignored } = parseExcludedNumbers("3, abc, 7");
    expect(valid).toEqual([3, 7]);
    expect(ignored).toEqual(["abc"]);
  });

  it("handles empty string", () => {
    const { valid, ignored } = parseExcludedNumbers("");
    expect(valid).toEqual([]);
    expect(ignored).toEqual([]);
  });

  it("handles whitespace-only string", () => {
    const { valid, ignored } = parseExcludedNumbers("   ,  , ");
    expect(valid).toEqual([]);
    expect(ignored).toEqual([]);
  });

  it("accepts boundary values 1 and 45", () => {
    const { valid, ignored } = parseExcludedNumbers("1, 45");
    expect(valid).toEqual([1, 45]);
    expect(ignored).toEqual([]);
  });
});
