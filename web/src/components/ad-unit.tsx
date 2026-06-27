"use client";

import { useEffect, useRef } from "react";

type AdUnitProps = {
  unit: string;
  width: number;
  height: number;
  label?: string;
  className?: string;
};

export function AdUnit({ unit, width, height, label = "광고", className }: AdUnitProps) {
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    if (!document.querySelector('script[src*="kakaocdn.net/kas"]')) {
      const script = document.createElement("script");
      script.async = true;
      script.src = "https://t1.kakaocdn.net/kas/static/ba.min.js";
      document.head.appendChild(script);
    }
  }, []);

  return (
    <div className={`ad-unit${className ? ` ${className}` : ""}`} aria-label={label}>
      <ins
        className="kakao_ad_area"
        data-ad-unit={unit}
        data-ad-width={String(width)}
        data-ad-height={String(height)}
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
