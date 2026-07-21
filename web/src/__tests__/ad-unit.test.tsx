import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { AdSenseUnit, AdUnit, PageAd, StickyMobileAd } from "@/components/ad-unit";

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

describe("애드센스 유닛", () => {
  const originalClientId = process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID;

  afterEach(() => {
    process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID = originalClientId;
  });

  it("client ID와 slot이 모두 있으면 ins.adsbygoogle 태그를 올바른 속성으로 렌더링한다", () => {
    process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID = "ca-pub-1234567890123456";
    render(<AdSenseUnit slot="1111111111" width={728} height={90} />);

    const ins = document.querySelector("ins.adsbygoogle");
    expect(ins).toBeInTheDocument();
    expect(ins).toHaveAttribute("data-ad-client", "ca-pub-1234567890123456");
    expect(ins).toHaveAttribute("data-ad-slot", "1111111111");
  });

  it("slot이 비어있으면 ins를 렌더링하지 않되 지정한 크기의 플레이스홀더 자리는 예약한다", () => {
    process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID = "ca-pub-1234567890123456";
    render(<AdSenseUnit slot="" width={300} height={600} label="사이드바 광고" />);

    expect(document.querySelector("ins.adsbygoogle")).not.toBeInTheDocument();

    const placeholder = screen.getByLabelText("사이드바 광고");
    expect(placeholder).toHaveStyle({ minWidth: "300px", minHeight: "600px" });
  });

  it("client ID가 없으면 ins를 렌더링하지 않되 플레이스홀더 자리는 예약한다", () => {
    process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID = "";
    render(<AdSenseUnit slot="1111111111" width={728} height={90} />);

    expect(document.querySelector("ins.adsbygoogle")).not.toBeInTheDocument();
    expect(screen.getByLabelText("광고")).toHaveStyle({ minWidth: "728px", minHeight: "90px" });
  });
});

describe("본문 광고 (F4: 슬롯당 한 네트워크만)", () => {
  // 슬롯별 env 매핑이 모듈 로드 시점에 한 번 계산되므로(빌드타임 치환과 동일한 전제),
  // 값을 바꿔서 검증하려면 매번 모듈을 새로 import해야 한다.
  const originalEnv = { ...process.env };

  afterEach(() => {
    process.env = { ...originalEnv };
    vi.resetModules();
  });

  it("NEXT_PUBLIC_AD_NETWORK 미설정 시 애드핏만 렌더한다(기본값)", async () => {
    process.env = { ...originalEnv };
    delete process.env.NEXT_PUBLIC_AD_NETWORK;
    vi.resetModules();
    const { InArticleAd } = await import("@/components/ad-unit");

    render(<InArticleAd slot="rounds-list" />);

    expect(document.querySelector("ins.adsbygoogle")).not.toBeInTheDocument();
  });

  it("NEXT_PUBLIC_AD_NETWORK=adsense면 애드센스만 렌더한다", async () => {
    process.env = {
      ...originalEnv,
      NEXT_PUBLIC_AD_NETWORK: "adsense",
      NEXT_PUBLIC_ADSENSE_CLIENT_ID: "ca-pub-1234567890123456",
      NEXT_PUBLIC_ADSENSE_UNIT_ROUNDS_LIST_MOBILE: "2222222222",
    };
    vi.resetModules();
    const { InArticleAd } = await import("@/components/ad-unit");

    render(<InArticleAd slot="rounds-list" />);

    expect(document.querySelector("ins.kakao_ad_area")).not.toBeInTheDocument();
    expect(document.querySelector("ins.adsbygoogle")).toBeInTheDocument();
  });
});

describe("모바일 하단 고정 배너", () => {
  it("unit이 비어있으면 아무것도 렌더링하지 않는다", () => {
    const { container } = render(<StickyMobileAd unit="" />);

    expect(container).toBeEmptyDOMElement();
  });

  it("unit이 있으면 광고와 닫기 버튼을 렌더링한다", () => {
    render(<StickyMobileAd unit="DAN-sticky123" />);

    const ins = document.querySelector("ins.kakao_ad_area");
    expect(ins).toBeInTheDocument();
    expect(ins).toHaveAttribute("data-ad-unit", "DAN-sticky123");
    expect(screen.getByLabelText("광고 닫기")).toBeInTheDocument();
  });

  it("닫기 버튼을 클릭하면 배너가 사라진다", () => {
    const { container } = render(<StickyMobileAd unit="DAN-sticky123" />);

    fireEvent.click(screen.getByLabelText("광고 닫기"));

    expect(container).toBeEmptyDOMElement();
  });
});
