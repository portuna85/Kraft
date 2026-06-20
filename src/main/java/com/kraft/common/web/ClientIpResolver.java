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
            for (String cidr : securityProperties.trustedProxyCidr().split(",")) {
                if (isInCidr(addr, cidr.trim())) {
                    return true;
                }
            }
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isInCidr(InetAddress addr, String cidr) {
        try {
            if (cidr.isBlank()) {
                return false;
            }
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
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
