import { describe, expect, it, beforeEach, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { RecommendClient } from "@/components/recommend-client";

vi.mock("@/lib/device-token", () => ({
  getDeviceToken: () => "a".repeat(64),
}));

function mockFetch(body: unknown, status = 200) {
  return vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
  });
}

// 마운트 시 useEffect 자동 조회용 빈 응답
const EMPTY_RESULT = { recommendations: [] };

describe("RecommendClient", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    // 마운트 시 자동 조회가 즉시 완료되도록 기본 mock 설정
    global.fetch = mockFetch(EMPTY_RESULT);
  });

  it("renders the recommend form", async () => {
    render(<RecommendClient />);
    // 초기 자동 조회 완료 후 버튼이 "추천받기"로 돌아올 때까지 대기
    expect(await screen.findByRole("button", { name: "추천받기" })).toBeInTheDocument();
    expect(screen.getByLabelText(/조합 수/)).toBeInTheDocument();
    expect(screen.getByLabelText(/제외 번호/)).toBeInTheDocument();
  });

  it("calls the recommend API and renders returned numbers", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({  // 초기 자동 조회
        ok: true, status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({  // 수동 제출
        ok: true, status: 200,
        json: () => Promise.resolve({ recommendations: [[1, 7, 15, 23, 38, 45]] }),
      });

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button", { name: "추천받기" })).closest("form")!);

    await waitFor(() => {
      expect(screen.getByText("추천 1")).toBeInTheDocument();
    });
    [1, 7, 15, 23, 38, 45].forEach((n) => {
      expect(screen.getByText(String(n))).toBeInTheDocument();
    });
  });

  it("shows an error message when the API returns a non-OK status", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({  // 초기 자동 조회
        ok: true, status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({  // 수동 제출 (오류)
        ok: false, status: 500,
        json: () => Promise.resolve({ message: "서버 오류입니다." }),
      });

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button", { name: "추천받기" })).closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent("서버 오류입니다.");
    });
  });

  it("shows a fallback error when the API returns no message", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({  // 초기 자동 조회
        ok: true, status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({  // 수동 제출 (오류, 메시지 없음)
        ok: false, status: 503,
        json: () => Promise.resolve({}),
      });

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button", { name: "추천받기" })).closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent("추천 생성에 실패했습니다.");
    });
  });

  it("warns about ignored excluded numbers", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({  // 초기 자동 조회
        ok: true, status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({  // 수동 제출
        ok: true, status: 200,
        json: () => Promise.resolve({ recommendations: [[2, 8, 14, 20, 30, 40]] }),
      });

    render(<RecommendClient />);
    const btn = await screen.findByRole("button", { name: "추천받기" });
    fireEvent.change(screen.getByLabelText(/제외 번호/), { target: { value: "3, abc, 99" } });
    fireEvent.submit(btn.closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent("무시된 입력값");
    });
  });

  it("shows 저장됨 after saving a recommendation", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({  // 초기 자동 조회
        ok: true, status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({  // 수동 제출
        ok: true, status: 200,
        json: () => Promise.resolve({ recommendations: [[1, 7, 15, 23, 38, 45]] }),
      })
      .mockResolvedValueOnce({  // 저장
        ok: true, status: 201,
        json: () => Promise.resolve({ created: true }),
      });

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button", { name: "추천받기" })).closest("form")!);

    await waitFor(() => expect(screen.getByText("저장")).toBeInTheDocument());
    fireEvent.click(screen.getByText("저장"));

    await waitFor(() => expect(screen.getByText("저장됨")).toBeInTheDocument());
  });
});
