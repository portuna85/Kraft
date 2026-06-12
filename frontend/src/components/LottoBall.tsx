'use client'

import { getBallStyle } from '@/lib/ballColors'

interface Props {
  number: number
  size?: 'sm' | 'md' | 'lg'
  bonus?: boolean
}

const SIZE = { sm: 28, md: 36, lg: 44 }

export default function LottoBall({ number, size = 'md', bonus = false }: Props) {
  const px = SIZE[size]
  const { bg, text } = getBallStyle(number)
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
