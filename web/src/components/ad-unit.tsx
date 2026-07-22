"use client";

import { useEffect, useState } from "react";
import Script from "next/script";
import { useMediaQuery } from "@/lib/use-media-query";

// globals.css의 데스크톱 브레이크포인트(min-width: 1024px)와 동일하게 맞춘다.
const DESKTOP_QUERY = "(min-width: 1024px)";

type AdUnitProps = {
  unit: string;
  width: number;
  height: number;
  label?: string;
  className?: string;
};

export function AdUnit({ unit, width, height, label = "광고", className }: AdUnitProps) {
  return (
    <div
      className={`ad-unit${className ? ` ${className}` : ""}`}
      role="complementary"
      aria-label={label}
      style={{ minWidth: width, minHeight: height }}
    >
      <ins
        className="kakao_ad_area"
        data-ad-unit={unit}
        data-ad-width={String(width)}
        data-ad-height={String(height)}
      />
      {/* id로 동일 src의 중복 로드를 Next가 자동으로 걸러낸다 — 한 페이지에 AdUnit이
          여러 개(모바일+데스크톱) 있어도 SDK 스크립트는 한 번만 삽입된다.
          lazyOnload: 페이지가 상호작용 가능해진 뒤 로드해 초기 렌더 성능에 영향 없음. */}
      <Script
        id="kakao-adfit-sdk"
        src="https://t1.kakaocdn.net/kas/static/ba.min.js"
        strategy="lazyOnload"
        onError={() => console.error("카카오 애드핏 SDK 로드 실패")}
      />
    </div>
  );
}

type PageAdProps = {
  slot: "frequency";
};

const AD_MOBILE: Record<PageAdProps["slot"], string> = {
  frequency: process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_FREQUENCY ?? "",
};

const AD_DESKTOP: Record<PageAdProps["slot"], string> = {
  frequency: process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_FREQUENCY_DESKTOP ?? "",
};

export function PageAd({ slot }: PageAdProps) {
  const mobile = AD_MOBILE[slot];
  const desktop = AD_DESKTOP[slot];
  const isDesktop = useMediaQuery(DESKTOP_QUERY);
  if (!mobile && !desktop) return null;

  // 뷰포트에 맞는 광고 하나만 mount한다 — 둘 다 mount하고 CSS로 숨기면 보이지 않는
  // 쪽도 SDK 요청이 발생한다.
  if (isDesktop) {
    return desktop ? <AdUnit unit={desktop} width={728} height={90} className="ad-desktop" /> : null;
  }
  return mobile ? <AdUnit unit={mobile} width={320} height={100} className="ad-mobile" /> : null;
}

type AdSenseUnitProps = {
  slot: string;
  width: number;
  height: number;
  label?: string;
  className?: string;
};

// F3: slot이 비어 있으면(애드센스 계정 승인 전 등) 기본적으로 아무것도 렌더하지 않는다
// — 빈 자리를 예약하면 실제로 채워지지 않는 열이 운영 화면에 남는다. 로컬에서 CLS 없이
// 사이드바/레이아웃을 미리 검증하고 싶을 때만 NEXT_PUBLIC_ADSENSE_RESERVE_PLACEHOLDER=true로
// 켠다(운영 기본값은 off).
const RESERVE_PLACEHOLDER = process.env.NEXT_PUBLIC_ADSENSE_RESERVE_PLACEHOLDER === "true";

export function AdSenseUnit({ slot, width, height, label = "광고", className }: AdSenseUnitProps) {
  const clientId = process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID ?? "";
  const enabled = Boolean(clientId && slot);

  useEffect(() => {
    if (!enabled) return;
    try {
      // @ts-expect-error adsbygoogle 전역
      (window.adsbygoogle = window.adsbygoogle || []).push({});
    } catch (e) {
      console.error("[AdSenseUnit] adsbygoogle push 실패", e);
    }
  }, [enabled]);

  if (!enabled && !RESERVE_PLACEHOLDER) return null;

  return (
    <div
      className={`ad-unit${className ? ` ${className}` : ""}`}
      role="complementary"
      aria-label={label}
      style={{ minWidth: width, minHeight: height }}
    >
      {enabled ? (
        <>
          <ins
            className="adsbygoogle"
            style={{ display: "inline-block", width, height }}
            data-ad-client={clientId}
            data-ad-slot={slot}
          />
          <Script
            id="adsbygoogle-sdk"
            src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${clientId}`}
            strategy="lazyOnload"
            crossOrigin="anonymous"
            onError={() => console.error("애드센스 SDK 로드 실패")}
          />
        </>
      ) : null}
    </div>
  );
}

const IN_ARTICLE_ADSENSE_MOBILE_ENV: Record<PageAdProps["slot"], string | undefined> = {
  frequency: process.env.NEXT_PUBLIC_ADSENSE_UNIT_FREQUENCY_MOBILE,
};

const IN_ARTICLE_ADSENSE_DESKTOP_ENV: Record<PageAdProps["slot"], string | undefined> = {
  frequency: process.env.NEXT_PUBLIC_ADSENSE_UNIT_FREQUENCY,
};

// F4: 같은 슬롯에 애드핏(PageAd)과 애드센스(AdSenseUnit)를 동시에 넣으면 페이지 무게·
// 광고 밀도가 두 배가 된다. NEXT_PUBLIC_AD_NETWORK로 슬롯당 한 네트워크만 렌더한다
// (기본값 "adfit" — 애드센스 승인 전까지의 기존 운영 상태를 그대로 유지).
export function InArticleAd({ slot }: PageAdProps) {
  const network = process.env.NEXT_PUBLIC_AD_NETWORK === "adsense" ? "adsense" : "adfit";
  const isDesktop = useMediaQuery(DESKTOP_QUERY);

  if (network === "adsense") {
    return isDesktop ? (
      <AdSenseUnit
        slot={IN_ARTICLE_ADSENSE_DESKTOP_ENV[slot] ?? ""}
        width={728}
        height={90}
        className="ad-desktop"
      />
    ) : (
      <AdSenseUnit
        slot={IN_ARTICLE_ADSENSE_MOBILE_ENV[slot] ?? ""}
        width={300}
        height={250}
        className="ad-mobile"
      />
    );
  }

  return <PageAd slot={slot} />;
}

export function AdSenseSidebar({ slot }: { slot: string }) {
  return (
    <AdSenseUnit
      slot={slot}
      width={300}
      height={600}
      label="사이드바 광고"
      className="ad-sidebar"
    />
  );
}

export function StickyMobileAd({ unit }: { unit: string }) {
  const [closed, setClosed] = useState(false);

  if (!unit || closed) return null;

  return (
    <div className="ad-sticky-mobile">
      <button
        type="button"
        onClick={() => setClosed(true)}
        aria-label="광고 닫기"
        className="ad-sticky-mobile-close"
      >
        ✕
      </button>
      <AdUnit unit={unit} width={320} height={50} label="하단 고정 광고" />
    </div>
  );
}
