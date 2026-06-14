export interface LottoValidationOk {
  ok: true;
  numbers: number[];
}

export interface LottoValidationFail {
  ok: false;
  message: string;
}

export type LottoValidationResult = LottoValidationOk | LottoValidationFail;

export function validateLottoNumbers(raw: (number | string)[]): LottoValidationResult {
  if (raw.length !== 6) {
    return { ok: false, message: `번호는 6개여야 합니다. (현재 ${raw.length}개)` };
  }

  const numbers: number[] = [];
  for (const v of raw) {
    const n = typeof v === "number" ? v : Number(v);
    if (!Number.isInteger(n)) {
      return { ok: false, message: `모든 번호는 정수여야 합니다: ${v}` };
    }
    if (n < 1 || n > 45) {
      return { ok: false, message: `번호는 1에서 45 사이여야 합니다: ${n}` };
    }
    numbers.push(n);
  }

  const unique = new Set(numbers);
  if (unique.size !== 6) {
    return { ok: false, message: "번호에 중복이 있습니다." };
  }

  return { ok: true, numbers };
}

export function parseExcludedNumbers(input: string): number[] {
  return input
    .split(",")
    .map((v) => Number(v.trim()))
    .filter((v) => Number.isInteger(v) && v >= 1 && v <= 45);
}
