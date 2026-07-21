import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { DataFreshnessNote } from "@/components/data-freshness-note";
import type { RoundFreshness } from "@/lib/api";

describe("데이터 최신성 안내", () => {
  it("freshness가 null이면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<DataFreshnessNote freshness={null} />);

    expect(container).toBeEmptyDOMElement();
  });

  it("최신 회차까지 반영됐으면 반영 완료 문구를 보여준다", () => {
    const freshness: RoundFreshness = {
      latestRound: 1200,
      latestDrawDate: "2026-07-18",
      fresh: true,
      checkedAt: "2026-07-21T00:00:00Z",
    };
    render(<DataFreshnessNote freshness={freshness} />);

    expect(screen.getByRole("status")).toHaveTextContent("1200회");
    expect(screen.getByRole("status")).toHaveTextContent("최신 회차까지 반영됨");
  });

  it("반영이 지연됐으면 지연 안내 문구를 보여준다", () => {
    const freshness: RoundFreshness = {
      latestRound: 1199,
      latestDrawDate: "2026-07-11",
      fresh: false,
      checkedAt: "2026-07-21T00:00:00Z",
    };
    render(<DataFreshnessNote freshness={freshness} />);

    expect(screen.getByRole("status")).toHaveTextContent("반영이 지연되고 있습니다");
  });
});
