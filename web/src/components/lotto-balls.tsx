import { ballColorClass } from "@/lib/ball-color";

type Props = {
  numbers: number[];
  bonusNumber?: number;
};

export function LottoBalls({ numbers, bonusNumber }: Props) {
  return (
    <div className="balls">
      {numbers.map((number) => (
        <span key={number} className={`ball ${ballColorClass(number)}`}>
          {number}
        </span>
      ))}
      {bonusNumber !== undefined ? (
        <>
          <span className="ball-separator">+</span>
          <span className="ball bonus-ball">{bonusNumber}</span>
        </>
      ) : null}
    </div>
  );
}
