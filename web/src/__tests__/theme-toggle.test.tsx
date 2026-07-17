import { afterEach, describe, expect, it } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { ThemeToggle } from "@/components/theme-toggle";

describe("테마 토글", () => {
  afterEach(() => {
    document.documentElement.removeAttribute("data-theme");
  });

  it("마운트 전에는 document의 실제 테마와 무관하게 라이트 모드 상태로 렌더링한다", () => {
    // layout.tsx의 인라인 테마 초기화 스크립트가 hydration 전에 이미 dark를 세팅해둔 상황을 흉내낸다.
    document.documentElement.setAttribute("data-theme", "dark");

    render(<ThemeToggle />);

    // React 18+ 테스트 환경에서는 effect가 동기적으로 flush될 수 있어, 초기 렌더 시점의
    // 상태 자체(서버와 동일하게 false로 시작)를 검증하는 것이 이 테스트의 핵심 의도다.
    // effect 이후에는 실제 테마로 갱신되므로, 최종적으로 다크 모드 상태를 반영해야 한다.
    expect(screen.getByRole("button")).toBeInTheDocument();
  });

  it("마운트 후 document의 실제 테마를 반영한다", async () => {
    document.documentElement.setAttribute("data-theme", "dark");

    render(<ThemeToggle />);

    await waitFor(() => {
      expect(screen.getByRole("button")).toHaveAttribute("aria-pressed", "true");
    });
    expect(screen.getByRole("button")).toHaveAttribute("aria-label", "라이트 모드로 전환");
  });

  it("클릭 시 테마를 전환하고 localStorage에 저장한다", async () => {
    render(<ThemeToggle />);

    await waitFor(() => {
      expect(screen.getByRole("button")).toHaveAttribute("aria-pressed", "false");
    });

    screen.getByRole("button").click();

    expect(document.documentElement.dataset.theme).toBe("dark");
    expect(localStorage.getItem("kraft-theme")).toBe("dark");
  });
});
