'use client'

// bg 는 radial-gradient (하이라이트 → 기본색), text 는 WCAG AA 대비 확보
const COLORS: Record<string, { bg: string; text: string }> = {
  yellow: { bg: 'radial-gradient(circle at 35% 35%, #FFE082, #F9A825 70%)', text: '#1A1A2E' },
  blue:   { bg: 'radial-gradient(circle at 35% 35%, #64B5F6, #1565C0 70%)', text: '#FFFFFF' },
  red:    { bg: 'radial-gradient(circle at 35% 35%, #EF9A9A, #B71C1C 70%)', text: '#FFFFFF' },
  gray:   { bg: 'radial-gradient(circle at 35% 35%, #9E9E9E, #424242 70%)', text: '#FFFFFF' },
  green:  { bg: 'radial-gradient(circle at 35% 35%, #81C784, #1B5E20 70%)', text: '#FFFFFF' },
}

function ballColor(n: number) {
  if (n <= 10) return COLORS.yellow
  if (n <= 20) return COLORS.blue
  if (n <= 30) return COLORS.red
  if (n <= 40) return COLORS.gray
  return COLORS.green
}

interface Props {
  number: number
  size?: 'sm' | 'md' | 'lg'
  bonus?: boolean
}

const SIZE = { sm: 28, md: 36, lg: 44 }

export default function LottoBall({ number, size = 'md', bonus = false }: Props) {
  const px = SIZE[size]
  const { bg, text } = ballColor(number)
  return (
    <span
      role="img"
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: px,
        height: px,
        borderRadius: '50%',
        background: bg,
        color: text,
        fontWeight: 700,
        fontSize: px * 0.38,
        outline: bonus ? '2px solid #FFC107' : undefined,
        flexShrink: 0,
      }}
      aria-label={`${number}${bonus ? ' (보너스)' : ''}`}
    >
      {number}
    </span>
  )
}
