"use client";

import { useEffect, useState } from "react";
import Script from "next/script";

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
  if (!mobile && !desktop) return null;
  return (
    <>
      {mobile && <AdUnit unit={mobile} width={320} height={100} className="ad-mobile" />}
      {desktop && <AdUnit unit={desktop} width={728} height={90} className="ad-desktop" />}
    </>
  );
}

type AdSenseUnitProps = {
  slot: string;
  width: number;
  height: number;
  label?: string;
  className?: string;
};

// 애드핏 PageAd와 달리 slot이 비어 있어도 null을 반환하지 않고 자리만 예약한 플레이스홀더를
// 렌더한다. 애드센스 계정 승인 전이라 실 slot ID가 없는 기간에도 사이드바/레이아웃을
// 로컬에서 CLS 없이 검증할 수 있어야 하기 때문이다.
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

  if (network === "adsense") {
    return (
      <>
        <AdSenseUnit
          slot={IN_ARTICLE_ADSENSE_MOBILE_ENV[slot] ?? ""}
          width={300}
          height={250}
          className="ad-mobile"
        />
        <AdSenseUnit
          slot={IN_ARTICLE_ADSENSE_DESKTOP_ENV[slot] ?? ""}
          width={728}
          height={90}
          className="ad-desktop"
        />
      </>
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
