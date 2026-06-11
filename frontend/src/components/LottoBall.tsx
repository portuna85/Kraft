'use client'

// bg/text 쌍: WCAG AA (4.5:1 이상) 대비율 확보
const COLORS: Record<string, { bg: string; text: string }> = {
  yellow: { bg: '#F9A825', text: '#1A1A2E' },
  blue:   { bg: '#1565C0', text: '#FFFFFF' },
  red:    { bg: '#B71C1C', text: '#FFFFFF' },
  gray:   { bg: '#424242', text: '#FFFFFF' },
  green:  { bg: '#1B5E20', text: '#FFFFFF' },
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
        backgroundColor: ballColor(number).bg,
        color: ballColor(number).text,
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
