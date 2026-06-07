#!/usr/bin/env python3
"""
당첨 판매점 릴레이 서버
동행복권 store API를 대신 호출해 kraft-server에 JSON으로 제공합니다.

사용법:
  pip install flask requests
  python relay.py

환경변수:
  PORT      리슨 포트 (기본 5000)
  HOST      리슨 주소 (기본 0.0.0.0)
  API_TOKEN 인증 토큰 — 설정 시 ?token=값 필수 (선택)
"""

import os
import requests
from flask import Flask, request, jsonify, abort

app = Flask(__name__)

DH_STORE_URL    = "https://www.dhlottery.co.kr/store.do"
DH_SESSION_URL  = "https://www.dhlottery.co.kr/gameResult.do?method=byWin"
API_TOKEN       = os.environ.get("API_TOKEN", "")

SESSION = requests.Session()
SESSION.headers.update({
    "User-Agent":      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Accept":          "application/json, text/javascript, */*; q=0.01",
    "X-Requested-With": "XMLHttpRequest",
    "Referer":         DH_SESSION_URL,
})


def _check_token():
    if not API_TOKEN:
        return
    if request.args.get("token") != API_TOKEN:
        abort(401)


def _establish_session(round_no: int):
    try:
        SESSION.get(f"{DH_SESSION_URL}&drwNoSelect={round_no}", timeout=5)
    except Exception:
        pass


@app.get("/")
def fetch_stores():
    _check_token()

    round_no = request.args.get("round", type=int)
    grade    = request.args.get("grade",  type=int)
    if not round_no or not grade:
        return jsonify({"error": "round, grade 파라미터 필수"}), 400
    if grade not in (1, 2):
        return jsonify({"error": "grade는 1 또는 2"}), 400

    _establish_session(round_no)

    try:
        resp = SESSION.get(DH_STORE_URL, params={
            "method":   "searchStoreOfDraw",
            "drwNo":    round_no,
            "winGrade": grade,
        }, timeout=10)
        resp.raise_for_status()
    except Exception as e:
        return jsonify({"error": str(e)}), 502

    text = resp.text.strip()
    if text.startswith("<"):
        return jsonify({"error": "dhlottery returned HTML — session failed"}), 502

    try:
        data = resp.json()
    except Exception:
        return jsonify({"error": "JSON parse failed"}), 502

    arr = data.get("arrWinInfo", [])
    stores = [
        {
            "name":     item.get("BPLC_NM", "").strip(),
            "address":  item.get("BPLC_ADRS", "").strip(),
            "winCount": int(item.get("WIN_CNT", 1)),
        }
        for item in arr
        if item.get("BPLC_NM", "").strip()
    ]

    return jsonify({"stores": stores})


@app.get("/health")
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    host = os.environ.get("HOST", "0.0.0.0")
    print(f"릴레이 서버 시작: http://{host}:{port}")
    app.run(host=host, port=port)
