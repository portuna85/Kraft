import { describe, expect, it } from "vitest";
import { analyzeNumbers } from "@/lib/analyze";

describe("번호 분석", () => {
  it("홀짝 개수를 올바르게 센다", () => {
    const result = analyzeNumbers([1, 2, 3, 4, 5, 6]);
    expect(result.oddCount).toBe(3);
    expect(result.evenCount).toBe(3);
  });

  it("22는 저번호, 23은 고번호로 분류한다", () => {
    const low = analyzeNumbers([1, 2, 3, 4, 5, 22]);
    expect(low.lowCount).toBe(6);
    expect(low.highCount).toBe(0);

    const high = analyzeNumbers([1, 2, 3, 4, 5, 23]);
    expect(high.lowCount).toBe(5);
    expect(high.highCount).toBe(1);
  });

  it("합계 구간 경계값(66,111,156,201)을 올바르게 분류한다", () => {
    // 각 케이스는 5개 고정 번호(1,2,3,4,5=15) + 6번째 번호로 목표 합계를 만든다.
    expect(analyzeNumbers([1, 2, 3, 4, 5, 50]).sumOfNumbers).toBe(65);
    expect(analyzeNumbers([1, 2, 3, 4, 5, 50]).sumBucket).toBe("21-65");

    expect(analyzeNumbers([1, 2, 3, 4, 5, 51]).sumOfNumbers).toBe(66);
    expect(analyzeNumbers([1, 2, 3, 4, 5, 51]).sumBucket).toBe("66-110");

    expect(analyzeNumbers([1, 2, 3, 4, 5, 96]).sumOfNumbers).toBe(111);
    expect(analyzeNumbers([1, 2, 3, 4, 5, 96]).sumBucket).toBe("111-155");

    expect(analyzeNumbers([1, 2, 3, 4, 5, 141]).sumOfNumbers).toBe(156);
    expect(analyzeNumbers([1, 2, 3, 4, 5, 141]).sumBucket).toBe("156-200");

    expect(analyzeNumbers([1, 2, 3, 4, 5, 186]).sumOfNumbers).toBe(201);
    expect(analyzeNumbers([1, 2, 3, 4, 5, 186]).sumBucket).toBe("201-255");
  });

  it("연속 번호 쌍을 여러 개/0개 모두 올바르게 센다", () => {
    expect(analyzeNumbers([1, 2, 3, 4, 5, 6]).consecutivePairCount).toBe(5);
    expect(analyzeNumbers([1, 5, 10, 15, 20, 25]).consecutivePairCount).toBe(0);
    expect(analyzeNumbers([1, 2, 10, 20, 21, 30]).consecutivePairCount).toBe(2);
  });

  it("번호 범위 구간 경계값(9/10,19/20,29/30,39/40)을 올바르게 분류한다", () => {
    const result = analyzeNumbers([9, 10, 19, 20, 40, 45]);
    const byLabel = Object.fromEntries(result.rangeDistribution.map((r) => [r.range, r.count]));
    expect(byLabel["1-9"]).toBe(1);
    expect(byLabel["10-19"]).toBe(2);
    expect(byLabel["20-29"]).toBe(1);
    expect(byLabel["30-39"]).toBe(0);
    expect(byLabel["40-45"]).toBe(2);
  });

  it("입력 배열을 변형하지 않고 정렬된 번호 목록을 반환한다", () => {
    const input = [6, 5, 4, 3, 2, 1];
    const result = analyzeNumbers(input);
    expect(input).toEqual([6, 5, 4, 3, 2, 1]);
    expect(result.numbers).toEqual([1, 2, 3, 4, 5, 6]);
  });

  it("백엔드 통계 캐시 서비스 픽스처와 동일한 결과를 낸다", () => {
    const result = analyzeNumbers([1, 2, 3, 4, 5, 6]);
    expect(result.oddCount).toBe(3);
    expect(result.evenCount).toBe(3);
    expect(result.sumOfNumbers).toBe(21);
    expect(result.sumBucket).toBe("21-65");
    expect(result.consecutivePairCount).toBe(5);
    expect(result.lowCount).toBe(6);
    expect(result.highCount).toBe(0);
  });

  // BE-14 골든 픽스처 — WinningStatisticsCacheServiceTest(Java)의 analyze_goldenFixture_*
  // 테스트와 입력·기댓값이 반드시 일치해야 한다. 두 언어의 독립 구현이 갈라지면 이 두
  // 테스트 세트 중 한쪽만 깨지므로 드리프트가 드러난다.
  it("골든 픽스처: 범위 분산 조합(9,10,19,20,40,45)이 백엔드 구현과 동일한 결과를 낸다", () => {
    const result = analyzeNumbers([9, 10, 19, 20, 40, 45]);

    expect(result.oddCount).toBe(3);
    expect(result.evenCount).toBe(3);
    expect(result.lowCount).toBe(4);
    expect(result.highCount).toBe(2);
    expect(result.sumOfNumbers).toBe(143);
    expect(result.sumBucket).toBe("111-155");
    expect(result.consecutivePairCount).toBe(2); // 9-10, 19-20
    expect(result.rangeDistribution).toEqual([
      { range: "1-9", count: 1 },
      { range: "10-19", count: 2 },
      { range: "20-29", count: 1 },
      { range: "30-39", count: 0 },
      { range: "40-45", count: 2 }
    ]);
  });

  it("골든 픽스처: 7의 배수 조합(7,14,21,28,35,42)이 백엔드 구현과 동일한 결과를 낸다", () => {
    const result = analyzeNumbers([7, 14, 21, 28, 35, 42]);

    expect(result.oddCount).toBe(3);
    expect(result.evenCount).toBe(3);
    expect(result.lowCount).toBe(3);
    expect(result.highCount).toBe(3);
    expect(result.sumOfNumbers).toBe(147);
    expect(result.sumBucket).toBe("111-155");
    expect(result.consecutivePairCount).toBe(0);
    expect(result.rangeDistribution).toEqual([
      { range: "1-9", count: 1 },
      { range: "10-19", count: 1 },
      { range: "20-29", count: 2 },
      { range: "30-39", count: 1 },
      { range: "40-45", count: 1 }
    ]);
  });
});
