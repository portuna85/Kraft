export function getDeviceToken(): string {
  if (typeof window === 'undefined') return ''
  let token = localStorage.getItem('deviceToken')
  if (!token) {
    token = crypto.randomUUID()
    localStorage.setItem('deviceToken', token)
  }
  return token
}
