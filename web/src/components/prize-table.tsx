import { formatCurrency } from "@/lib/format";
import { calcAfterTax } from "@/lib/tax";

type Props = {
  firstPrizeAmount: number;
  secondPrize: number;
};

function PrizeRow({ rank, prizeAmount }: { rank: string; prizeAmount: number }) {
  return (
    <tr>
      <td className="prize-table-rank">{rank} 당첨금</td>
      <td className="prize-table-amount">{formatCurrency(prizeAmount)}</td>
      <td className="prize-table-after-tax">
        <span className="prize-table-after-tax-label">예상 당첨금</span>
        <span className="prize-table-after-tax-value">{formatCurrency(calcAfterTax(prizeAmount))}</span>
      </td>
    </tr>
  );
}

export function PrizeTable({ firstPrizeAmount, secondPrize }: Props) {
  return (
    <div className="prize-table-wrap">
      <table className="prize-table">
        <tbody>
          <PrizeRow rank="1등" prizeAmount={firstPrizeAmount} />
          <PrizeRow rank="2등" prizeAmount={secondPrize} />
        </tbody>
      </table>
    </div>
  );
}
