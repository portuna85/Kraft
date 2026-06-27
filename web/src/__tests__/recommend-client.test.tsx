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

describe("RecommendClient", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    global.fetch = mockFetch(EMPTY_RESULT);
  });

  it("renders the recommend form with the prize checkbox checked by default", async () => {
    render(<RecommendClient />);

    expect(await screen.findByRole("button")).toBeInTheDocument();
    expect(screen.getByRole("spinbutton")).toHaveValue(5);
    expect(screen.getByRole("textbox")).toHaveValue("");
    expect(screen.getByRole("checkbox")).toBeChecked();
  });

  it("requests recommendations with maximizePrize enabled on initial load", async () => {
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

  it("calls the recommend API and renders returned numbers", async () => {
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
      expect(screen.getByText(/1/)).toBeInTheDocument();
    });

    [1, 7, 15, 23, 38, 45].forEach((n) => {
      expect(screen.getByText(String(n))).toBeInTheDocument();
    });
  });

  it("shows an error message when the API returns a non-OK status", async () => {
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

  it("shows a fallback error when the API returns no message", async () => {
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

  it("warns about ignored excluded numbers", async () => {
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

  it("shows the saved state after saving a recommendation", async () => {
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

    const buttons = await screen.findAllByRole("button");
    const saveButton = buttons[1];
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(buttons[1]).toBeDisabled();
    });
  });
});
