import type { AnalysisResponse } from "@/lib/api";

type AnalysisResultProps = {
  analysis: AnalysisResponse;
  title: string;
};

export function AnalysisResult({ analysis, title }: AnalysisResultProps) {
  return (
    <div className="analysis-result">
      <h2 className="section-title">{title}</h2>

      <div className="result-grid">
        <div className="result-cell">
          <span className="result-label">홀수 / 짝수</span>
          <span className="result-value">{analysis.oddCount} / {analysis.evenCount}</span>
        </div>
        <div className="result-cell">
          <span className="result-label">저번호 / 고번호</span>
          <span className="result-value">{analysis.lowCount} / {analysis.highCount}</span>
        </div>
        <div className="result-cell">
          <span className="result-label">합계</span>
          <span className="result-value">{analysis.sumOfNumbers}</span>
          <span className="result-sub">{analysis.sumBucket} 구간</span>
        </div>
        <div className="result-cell">
          <span className="result-label">연속 번호</span>
          <span className="result-value">{analysis.consecutivePairCount}쌍</span>
        </div>
      </div>

      <div>
        <p className="section-title analysis-section-title">구간 분포</p>
        <ul className="range-dist-list">
          {analysis.rangeDistribution.map((range) => (
            <li key={range.range} className="range-dist-item">
              <span className="range-label">{range.range}</span>
              <div className="bar-track">
                <div
                  className="bar-fill"
                  style={{ width: `${Math.round((range.count / 6) * 100)}%` }}
                />
              </div>
              <span className="range-count">{range.count}</span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
