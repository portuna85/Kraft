package com.kraft.lotto.feature.winningnumber.application;

import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

class DhLotteryTracerClient {

    private static final Logger log = LoggerFactory.getLogger(DhLotteryTracerClient.class);
    private static final String TRACER_BASE = "https://tracer.dhlottery.co.kr:48081/TRACERAPI";
    private static final String DH_HOST = "www.dhlottery.co.kr";
    private static final int DH_PORT = 443;
    private static final int MAX_WAIT_ATTEMPTS = 5;
    private static final long WAIT_MS = 2_000L;

    private final RestClient client;
    private final String serverIp;

    DhLotteryTracerClient(RestClient client, String serverIp) {
        this.client   = client;
        this.serverIp = serverIp != null ? serverIp : "";
    }

    static String generateWcCookie() {
        int rand = ThreadLocalRandom.current().nextInt(10_000, 100_000);
        return "_T_" + rand + "_WC";
    }

    boolean performHandshake(String pageUrl, String loginId, String userAgent) {
        String reject = checkBotIp(pageUrl);
        if (!"F".equals(reject)) {
            log.debug("tracer checkBotIp={}, proceeding without queue", reject);
            return true;
        }
        log.debug("tracer checkBotIp=F, entering queue pageUrl={}", pageUrl);
        return pollQueue(pageUrl, loginId, userAgent);
    }

    private String checkBotIp(String pageUrl) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("host", DH_HOST);
            form.add("ip", serverIp);
            form.add("port", String.valueOf(DH_PORT));
            form.add("pageUrl", pageUrl);

            String body = client.post()
                    .uri(TRACER_BASE + "/checkBotIp.do")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return body != null ? body.trim() : "E";
        } catch (Exception e) {
            log.debug("tracer checkBotIp error ({}), treating as E", e.getMessage());
            return "E";
        }
    }

    private boolean pollQueue(String pageUrl, String loginId, String userAgent) {
        for (int i = 0; i < MAX_WAIT_ATTEMPTS; i++) {
            String isWait = inputQueue(pageUrl, loginId, userAgent);
            log.debug("tracer inputQueue attempt={} isWait={}", i + 1, isWait);
            if ("F".equals(isWait) || "E".equals(isWait) || "NE".equals(isWait)) {
                return true;
            }
            if (!"W".equals(isWait)) {
                log.warn("tracer queue rejected: isWait={}", isWait);
                return false;
            }
            try {
                Thread.sleep(WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        log.warn("tracer queue wait timeout after {} attempts, proceeding anyway", MAX_WAIT_ATTEMPTS);
        return true;
    }

    private String inputQueue(String pageUrl, String loginId, String userAgent) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("host", DH_HOST);
            form.add("ip", serverIp);
            form.add("loginId", loginId);
            form.add("port", String.valueOf(DH_PORT));
            form.add("pageUrl", pageUrl);
            form.add("userAgent", userAgent);

            String body = client.post()
                    .uri(TRACER_BASE + "/inputQueue.do")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            if (body == null) {
                return "E";
            }
            String[] parts = body.split("\t");
            return parts.length > 7 ? parts[7].trim() : "E";
        } catch (Exception e) {
            log.debug("tracer inputQueue error ({}), treating as E", e.getMessage());
            return "E";
        }
    }
}
