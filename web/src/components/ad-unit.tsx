"use client";

import { useEffect, useRef } from "react";

type AdUnitProps = {
  unit: string;
  width: number;
  height: number;
  label?: string;
};

export function AdUnit({ unit, width, height, label = "광고" }: AdUnitProps) {
  const initialized = useRef(false);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;

    if (!document.querySelector('script[src*="daumcdn.net/kas"]')) {
      const script = document.createElement("script");
      script.async = true;
      script.src = "https://t1.daumcdn.net/kas/static/ba.min.js";
      document.head.appendChild(script);
    }
  }, []);

  return (
    <div className="ad-unit" aria-label={label}>
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

const AD_ENV: Record<PageAdProps["slot"], string> = {
  "rounds-list": process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_ROUNDS_LIST ?? "",
  "rounds-detail": process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_ROUNDS_DETAIL ?? "",
  frequency: process.env.NEXT_PUBLIC_KAKAO_ADFIT_UNIT_FREQUENCY ?? "",
};

export function PageAd({ slot }: PageAdProps) {
  const unit = AD_ENV[slot];
  if (!unit) return null;
  return <AdUnit unit={unit} width={320} height={100} />;
}
