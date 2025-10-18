package com.comp5619.libraryreservationreviewhub.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具（Google Custom Search 版）
 */
public class WebSearchTool {

    // Google Custom Search API URL
    private static final String SEARCH_API_URL = "https://www.googleapis.com/customsearch/v1";

    private final String apiKey;
    private final String searchEngineId; // cx

    public WebSearchTool(String apiKey, String searchEngineId) {
        this.apiKey = apiKey;
        this.searchEngineId = searchEngineId;
    }

    @Tool(description = "Search for information from Google Custom Search")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        try {
            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("key", apiKey);
            paramMap.put("cx", searchEngineId);
            paramMap.put("q", query);
            paramMap.put("num", 5); // 返回前 5 条结果
            paramMap.put("alt", "json");

            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            JSONObject jsonObject = JSONUtil.parseObj(response);
            JSONArray items = jsonObject.getJSONArray("items"); // 搜索结果列表

            if (items == null) return "No results found.";

            List<Object> topResults = items.subList(0, Math.min(items.size(), 5));
            String result = topResults.stream().map(obj -> {
                JSONObject item = (JSONObject) obj;
                String title = item.getStr("title");
                String link = item.getStr("link");
                String snippet = item.getStr("snippet");
                return String.format("Title: %s, Link: %s, Snippet: %s", title, link, snippet);
            }).collect(Collectors.joining("\n\n"));

            return result;

        } catch (Exception e) {
            return "Error searching Google: " + e.getMessage();
        }
    }
}
