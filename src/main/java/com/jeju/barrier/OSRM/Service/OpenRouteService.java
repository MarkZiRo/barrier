package com.jeju.barrier.OSRM.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;

@Slf4j
@Service
public class OpenRouteService {

    private static final String BASE_URL = "https://api.openrouteservice.org/v2/directions/foot-walking";
    private static final String BASE_URL2 = "https://api.openrouteservice.org/v2/directions/wheelchair";
    private static final String ELEVATION_URL = "https://api.openrouteservice.org/elevation/point";

    @Value("${openroute.api.key}")
    private String apiKey;

    public Object getAccessibleWalking(String start, String end) {
        String url = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("start", start)
                .queryParam("end", end)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization",  apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            log.info("Request URL: " + request.getURI());
            log.info("Request Headers: " + request.getHeaders());
            ClientHttpResponse response = execution.execute(request, body);
            log.info("Response Status: " + response.getStatusCode());
            return response;
        }));

        return restTemplate.exchange(url, HttpMethod.GET, entity, Object.class).getBody();
    }

    public Object getAccessibleWheelchair(String start, String end) {
        String url = UriComponentsBuilder.fromUriString(BASE_URL2)
                .queryParam("start", start)
                .queryParam("end", end)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization",  apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            log.info("Request URL: " + request.getURI());
            log.info("Request Headers: " + request.getHeaders());
            ClientHttpResponse response = execution.execute(request, body);
            log.info("Response Status: " + response.getStatusCode());
            return response;
        }));


        return restTemplate.exchange(url, HttpMethod.GET, entity, Object.class).getBody();
    }

    public Object getElevation(String coordinates) {
        String url = UriComponentsBuilder.fromUriString(ELEVATION_URL)
                .queryParam("geometry", coordinates)  // 좌표값을 쿼리 파라미터로 전달
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        // API 요청/응답 로깅
        restTemplate.setInterceptors(Collections.singletonList((request, body, execution) -> {
            log.info("Request URL: " + request.getURI());
            log.info("Request Headers: " + request.getHeaders());
            ClientHttpResponse response = execution.execute(request, body);
            log.info("Response Status: " + response.getStatusCode());
            return response;
        }));

        return restTemplate.exchange(url, HttpMethod.GET, entity, Object.class).getBody();
    }
}