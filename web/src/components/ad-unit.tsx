"use client";

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
  slot: "rounds-list" | "rounds-detail" | "frequency";
};

const AD_MOBILE: Record<PageAdProps["slot"], string> = {
  "rounds-list": process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_ROUNDS_LIST ?? "",
  "rounds-detail": process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_ROUNDS_DETAIL ?? "",
  frequency: process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_FREQUENCY ?? "",
};

const AD_DESKTOP: Record<PageAdProps["slot"], string> = {
  "rounds-list": process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_ROUNDS_LIST_DESKTOP ?? "",
  "rounds-detail": process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_ROUNDS_DETAIL_DESKTOP ?? "",
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
