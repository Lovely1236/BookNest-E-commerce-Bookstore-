package org.example.web.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.example.web.config.ServiceUrlsProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class BackendGateway {

    private final RestTemplate restTemplate;
    private final ServiceUrlsProperties serviceUrls;

    public ServiceUrlsProperties serviceUrls() {
        return serviceUrls;
    }

    public Map<String, Object> getObject(String url) {
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    }).getBody();
        } catch (RestClientException ex) {
            return Collections.emptyMap();
        }
    }

    public List<Map<String, Object>> getList(String url) {
        try {
            return restTemplate.exchange(url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    }).getBody();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }

    public double getDouble(String url, double fallback) {
        try {
            ResponseEntity<Double> response = restTemplate.getForEntity(url, Double.class);
            return response.getBody() == null ? fallback : response.getBody();
        } catch (RestClientException ex) {
            return fallback;
        }
    }

    public String getString(String url, String fallback) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getBody() == null ? fallback : response.getBody();
        } catch (RestClientException ex) {
            return fallback;
        }
    }

    public void post(String url, Object body) {
        try {
            restTemplate.postForEntity(url, body, Void.class);
        } catch (RestClientException ignored) {
        }
    }

    public Map<String, Object> postForObject(String url, Object body) {
        try {
            return restTemplate.postForObject(url, body, Map.class);
        } catch (RestClientException ex) {
            return Collections.emptyMap();
        }
    }

    public void put(String url, Object body) {
        try {
            restTemplate.put(url, body);
        } catch (RestClientException ignored) {
        }
    }

    public Map<String, Object> putForObject(String url, Object body) {
        try {
            RequestEntity<Object> request = RequestEntity.put(url).body(body);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    request, new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return response.getBody() == null ? Collections.emptyMap() : response.getBody();
        } catch (RestClientException ex) {
            return Collections.emptyMap();
        }
    }

    public void delete(String url) {
        try {
            restTemplate.delete(url);
        } catch (RestClientException ignored) {
        }
    }
}
