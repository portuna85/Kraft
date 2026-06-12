// bg: radial-gradient (하이라이트 → 기본색), text: WCAG AA 대비 확보
export const BALL_COLORS = {
  yellow: { bg: 'radial-gradient(circle at 35% 35%, #FFE082, #F9A825 70%)', text: '#1A1A2E' },
  blue:   { bg: 'radial-gradient(circle at 35% 35%, #64B5F6, #1565C0 70%)', text: '#FFFFFF' },
  red:    { bg: 'radial-gradient(circle at 35% 35%, #EF9A9A, #B71C1C 70%)', text: '#FFFFFF' },
  gray:   { bg: 'radial-gradient(circle at 35% 35%, #9E9E9E, #424242 70%)', text: '#FFFFFF' },
  green:  { bg: 'radial-gradient(circle at 35% 35%, #81C784, #1B5E20 70%)', text: '#FFFFFF' },
}

export function getBallStyle(n: number) {
  if (n <= 10) return BALL_COLORS.yellow
  if (n <= 20) return BALL_COLORS.blue
  if (n <= 30) return BALL_COLORS.red
  if (n <= 40) return BALL_COLORS.gray
  return BALL_COLORS.green
}
