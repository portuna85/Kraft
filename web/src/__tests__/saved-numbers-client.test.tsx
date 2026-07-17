import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { SavedNumbersClient } from "@/components/saved-numbers-client";

vi.mock("@/lib/device-token", () => ({
  getDeviceToken: () => "a".repeat(64),
}));

const SAVED_ITEM = {
  id: 1,
  numbers: [1, 2, 3, 4, 5, 6],
  label: null,
  source: "MANUAL",
  createdAt: "2026-01-01T00:00:00Z",
};

function mockFetch(handler: (url: string) => { status: number; body: unknown }) {
  return vi.fn().mockImplementation((url: string) => {
    const { status, body } = handler(url);
    return Promise.resolve({
      ok: status >= 200 && status < 300,
      status,
      json: () => Promise.resolve(body),
    });
  });
}

describe("저장 번호 화면", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("회차 선택 옵션은 최신 회차 포함 최근 20개로 제한된다", async () => {
    global.fetch = mockFetch((url) => {
      if (url.includes("/matches")) return { status: 200, body: [] };
      return { status: 200, body: [SAVED_ITEM] };
    });

    render(<SavedNumbersClient latestRound={1230} />);

    const select = await screen.findByLabelText("대조할 회차");
    const options = within(select).getAllByRole("option");
    // "latest" 옵션 1개 + 최근 20개 이하
    expect(options.length).toBeLessThanOrEqual(21);
  });

  it("대조 결과 조회가 실패해도 이전 결과를 유지하고 재시도 버튼을 보여준다", async () => {
    let matchCallCount = 0;
    global.fetch = mockFetch((url) => {
      if (url.includes("/matches")) {
        matchCallCount++;
        if (matchCallCount === 1) {
          return {
            status: 200,
            body: [
              {
                savedNumber: SAVED_ITEM,
                round: 1230,
                drawDate: "2026-01-01",
                drawNumbers: [1, 2, 3, 4, 5, 6],
                bonusNumber: 7,
                matchedCount: 6,
                bonusMatch: false,
                prizeTier: "1등",
              },
            ],
          };
        }
        return { status: 500, body: {} };
      }
      return { status: 200, body: [SAVED_ITEM] };
    });

    render(<SavedNumbersClient latestRound={1230} />);

    await waitFor(() => {
      expect(screen.getByText("1등")).toBeInTheDocument();
    });

    // 회차를 바꿔 매치 요청이 실패하게 만든다
    fireEvent.change(screen.getByLabelText("대조할 회차"), { target: { value: "1229" } });

    await waitFor(() => {
      expect(screen.getByText(/대조 결과를 불러오지 못했습니다/)).toBeInTheDocument();
    });

    // 이전 성공 결과(1등)는 그대로 남아 있어야 한다
    expect(screen.getByText("1등")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "다시 시도" })).toBeInTheDocument();
  });

  it("직접 입력한 회차를 적용하면 해당 회차로 대조를 요청한다", async () => {
    global.fetch = mockFetch((url) => {
      if (url.includes("/matches")) return { status: 200, body: [] };
      return { status: 200, body: [SAVED_ITEM] };
    });

    render(<SavedNumbersClient latestRound={1230} />);

    await screen.findByLabelText("대조할 회차");

    fireEvent.change(screen.getByLabelText("회차 직접 입력"), { target: { value: "500" } });
    fireEvent.click(screen.getByRole("button", { name: "적용" }));

    await waitFor(() => {
      expect(global.fetch).toHaveBeenLastCalledWith(
        "/api/v1/saved/matches?round=500",
        expect.anything(),
      );
    });
  });
});
