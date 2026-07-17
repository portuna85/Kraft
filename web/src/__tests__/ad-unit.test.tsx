import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { AdUnit, PageAd } from "@/components/ad-unit";

describe("광고 유닛", () => {
  it("전달받은 unit·width·height로 ins 태그를 렌더링하고 정확한 크기를 예약한다", () => {
    render(<AdUnit unit="DAN-abc123" width={320} height={100} />);

    const ins = document.querySelector("ins.kakao_ad_area");
    expect(ins).toBeInTheDocument();
    expect(ins).toHaveAttribute("data-ad-unit", "DAN-abc123");
    expect(ins).toHaveAttribute("data-ad-width", "320");
    expect(ins).toHaveAttribute("data-ad-height", "100");

    const container = screen.getByLabelText("광고");
    expect(container).toHaveStyle({ minWidth: "320px", minHeight: "100px" });
  });

  it("label을 지정하면 aria-label에 반영된다", () => {
    render(<AdUnit unit="DAN-abc123" width={320} height={100} label="회차 목록 광고" />);

    expect(screen.getByLabelText("회차 목록 광고")).toBeInTheDocument();
  });

  it("모바일·데스크톱 유닛이 모두 없으면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<PageAd slot="rounds-list" />);

    expect(container).toBeEmptyDOMElement();
  });
});
