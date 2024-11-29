package com.jeju.barrier.OSRM.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class OpenRouteService {
    private static final String BASE_URL = "https://api.openrouteservice.org/v2/directions/foot-walking";

    @Value("${openroute.api.key}")
    private String apiKey;

    public Object getAccessibleRoute(String start, String end) {
        String url = UriComponentsBuilder.fromUriString(BASE_URL)
                .queryParam("start", start)
                .queryParam("end", end)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<?> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        return restTemplate.exchange(url, HttpMethod.GET, entity, Object.class).getBody();
    }
}