package org.example.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
public class SimpleBackendGateway implements BackendGateway {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ServiceUrls serviceUrls;

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getList(String url) {
        try {
            Object resp = restTemplate.getForObject(url, Object.class);
            if (resp instanceof List) return (List<Map<String,Object>>) resp;
            return List.of();
        } catch (Exception ex) {
            throw new BackendGatewayException(ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getList(String url, String token) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token));
            ResponseEntity<Object> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            Object body = resp.getBody();
            if (body instanceof List) return (List<Map<String,Object>>) body;
            return List.of();
        } catch (Exception ex) {
            throw new BackendGatewayException(ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMap(String url, String token) {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token));
            ResponseEntity<Object> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Object.class);
            Object body = resp.getBody();
            if (body instanceof Map) return (Map<String, Object>) body;
            return Map.of();
        } catch (Exception ex) {
            throw new BackendGatewayException(ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> postForMap(String url, Object request) throws BackendGatewayException {
        try {
            ResponseEntity<Object> response = restTemplate.postForEntity(url, request, Object.class);
            Object body = response.getBody();
            if (body instanceof Map) return (Map<String, Object>) body;
            return Map.of();
        } catch (Exception ex) {
            throw new BackendGatewayException(ex.getMessage(), ex);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> putForMap(String url, Object request) throws BackendGatewayException {
        try {
            HttpEntity<Object> entity = new HttpEntity<>(request);
            ResponseEntity<Object> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Object.class);
            Object body = response.getBody();
            if (body instanceof Map) return (Map<String, Object>) body;
            return Map.of();
        } catch (Exception ex) {
            throw new BackendGatewayException(ex.getMessage(), ex);
        }
    }

    @Override
    public void postStrict(String url, Object request) throws BackendGatewayException {
        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception ex) {
            throw new BackendGatewayException(ex.getMessage(), ex);
        }
    }

    @Override
    public void putStrict(String url, Object request) throws BackendGatewayException {
        try {
            restTemplate.put(url, request);
        } catch (Exception ex) {
            throw new BackendGatewayException(ex.getMessage(), ex);
        }
    }

    @Override
    public void deleteStrict(String url) throws BackendGatewayException {
        try {
            restTemplate.delete(url);
        } catch (Exception ex) {
            throw new BackendGatewayException(ex.getMessage(), ex);
        }
    }

    @Override
    public ServiceUrls serviceUrls() { return serviceUrls; }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }
}
