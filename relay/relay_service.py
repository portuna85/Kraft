#!/usr/bin/env python3
"""
당첨 판매점 릴레이 서비스
- 이 서버는 dhlottery.co.kr에 차단되지 않은 별도 IP에 배포해야 합니다.
- 기동: pip install flask  &&  python relay_service.py
- Spring 앱 환경변수: KRAFT_API_STORE_RELAY_URL=http://<이 서버 IP>:8090/stores

응답 형식:
  GET /stores?round=1227&grade=1
  -> {"stores": [{"name": "판매점명", "address": "주소", "winCount": 1}, ...]}
"""

import json
import os
import urllib.request
import urllib.parse
import http.cookiejar
from flask import Flask, request, jsonify

app = Flask(__name__)

DH_SEED = "https://www.dhlottery.co.kr/gameResult.do?method=byWin"
DH_STORE = "https://www.dhlottery.co.kr/store.do"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
)
RELAY_SECRET = os.environ.get("RELAY_SECRET", "")


@app.route("/stores")
def stores():
    if RELAY_SECRET and request.headers.get("X-Relay-Secret") != RELAY_SECRET:
        return jsonify({"error": "unauthorized"}), 401

    round_no = request.args.get("round", type=int)
    grade = request.args.get("grade", type=int)
    if not round_no or not grade:
        return jsonify({"error": "round and grade are required"}), 400

    jar = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
    opener.addheaders = [("User-Agent", USER_AGENT)]

    seed_url = f"{DH_SEED}&drwNoSelect={round_no}"
    try:
        opener.open(seed_url, timeout=10)
    except Exception as e:
        app.logger.warning("session establishment failed: %s", e)

    store_url = (
        f"{DH_STORE}?method=searchStoreOfDraw"
        f"&drwNo={round_no}&winGrade={grade}"
    )
    req = urllib.request.Request(
        store_url,
        headers={
            "Referer": seed_url,
            "Accept": "application/json, text/javascript, */*; q=0.01",
            "X-Requested-With": "XMLHttpRequest",
            "User-Agent": USER_AGENT,
        },
    )
    try:
        resp = opener.open(req, timeout=10)
        body = resp.read().decode("utf-8")
    except Exception as e:
        app.logger.error("dhlottery fetch failed: %s", e)
        return jsonify({"stores": []}), 200

    if not body or body.strip().startswith("<"):
        app.logger.warning("dhlottery returned HTML: round=%s grade=%s", round_no, grade)
        return jsonify({"stores": []}), 200

    try:
        data = json.loads(body)
    except Exception as e:
        app.logger.error("json parse error: %s", e)
        return jsonify({"stores": []}), 200

    result = []
    for item in data.get("arrWinInfo", []):
        name = item.get("BPLC_NM", "").strip()
        if not name:
            continue
        try:
            win_count = int(item.get("WIN_CNT", "1"))
        except ValueError:
            win_count = 1
        result.append({
            "name": name,
            "address": item.get("BPLC_ADRS", "").strip(),
            "winCount": win_count,
        })

    app.logger.info("relay fetched: round=%s grade=%s count=%s", round_no, grade, len(result))
    return jsonify({"stores": result})


@app.route("/health")
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8090))
    app.run(host="0.0.0.0", port=port)
