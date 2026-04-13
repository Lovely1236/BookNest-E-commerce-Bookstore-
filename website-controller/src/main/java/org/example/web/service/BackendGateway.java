package org.example.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.web.config.ServiceUrlsProperties;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BackendGateway {

    private final RestTemplate restTemplate;
    private final ServiceUrlsProperties serviceUrls;
    private final ObjectMapper objectMapper;

    public ServiceUrlsProperties serviceUrls() {
        return serviceUrls;
    }

    public Map<String, Object> getObject(String url) {
        return getObject(url, null);
    }

    public Map<String, Object> getObject(String url, String token) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    request(HttpMethod.GET, url, null, token),
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return response.getBody() == null ? Collections.emptyMap() : response.getBody();
        } catch (RestClientException ex) {
            return Collections.emptyMap();
        }
    }

    public List<Map<String, Object>> getList(String url) {
        return getList(url, null);
    }

    public List<Map<String, Object>> getList(String url, String token) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    request(HttpMethod.GET, url, null, token),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            return response.getBody() == null ? Collections.emptyList() : response.getBody();
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

    public Map<String, Object> postForObjectStrict(String url, Object body) {
        return postForObjectStrict(url, body, null);
    }

    public Map<String, Object> postForObjectStrict(String url, Object body, String token) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    request(HttpMethod.POST, url, body, token),
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            return response.getBody() == null ? Collections.emptyMap() : response.getBody();
        } catch (RestClientResponseException ex) {
            throw new BackendGatewayException(extractMessage(ex));
        } catch (RestClientException ex) {
            throw new BackendGatewayException("Unable to reach the backend service.");
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

    public void postStrict(String url, Object body) {
        postStrict(url, body, null);
    }

    public void postStrict(String url, Object body, String token) {
        try {
            restTemplate.exchange(request(HttpMethod.POST, url, body, token), Void.class);
        } catch (RestClientResponseException ex) {
            throw new BackendGatewayException(extractMessage(ex));
        } catch (RestClientException ex) {
            throw new BackendGatewayException("Unable to reach the backend service.");
        }
    }

    public void putStrict(String url, Object body) {
        putStrict(url, body, null);
    }

    public void putStrict(String url, Object body, String token) {
        try {
            restTemplate.exchange(request(HttpMethod.PUT, url, body, token), Void.class);
        } catch (RestClientResponseException ex) {
            throw new BackendGatewayException(extractMessage(ex));
        } catch (RestClientException ex) {
            throw new BackendGatewayException("Unable to reach the backend service.");
        }
    }

    public void deleteStrict(String url) {
        deleteStrict(url, null);
    }

    public void deleteStrict(String url, String token) {
        try {
            restTemplate.exchange(request(HttpMethod.DELETE, url, null, token), Void.class);
        } catch (RestClientResponseException ex) {
            throw new BackendGatewayException(extractMessage(ex));
        } catch (RestClientException ex) {
            throw new BackendGatewayException("Unable to reach the backend service.");
        }
    }

    public void delete(String url) {
        try {
            restTemplate.delete(url);
        } catch (RestClientException ignored) {
        }
    }

    private RequestEntity<Object> request(HttpMethod method, String url, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
        return new RequestEntity<>(body, headers, method, URI.create(url));
    }

    private String extractMessage(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(body);
                if (root.has("errors") && root.get("errors").isObject()) {
                    StringBuilder builder = new StringBuilder();
                    Iterator<Map.Entry<String, JsonNode>> fields = root.get("errors").fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        if (!builder.isEmpty()) {
                            builder.append(", ");
                        }
                        builder.append(field.getKey()).append(": ").append(field.getValue().asText());
                    }
                    if (!builder.isEmpty()) {
                        return builder.toString();
                    }
                }
                if (root.has("message")) {
                    return root.get("message").asText();
                }
                if (root.has("error")) {
                    return root.get("error").asText();
                }
                if (root.isTextual()) {
                    return root.asText();
                }
            } catch (Exception ignored) {
                return body;
            }
            return body;
        }
        return ex.getStatusText() == null || ex.getStatusText().isBlank()
                ? "Backend request failed."
                : ex.getStatusText();
    }
}
