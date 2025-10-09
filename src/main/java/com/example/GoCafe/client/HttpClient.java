package com.example.GoCafe.client;

import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

@Slf4j
@Component
public class HttpClient {

    private final WebClient webClient; // 한 줄 주석

    public HttpClient(
            @Value("${menu-extract.base-url}") String baseUrl,
            @Value("${menu-extract.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${menu-extract.read-timeout-ms:5000}") int readTimeoutMs
    ) {
        reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs)); // 한 줄 주석
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build(); // 한 줄 주석
    }

    public Map<String, Object> extractReviewTags(
            String text,
            Object menuMap,                         // Map<String, List<Float>> 또는 List<Map<String, List<Float>>>
            Map<String, List<String>> alias         // Map<String, List<String>>
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("text", text);
        if (menuMap != null) body.put("menuMap", menuMap);
        if (alias != null) body.put("alias", alias);

        return webClient.post()
                .uri("/extract")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    public byte[] getMenuVectorBytes(String menuText) {
        if (menuText == null || menuText.trim().isEmpty()) {
            throw new IllegalArgumentException("menuText must not be blank"); // 한 줄 주석
        }

        // 1) FastAPI 호출
        Map<String, Object> resp = webClient.post()
                .uri("/menu-vec")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("menuText", menuText))
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // 한 줄 주석

        // 2) 벡터 추출
        Object v = (resp != null) ? resp.get("vector") : null;
        if (!(v instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalStateException("Invalid /menu-vec response: 'vector' not found"); // 한 줄 주석
        }

        // 3) float32(리틀엔디언)로 직렬화
        ByteBuffer bb = ByteBuffer.allocate(list.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (Object e : list) {
            float f = (e instanceof Number num) ? num.floatValue() : Float.parseFloat(String.valueOf(e));
            bb.putFloat(f);
        }
        return bb.array();
    }

}