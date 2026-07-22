import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { AnalysisClient } from "@/components/analysis-client";

describe("번호 분석 입력 검증 (F4: 공통 validator 재사용)", () => {
  it("정수가 아닌 값(예: 3x)이 섞이면 오류를 보여준다", () => {
    render(<AnalysisClient />);

    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "3x, 11, 19, 28, 34, 42" },
    });
    fireEvent.click(screen.getByRole("button", { name: "분석하기" }));

    expect(screen.getByRole("alert")).toHaveTextContent("정수여야 합니다");
  });

  it("6개 미만이면 오류를 보여준다", () => {
    render(<AnalysisClient />);

    fireEvent.change(screen.getByRole("textbox"), { target: { value: "3, 11, 19" } });
    fireEvent.click(screen.getByRole("button", { name: "분석하기" }));

    expect(screen.getByRole("alert")).toHaveTextContent("6개여야 합니다");
  });

  it("유효한 번호 6개를 입력하면 분석 결과를 보여준다", () => {
    render(<AnalysisClient />);

    fireEvent.change(screen.getByRole("textbox"), {
      target: { value: "3, 11, 19, 28, 34, 42" },
    });
    fireEvent.click(screen.getByRole("button", { name: "분석하기" }));

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });
});
