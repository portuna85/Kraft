import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { AccountMenu } from "@/components/community/account-menu";

function mockFetch(body: unknown) {
  return vi.fn().mockResolvedValue({
    ok: true,
    status: 200,
    json: () => Promise.resolve(body),
  });
}

describe("커뮤니티 계정 메뉴", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  it("로그인하지 않은 경우 Google·Naver 로그인 링크를 보여준다", async () => {
    global.fetch = mockFetch({ loggedIn: false, userId: null, nickname: null });

    render(<AccountMenu />);

    expect(await screen.findByText("Google 로그인")).toBeInTheDocument();
    expect(screen.getByText("Naver 로그인")).toBeInTheDocument();
  });

  it("로그인한 경우 닉네임과 로그아웃 버튼을 보여준다", async () => {
    global.fetch = mockFetch({ loggedIn: true, userId: 1, nickname: "글쓴이" });

    render(<AccountMenu />);

    await waitFor(() => {
      expect(screen.getByText("글쓴이님")).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: "로그아웃" })).toBeInTheDocument();
  });

  it("세션 조회가 실패하면 로그인 링크로 안전하게 대체된다", async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error("network error"));

    render(<AccountMenu />);

    expect(await screen.findByText("Google 로그인")).toBeInTheDocument();
  });
});
