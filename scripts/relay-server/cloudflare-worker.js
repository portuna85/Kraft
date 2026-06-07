/**
 * 당첨 판매점 릴레이 — Cloudflare Worker
 *
 * 배포:
 *   1. https://workers.cloudflare.com 에서 워커 생성
 *   2. 이 파일 내용을 붙여넣고 저장/배포
 *   3. 생성된 URL(예: https://relay.xxx.workers.dev)을
 *      kraft-server .env 에 KRAFT_API_STORE_RELAY_URL=<URL> 로 설정
 *
 * 선택: Workers 대시보드 > Settings > Variables 에서
 *   API_TOKEN 환경변수를 설정하면 ?token=값 인증이 활성화됩니다.
 */

const DH_STORE_URL   = "https://www.dhlottery.co.kr/store.do";
const DH_SESSION_URL = "https://www.dhlottery.co.kr/gameResult.do?method=byWin";

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/health") {
      return Response.json({ status: "ok" });
    }

    // 토큰 인증 (API_TOKEN 환경변수 설정 시)
    if (env.API_TOKEN) {
      if (url.searchParams.get("token") !== env.API_TOKEN) {
        return Response.json({ error: "Unauthorized" }, { status: 401 });
      }
    }

    const round = parseInt(url.searchParams.get("round") ?? "");
    const grade = parseInt(url.searchParams.get("grade") ?? "");
    if (!round || !grade || (grade !== 1 && grade !== 2)) {
      return Response.json({ error: "round, grade(1 or 2) 파라미터 필수" }, { status: 400 });
    }

    const headers = {
      "User-Agent":       "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
      "Accept":           "application/json, text/javascript, */*; q=0.01",
      "X-Requested-With": "XMLHttpRequest",
      "Referer":          `${DH_SESSION_URL}&drwNoSelect=${round}`,
    };

    // 세션 수립
    try {
      await fetch(`${DH_SESSION_URL}&drwNoSelect=${round}`, { headers });
    } catch (_) { /* 실패해도 진행 */ }

    // 판매점 데이터 요청
    const apiUrl = `${DH_STORE_URL}?method=searchStoreOfDraw&drwNo=${round}&winGrade=${grade}`;
    let resp;
    try {
      resp = await fetch(apiUrl, { headers });
    } catch (e) {
      return Response.json({ error: String(e) }, { status: 502 });
    }

    const text = await resp.text();
    if (text.trimStart().startsWith("<")) {
      return Response.json(
        { error: "dhlottery returned HTML — session failed or waiting room active" },
        { status: 502 }
      );
    }

    let data;
    try {
      data = JSON.parse(text);
    } catch (_) {
      return Response.json({ error: "JSON parse failed" }, { status: 502 });
    }

    const stores = (data.arrWinInfo ?? [])
      .filter(item => item.BPLC_NM?.trim())
      .map(item => ({
        name:     item.BPLC_NM.trim(),
        address:  (item.BPLC_ADRS ?? "").trim(),
        winCount: parseInt(item.WIN_CNT ?? "1") || 1,
      }));

    return Response.json({ stores });
  },
};
