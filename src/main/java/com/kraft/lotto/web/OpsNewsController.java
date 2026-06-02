package com.kraft.lotto.web;

import com.kraft.lotto.feature.news.application.NewsCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@OpsApi
@RestController
@RequestMapping("/ops/news")
@Tag(name = "Ops", description = "Operational endpoints for collection and failure logs")
public class OpsNewsController {

    private final NewsCollectionService newsCollectionService;

    public OpsNewsController(NewsCollectionService newsCollectionService) {
        this.newsCollectionService = newsCollectionService;
    }

    @PostMapping("/collect")
    @Operation(summary = "Manually trigger news collection")
    public Map<String, Object> collectNews() {
        NewsCollectionService.NewsCollectResult result = newsCollectionService.collect();
        newsCollectionService.purgeOldArticles();
        Map<String, Object> response = new TreeMap<>();
        response.put("saved", result.saved());
        response.put("skipped", result.skipped());
        return response;
    }
}
