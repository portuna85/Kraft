package com.kraft.lotto.support;

import java.net.InetAddress;

record IpRange(byte[] baseAddress, int prefixLength) {

    static IpRange parse(String expression) throws Exception {
        String[] parts = expression.split("/", 2);
        InetAddress base = InetAddress.getByName(parts[0]);
        byte[] baseAddress = base.getAddress();
        int maxBits = baseAddress.length * 8;
        int prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : maxBits;
        if (prefix < 0 || prefix > maxBits) {
            throw new IllegalArgumentException("Invalid prefix length: " + prefix);
        }
        return new IpRange(baseAddress, prefix);
    }

    boolean matches(byte[] candidate) {
        if (candidate.length != baseAddress.length) {
            return false;
        }
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (candidate[i] != baseAddress[i]) {
                return false;
            }
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xFF << (8 - remainingBits);
        return (candidate[fullBytes] & mask) == (baseAddress[fullBytes] & mask);
    }
}
