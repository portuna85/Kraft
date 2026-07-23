"use client";

import { useEffect, useState } from "react";
import { getCommunitySession, loginUrl, logout, type CommunitySession } from "@/lib/community-client";

export function AccountMenu() {
  const [session, setSession] = useState<CommunitySession | null>(null);

  useEffect(() => {
    let cancelled = false;
    getCommunitySession()
      .then((result) => {
        if (!cancelled) setSession(result);
      })
      .catch(() => {
        if (!cancelled) setSession({ loggedIn: false, userId: null, nickname: null });
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (!session) {
    return null;
  }

  if (!session.loggedIn) {
    return (
      <div className="account-menu">
        {/* 좁은 화면에서는 두 provider 링크를 나란히 둘 자리가 없어 Google 로그인으로
            압축한다(NavLinks의 nav-desktop/nav-mobile 이중 렌더링과 같은 CSS 토글 방식). */}
        <a href={loginUrl("google")} className="account-login-link account-login-compact">
          로그인
        </a>
        <span className="account-login-full">
          <a href={loginUrl("google")} className="account-login-link">
            Google 로그인
          </a>
          <a href={loginUrl("naver")} className="account-login-link">
            Naver 로그인
          </a>
        </span>
      </div>
    );
  }

  const handleLogout = async () => {
    await logout();
    window.location.reload();
  };

  return (
    <div className="account-menu">
      <span className="account-nickname account-login-full">{session.nickname}님</span>
      <button type="button" className="account-logout-button" onClick={handleLogout}>
        로그아웃
      </button>
    </div>
  );
}
