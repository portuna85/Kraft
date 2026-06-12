// 1~45 번호 선택 그리드 — analysis, companion 페이지 공유
const GRADIENTS: Record<string, { bg: string; text: string }> = {
  yellow: { bg: 'radial-gradient(circle at 35% 35%, #FFE082, #F9A825 70%)', text: '#1A1A2E' },
  blue:   { bg: 'radial-gradient(circle at 35% 35%, #64B5F6, #1565C0 70%)', text: '#FFFFFF' },
  red:    { bg: 'radial-gradient(circle at 35% 35%, #EF9A9A, #B71C1C 70%)', text: '#FFFFFF' },
  gray:   { bg: 'radial-gradient(circle at 35% 35%, #9E9E9E, #424242 70%)', text: '#FFFFFF' },
  green:  { bg: 'radial-gradient(circle at 35% 35%, #81C784, #1B5E20 70%)', text: '#FFFFFF' },
}

function ballStyle(n: number) {
  if (n <= 10) return GRADIENTS.yellow
  if (n <= 20) return GRADIENTS.blue
  if (n <= 30) return GRADIENTS.red
  if (n <= 40) return GRADIENTS.gray
  return GRADIENTS.green
}

interface Props {
  selected: number[]
  onToggle: (n: number) => void
  max?: number
}

export default function BallGrid({ selected, onToggle, max = 6 }: Props) {
  return (
    <div className="grid grid-cols-9 gap-1">
      {Array.from({ length: 45 }, (_, i) => i + 1).map((n) => {
        const { bg, text } = ballStyle(n)
        const isSelected = selected.includes(n)
        const isDisabled = !isSelected && selected.length >= max
        return (
          <button
            key={n}
            onClick={() => !isDisabled && onToggle(n)}
            disabled={isDisabled}
            className={`aspect-square rounded-full text-xs font-bold transition-all ${
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
