package com.kraft.lotto.web;

import com.kraft.lotto.feature.winningnumber.application.LottoFetchLogQueryService;
import com.kraft.lotto.feature.winningnumber.application.WinningNumberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class InfoPageController {

    private static final int CHANGE_LOG_LIMIT = 20;

    private static final String FAQ_JSON_LD = """
            {
              "@context": "https://schema.org",
              "@type": "FAQPage",
              "mainEntity": [
                {
                  "@type": "Question",
                  "name": "이 사이트가 당첨번호를 예측하나요?",
                  "acceptedAnswer": {
                    "@type": "Answer",
                    "text": "아닙니다. 이 서비스는 당첨번호를 예측하지 않습니다. 과거 데이터와 조합 규칙을 참고해 흔한 선택 패턴을 피하도록 돕는 도구입니다."
                  }
                },
                {
                  "@type": "Question",
                  "name": "추천 번호를 사면 당첨 확률이 올라가나요?",
                  "acceptedAnswer": {
                    "@type": "Answer",
                    "text": "아닙니다. 로또 6/45에서 모든 6개 조합의 1등 당첨 확률은 동일합니다(1/8,145,060). 추천 번호를 사더라도 당첨 확률 자체는 변하지 않습니다."
                  }
                },
                {
                  "@type": "Question",
                  "name": "추천 알고리즘은 어떻게 작동하나요?",
                  "acceptedAnswer": {
                    "@type": "Answer",
                    "text": "생일 번호, 연속 번호 등 흔한 조합 패턴을 통계적으로 분석해 배제합니다. 당첨을 예측하거나 보장하지 않습니다."
                  }
                },
                {
                  "@type": "Question",
                  "name": "데이터는 어디서 가져오나요?",
                  "acceptedAnswer": {
                    "@type": "Answer",
                    "text": "당첨번호는 동행복권 공식 API에서 수집합니다."
                  }
                }
              ]
            }""";

    private final LottoFetchLogQueryService fetchLogQueryService;
    private final WinningNumberQueryService winningNumberQueryService;
    private final ObjectProvider<BuildProperties> buildPropertiesProvider;
    private final Environment environment;

    @GetMapping("/methodology")
    public String methodology() {
        return "info/methodology";
    }

    @GetMapping("/data-source")
    public String dataSource(Model model) {
        model.addAttribute("changeLog", fetchLogQueryService.recentCollectionLogs(CHANGE_LOG_LIMIT));
        winningNumberQueryService.findLatest().ifPresent(latest -> {
            model.addAttribute("latestStoredRound", latest.round());
            model.addAttribute("latestStoredDate", latest.drawDate());
        });
        model.addAttribute("expectedRound", winningNumberQueryService.expectedCurrentRound());
        model.addAttribute("maxSearchRound", winningNumberQueryService.maxPossibleRound());
        addBuildAttributes(model);
        return "info/data-source";
    }

    private void addBuildAttributes(Model model) {
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        if (buildProperties != null) {
            model.addAttribute("appVersion", buildProperties.getVersion());
            if (buildProperties.getTime() != null) {
                model.addAttribute("buildTimeText", buildProperties.getTime().toString());
            }
        }
        model.addAttribute("javaVersion", System.getProperty("java.version"));

        String commit = firstNonBlank(
                environment.getProperty("KRAFT_BUILD_COMMIT"),
                environment.getProperty("KRAFT_APP_IMAGE_TAG"));
        if (commit != null) {
            model.addAttribute("buildCommit", commit);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("pageJsonLd", FAQ_JSON_LD);
        return "info/faq";
    }

    @GetMapping("/responsible-play")
    public String responsiblePlay() {
        return "info/responsible-play";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "info/privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "info/terms";
    }

    @GetMapping("/contact")
    public String contact() {
        return "info/contact";
    }
}
