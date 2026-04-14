package org.example.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
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
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<Object> resp = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Object.class);
            Object body = resp.getBody();
            if (body instanceof List) return (List<Map<String,Object>>) body;
            return List.of();
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
}
