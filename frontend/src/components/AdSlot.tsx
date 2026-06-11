'use client'

import { useEffect, useRef } from 'react'

interface Props {
  slotId: string
}

declare global {
  interface Window {
    adsbygoogle: unknown[]
  }
}

export default function AdSlot({ slotId }: Props) {
  const clientId = process.env.NEXT_PUBLIC_ADSENSE_CLIENT_ID
  const pushed = useRef(false)

  useEffect(() => {
    if (!clientId || pushed.current) return
    pushed.current = true
    try {
      ;(window.adsbygoogle = window.adsbygoogle || []).push({})
    } catch (_) {}
  }, [clientId])

  if (!clientId) return null

  return (
    <div className="ad-slot">
      <ins
        className="adsbygoogle"
        style={{ display: 'block' }}
        data-ad-client={clientId}
        data-ad-slot={slotId}
        data-ad-format="auto"
        data-full-width-responsive="true"
      />
    </div>
  )
}
