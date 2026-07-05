import { beforeEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
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

const EMPTY_RESULT = { recommendations: [] };

describe("번호 추천 화면", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    global.fetch = mockFetch(EMPTY_RESULT);
  });

  it("기본값으로 폼을 렌더링한다", async () => {
    render(<RecommendClient />);

    expect(await screen.findByRole("button")).toBeInTheDocument();
    expect(screen.getByRole("spinbutton")).toHaveValue(5);
    expect(screen.getByRole("textbox")).toHaveValue("");
    expect(screen.getByRole("checkbox")).toBeChecked();
  });

  it("최초 로드 시 추천을 요청한다", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve(EMPTY_RESULT),
    });

    render(<RecommendClient />);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledTimes(1);
    });

    expect(global.fetch).toHaveBeenCalledWith(
      "/api/v1/numbers/recommend",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          count: 5,
          excludedNumbers: [],
          maximizePrize: true,
        }),
      }),
    );
  });

  it("마운트 시 반환된 초기 추천을 렌더링한다", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ recommendations: [[4, 8, 12, 16, 20, 24]] }),
    });

    render(<RecommendClient />);

    await waitFor(() => {
      expect(screen.getAllByRole("button")).toHaveLength(2);
    });

    [4, 8, 12, 16, 20, 24].forEach((n) => {
      expect(screen.getByText(String(n))).toBeInTheDocument();
    });
  });

  it("초기 로드가 응답 오류로 실패하면 서버 메시지를 보여준다", async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 503,
      json: () => Promise.resolve({ message: "temporary outage" }),
    });

    render(<RecommendClient />);

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent("temporary outage");
    });
  });

  it("초기 로드가 예외를 던지면 폴백 메시지를 보여준다", async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error("network down"));

    render(<RecommendClient />);

    await waitFor(() => {
      expect(screen.getByRole("status").textContent).not.toBe("");
    });
  });

  it("번호 추천 요청을 보내고 반환된 숫자를 렌더링한다", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ recommendations: [[1, 7, 15, 23, 38, 45]] }),
      });

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button")).closest("form")!);

    await waitFor(() => {
      expect(screen.getAllByRole("button")).toHaveLength(2);
    });

    [1, 7, 15, 23, 38, 45].forEach((n) => {
      expect(screen.getByText(String(n))).toBeInTheDocument();
    });
  });

  it("서버가 비정상 상태를 반환하면 오류 메시지를 보여준다", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: () => Promise.resolve({ message: "server error" }),
      });

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button")).closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent("server error");
    });
  });

  it("서버가 메시지를 주지 않으면 폴백 오류를 보여준다", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({
        ok: false,
        status: 503,
        json: () => Promise.resolve({}),
      });

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button")).closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status").textContent).not.toBe("");
    });
  });

  it("변경된 추천 개수와 당첨금 최대화 값을 제출한다", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      });

    render(<RecommendClient />);
    const submitButton = await screen.findByRole("button");

    fireEvent.change(screen.getByRole("spinbutton"), { target: { value: "7" } });
    fireEvent.click(screen.getByRole("checkbox"));
    fireEvent.submit(submitButton.closest("form")!);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledTimes(2);
    });

    expect(global.fetch).toHaveBeenLastCalledWith(
      "/api/v1/numbers/recommend",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({
          count: 7,
          excludedNumbers: [],
          maximizePrize: false,
        }),
      }),
    );
  });

  it("추천 제출이 예외를 던지면 폴백 메시지를 보여준다", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockRejectedValueOnce(new Error("network down"));

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button")).closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status").textContent).not.toBe("");
    });
  });

  it("무시된 제외 숫자에 대해 경고한다", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ recommendations: [[2, 8, 14, 20, 30, 40]] }),
      });

    render(<RecommendClient />);
    const submitButton = await screen.findByRole("button");

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "3, abc, 99" } });
    fireEvent.submit(submitButton.closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status").textContent).toContain("abc");
    });
  });

  it("추천을 저장한 뒤 저장 완료 상태를 보여준다", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ recommendations: [[1, 7, 15, 23, 38, 45]] }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 201,
        json: () => Promise.resolve({ created: true }),
      });

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button")).closest("form")!);

    await waitFor(() => {
      expect(screen.getAllByRole("button").length).toBeGreaterThan(1);
    });

    const saveButton = screen.getAllByRole("button")[1];
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect((screen.getAllByRole("button"))[1]).toBeDisabled();
    });
  });

  it("저장이 실패하면 폴백 메시지를 보여준다", async () => {
    global.fetch = vi.fn()
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve(EMPTY_RESULT),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: () => Promise.resolve({ recommendations: [[1, 7, 15, 23, 38, 45]] }),
      })
      .mockRejectedValueOnce(new Error("save failed"));

    render(<RecommendClient />);
    fireEvent.submit((await screen.findByRole("button")).closest("form")!);

    await waitFor(() => {
      expect(screen.getAllByRole("button").length).toBeGreaterThan(1);
    });

    fireEvent.click(screen.getAllByRole("button")[1]);

    await waitFor(() => {
      expect(screen.getByRole("status").textContent).not.toBe("");
    });
  });
});
