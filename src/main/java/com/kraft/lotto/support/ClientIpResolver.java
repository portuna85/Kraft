package com.kraft.lotto.support;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.List;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /**
     * X-Forwarded-For を右から左へたどり、信頼プロキシを除去した最初の非信頼ホップを返す。
     * これにより Caddy 設定が append 方式に変わっても偽造 IP が先頭に来ない。
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
