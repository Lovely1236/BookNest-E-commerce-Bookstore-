package org.example.web.service;

import java.util.List;
import java.util.Map;

public interface BackendGateway {
    List<Map<String, Object>> getList(String url);
    List<Map<String, Object>> getList(String url, String token);
    Map<String, Object> getMap(String url, String token);
    Map<String, Object> postForMap(String url, Object request) throws BackendGatewayException;
    Map<String, Object> putForMap(String url, Object request) throws BackendGatewayException;
    void postStrict(String url, Object request) throws BackendGatewayException;
    void putStrict(String url, Object request) throws BackendGatewayException;
    void deleteStrict(String url) throws BackendGatewayException;
    ServiceUrls serviceUrls();
}
