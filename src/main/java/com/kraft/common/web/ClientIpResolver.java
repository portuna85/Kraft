package com.kraft.common.web;

import com.kraft.common.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

// SpotBugs CT_CONSTRUCTOR_THROW: 생성자가 예외를 던지는 클래스는 서브클래싱을 통한
// finalizer 공격에 취약할 수 있어, final로 선언해 서브클래싱 자체를 차단한다.
@Component
public final class ClientIpResolver {

    private final List<ParsedCidr> trustedCidrs;

    public ClientIpResolver(SecurityProperties securityProperties) {
        this.trustedCidrs = parseCidrs(securityProperties.trustedProxyCidr());
    }

    public String resolve(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        if (!isTrustedProxy(remote)) {
            return remote;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String candidate = parts[i].trim();
                if (!isTrustedProxy(candidate)) {
                    return candidate;
                }
            }
        }
        return remote;
    }

    public boolean isTrustedProxy(String ip) {
        try {
            InetAddress addr = InetAddress.ofLiteral(ip);
            for (ParsedCidr cidr : trustedCidrs) {
                if (isInCidr(addr, cidr)) {
                    return true;
                }
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isInCidr(InetAddress addr, ParsedCidr cidr) {
        byte[] addrBytes = addr.getAddress();
        byte[] networkBytes = cidr.network().getAddress();
        if (addrBytes.length != networkBytes.length) {
            return false;
        }
        int fullBytes = cidr.prefixLength() / 8;
        int remainingBits = cidr.prefixLength() % 8;
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
    }

    /**
     * CIDR 목록을 기동 시 1회만 파싱한다. 매 요청마다 문자열을 split·parse하던 것을 없애고,
     * 잘못된 CIDR(오타 등)을 조용히 무시해 trusted-proxy 판정이 은근히 꺼진 채로 기동되는
     * 대신 즉시 기동을 실패시켜 배포 시점에 드러나게 한다(ProdEnvironmentValidator와 동일한
     * fail-fast 원칙).
     */
    private static List<ParsedCidr> parseCidrs(String raw) {
        List<ParsedCidr> result = new ArrayList<>();
        for (String entry : raw.split(",")) {
            String cidr = entry.trim();
            if (cidr.isEmpty()) {
                continue;
            }
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                throw new IllegalStateException("잘못된 CIDR 형식입니다(네트워크/프리픽스 형태 아님): " + cidr);
            }
            InetAddress network;
            try {
                network = InetAddress.ofLiteral(parts[0]);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("잘못된 CIDR 네트워크 주소입니다: " + cidr, e);
            }
            int prefixLength;
            try {
                prefixLength = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("잘못된 CIDR 프리픽스 길이입니다: " + cidr, e);
            }
            int maxPrefix = network.getAddress().length * 8; // IPv4=32, IPv6=128
            if (prefixLength < 0 || prefixLength > maxPrefix) {
                throw new IllegalStateException(
                        "CIDR 프리픽스 길이가 범위(0~" + maxPrefix + ")를 벗어났습니다: " + cidr);
            }
            result.add(new ParsedCidr(network, prefixLength));
        }
        return List.copyOf(result);
    }

    private record ParsedCidr(InetAddress network, int prefixLength) {
    }
}
