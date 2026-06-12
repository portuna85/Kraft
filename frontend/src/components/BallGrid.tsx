import { getBallStyle } from '@/lib/ballColors'

interface Props {
  selected: number[]
  onToggle: (n: number) => void
  max?: number
}

export default function BallGrid({ selected, onToggle, max = 6 }: Props) {
  return (
    <div className="grid grid-cols-9 gap-1">
      {Array.from({ length: 45 }, (_, i) => i + 1).map((n) => {
        const { bg, text } = getBallStyle(n)
        const isSelected = selected.includes(n)
        const isDisabled = !isSelected && selected.length >= max
        return (
          <button
            key={n}
            onClick={() => !isDisabled && onToggle(n)}
            disabled={isDisabled}
            aria-pressed={isSelected}
            className={`w-8 h-8 rounded-full text-xs font-bold transition-all ${
              isSelected
                ? 'ring-2 ring-gold scale-110'
                : isDisabled
                ? 'opacity-30 cursor-not-allowed'
                : 'hover:scale-105'
            }`}
            style={{ background: bg, color: text }}
          >
            {n}
          </button>
        )
      })}
    </div>
  )
}
