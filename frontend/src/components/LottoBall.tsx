'use client'

const COLORS: Record<string, string> = {
  yellow: '#F9A825',
  blue: '#42A5F5',
  red: '#EF5350',
  gray: '#757575',
  green: '#66BB6A',
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
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        width: px,
        height: px,
        borderRadius: '50%',
        backgroundColor: ballColor(number),
        color: '#fff',
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
