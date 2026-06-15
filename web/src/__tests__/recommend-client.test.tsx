import { describe, expect, it, beforeEach, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { RecommendClient } from "@/components/recommend-client";

// device-token uses crypto.randomUUID — provide a stable stub
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

describe("RecommendClient", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("renders the recommend form", () => {
    render(<RecommendClient />);
    expect(screen.getByText("추천받기")).toBeInTheDocument();
    expect(screen.getByLabelText(/조합 수/)).toBeInTheDocument();
    expect(screen.getByLabelText(/제외 번호/)).toBeInTheDocument();
  });

  it("calls the recommend API and renders returned numbers", async () => {
    global.fetch = mockFetch({ recommendations: [[1, 7, 15, 23, 38, 45]] });

    render(<RecommendClient />);
    fireEvent.submit(screen.getByRole("button", { name: "추천받기" }).closest("form")!);

    await waitFor(() => {
      expect(screen.getByText("추천 1")).toBeInTheDocument();
    });
    // Each of the 6 returned numbers should appear in the DOM
    [1, 7, 15, 23, 38, 45].forEach((n) => {
      expect(screen.getByText(String(n))).toBeInTheDocument();
    });
  });

  it("shows an error message when the API returns a non-OK status", async () => {
    global.fetch = mockFetch({ message: "서버 오류입니다." }, 500);

    render(<RecommendClient />);
    fireEvent.submit(screen.getByRole("button", { name: "추천받기" }).closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent("서버 오류입니다.");
    });
  });

  it("shows a fallback error when the API returns no message", async () => {
    global.fetch = mockFetch({}, 503);

    render(<RecommendClient />);
    fireEvent.submit(screen.getByRole("button", { name: "추천받기" }).closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent("추천 생성에 실패했습니다.");
    });
  });

  it("warns about ignored excluded numbers", async () => {
    global.fetch = mockFetch({ recommendations: [[2, 8, 14, 20, 30, 40]] });

    render(<RecommendClient />);
    fireEvent.change(screen.getByLabelText(/제외 번호/), { target: { value: "3, abc, 99" } });
    fireEvent.submit(screen.getByRole("button", { name: "추천받기" }).closest("form")!);

    await waitFor(() => {
      expect(screen.getByRole("status")).toHaveTextContent("무시된 입력값");
    });
  });

  it("shows 저장됨 after saving a recommendation", async () => {
    global.fetch = vi
      .fn()
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
    fireEvent.submit(screen.getByRole("button", { name: "추천받기" }).closest("form")!);

    await waitFor(() => expect(screen.getByText("저장")).toBeInTheDocument());
    fireEvent.click(screen.getByText("저장"));

    await waitFor(() => expect(screen.getByText("저장됨")).toBeInTheDocument());
  });
});
