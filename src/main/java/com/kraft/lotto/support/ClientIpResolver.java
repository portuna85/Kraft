package com.kraft.lotto.support;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.List;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /**
     * X-Forwarded-For 헤더를 오른쪽에서 왼쪽으로 순회해 신뢰 프록시를 제거한 뒤
     * 첫 번째 비신뢰 홉 IP를 반환한다. Caddy 가 append 방식으로 XFF 를 추가하더라도
     * 클라이언트가 헤더를 위조해 선두에 임의 IP 를 삽입하는 공격을 차단한다.
     */
    public static String resolve(HttpServletRequest request, List<String> trustedProxyCidrs) {
        if (!trustedProxyCidrs.isEmpty()) {
            String remoteAddr = request.getRemoteAddr();
            if (isTrustedProxy(remoteAddr, trustedProxyCidrs)) {
                String forwardedFor = request.getHeader("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isBlank()) {
                    String[] parts = forwardedFor.split(",");
                    // 오른쪽에서 신뢰 프록시 제거 → 남는 첫 비신뢰 홉 반환
                    for (int i = parts.length - 1; i >= 0; i--) {
                        String candidate = parts[i].trim();
                        if (!candidate.isBlank() && !isTrustedProxy(candidate, trustedProxyCidrs)) {
                            return candidate;
                        }
                    }
                    // 모두 신뢰 프록시인 경우 — 가장 왼쪽 비공백 IP 반환
                    for (String part : parts) {
                        String candidate = part.trim();
                        if (!candidate.isBlank()) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }

    private static boolean isTrustedProxy(String remoteAddr, List<String> trustedProxyCidrs) {
        try {
            InetAddress remote = InetAddress.getByName(remoteAddr);
            byte[] candidate = remote.getAddress();
            for (String cidr : trustedProxyCidrs) {
                try {
                    if (IpRange.parse(cidr).matches(candidate)) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (java.net.UnknownHostException ignored) {
        }
        return false;
    }
}
