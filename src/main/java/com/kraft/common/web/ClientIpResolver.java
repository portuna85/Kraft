package com.kraft.common.web;

import com.kraft.common.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private final SecurityProperties securityProperties;

    public ClientIpResolver(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    public String resolve(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // XFF: 우→좌 순회로 신뢰 프록시를 건너뛰고 실제 클라이언트 IP 추출
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!isTrustedProxy(candidate)) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }

    public boolean isTrustedProxy(String ip) {
        try {
            // B-2: ofLiteral() 사용 — 호스트명 입력 시 즉시 거부 (DNS 조회 없음)
            InetAddress addr = InetAddress.ofLiteral(ip);
            return isInCidr(addr, securityProperties.trustedProxyCidr());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isInCidr(InetAddress addr, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress network = InetAddress.ofLiteral(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);
            byte[] addrBytes = addr.getAddress();
            byte[] networkBytes = network.getAddress();
            if (addrBytes.length != networkBytes.length) {
                return false;
            }
            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (addrBytes[i] != networkBytes[i]) {
                    return false;
                }
            }
            if (remainingBits > 0 && fullBytes < addrBytes.length) {
                int mask = 0xFF & (0xFF << (8 - remainingBits));
                return (addrBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
