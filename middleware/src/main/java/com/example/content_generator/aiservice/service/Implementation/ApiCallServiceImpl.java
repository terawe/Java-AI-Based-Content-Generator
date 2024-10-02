package com.example.content_generator.aiservice.service.Implementation;

import com.example.content_generator.aiservice.service.ApiCallService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class ApiCallServiceImpl implements ApiCallService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiCallServiceImpl.class);
    private WebClient webClient;

    @Value("${BackendServiceBaseUrl}")
    private String baseUrl;

    @Value("${BackendServiceAccessKey}")
    private String accessKey;

    public ApiCallServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @PostConstruct
    public void init() {
        // Access and log the properties after they have been injected
        LOGGER.info("Initializing ApiCallServiceImpl with baseUrl: {}", baseUrl);

        // Rebuild the WebClient with the correct baseUrl and headers
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("api-key", accessKey)
                .filter((request, next) -> {
                    LOGGER.info("Request: {}", request.method().toString().toUpperCase() + " " + request.url());
                    return next.exchange(request);
                })
                .build();
    }

    public <T> Mono<List<T>> get(String endpoint, Map<String, String> queryParams) {
        return webClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(endpoint);
                    queryParams.forEach(uriBuilder::queryParam);
                    return uriBuilder.build();
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    LOGGER.error("4xx error: {}", clientResponse.statusCode());
                    return clientResponse.createException().flatMap(Mono::error);
                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    LOGGER.error("5xx error: {}", clientResponse.statusCode());
                    return clientResponse.createException().flatMap(Mono::error);
                })
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }

    public <T> Mono<List<T>> post(String endpoint, Object body) {
        return webClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path(endpoint);
                    return uriBuilder.build();
                })
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    LOGGER.error("4xx error: {}", clientResponse.statusCode());
                    return clientResponse.createException().flatMap(Mono::error);
                })
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
                    LOGGER.error("5xx error: {}", clientResponse.statusCode());
                    return clientResponse.createException().flatMap(Mono::error);
                })
                .bodyToMono(new ParameterizedTypeReference<>() {
                });
    }
}
