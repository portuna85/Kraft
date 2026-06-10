package com.kraft.lotto.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetAddress;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("아이피 대역 파서/매처 테스트")
class IpRangeTest {

    @Test
    @DisplayName("프리픽스가 없으면 주소 길이 전체 비트로 처리한다")
    void parseWithoutPrefixUsesFullBits() throws Exception {
        IpRange range = IpRange.parse("127.0.0.1");

        assertThat(range.prefixLength()).isEqualTo(32);
        assertThat(range.matches(InetAddress.getByName("127.0.0.1").getAddress())).isTrue();
        assertThat(range.matches(InetAddress.getByName("127.0.0.2").getAddress())).isFalse();
    }

    @Test
    @DisplayName("잘못된 프리픽스 길이는 예외가 발생한다")
    void parseRejectsInvalidPrefix() {
        assertThatThrownBy(() -> IpRange.parse("10.0.0.0/-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prefix length");
        assertThatThrownBy(() -> IpRange.parse("10.0.0.0/33"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid prefix length");
    }

    @Test
    @DisplayName("사이더 매칭은 바이트 길이, 바이트 비교, 잔여 비트 비교를 적용한다")
    void matchesWithByteAndBitChecks() throws Exception {
        IpRange range = IpRange.parse("192.168.10.0/24");

        assertThat(range.matches(InetAddress.getByName("192.168.10.55").getAddress())).isTrue();
        assertThat(range.matches(InetAddress.getByName("192.168.11.1").getAddress())).isFalse();
        assertThat(range.matches(InetAddress.getByName("::1").getAddress())).isFalse();
    }

    @Test
    @DisplayName("잔여 비트가 있는 프리픽스도 마스크 기준으로 매칭한다")
    void matchesWithPartialByteMask() throws Exception {
        IpRange range = IpRange.parse("10.0.0.0/13");

        assertThat(range.matches(InetAddress.getByName("10.3.255.10").getAddress())).isTrue();
        assertThat(range.matches(InetAddress.getByName("10.8.0.1").getAddress())).isFalse();
    }
}
