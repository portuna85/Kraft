import { describe, expect, it } from "vitest";
import { ballColorClass } from "@/lib/ball-color";

describe("ballColorClass", () => {
  it("returns empty string for 1-10 (yellow/default)", () => {
    expect(ballColorClass(1)).toBe("");
    expect(ballColorClass(10)).toBe("");
  });

  it("returns ball-blue for 11-20", () => {
    expect(ballColorClass(11)).toBe("ball-blue");
    expect(ballColorClass(20)).toBe("ball-blue");
  });

  it("returns ball-red for 21-30", () => {
    expect(ballColorClass(21)).toBe("ball-red");
    expect(ballColorClass(30)).toBe("ball-red");
  });

  it("returns ball-gray for 31-40", () => {
    expect(ballColorClass(31)).toBe("ball-gray");
    expect(ballColorClass(40)).toBe("ball-gray");
  });

  it("returns ball-green for 41-45", () => {
    expect(ballColorClass(41)).toBe("ball-green");
    expect(ballColorClass(45)).toBe("ball-green");
  });
});
