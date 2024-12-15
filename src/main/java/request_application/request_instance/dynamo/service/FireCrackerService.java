package request_application.request_instance.dynamo.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import request_application.request_instance.dynamo.controller.dto.ResourceResponse;
import request_application.request_instance.dynamo.controller.dto.ResourceRequest;
import request_application.request_instance.dynamo.exception.FailedFireCracker;
import request_application.request_instance.dynamo.repository.FireCrackerRepository;
import request_application.request_instance.dynamo.table.FireCrackerInfo;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;


import static java.lang.Thread.sleep;

@Service
@RequiredArgsConstructor
public class FireCrackerService {
    private static final Logger logger = LoggerFactory.getLogger(FireCrackerService.class);

    @Value("${firecracker.port}")
    private Integer port;

    private final FireCrackerRepository fireCrackerRepository;
    private final RestTemplate restTemplate;

    public Object processRequest(ResourceRequest resourceRequest) throws InterruptedException {
        FireCrackerInfo useInstance;
        while (true) {
            try {
                useInstance = getAvailableFireCrackerInstance(resourceRequest);
                updateRemainingMemory(useInstance, resourceRequest);
                break;
            } catch (ConditionalCheckFailedException e){
                logger.warn("Version modified, retrying updateRemainingMemory.", e);
            }
        }
        return requestFireCracker(useInstance, resourceRequest).getData();
    }

    private FireCrackerInfo getAvailableFireCrackerInstance(ResourceRequest resourceRequest) {
        return fireCrackerRepository.findByRemainResourceIp(resourceRequest.getRequestMemory())
                .orElseThrow(() -> new RuntimeException("No available FireCracker instance found."));
    }

    private void updateRemainingMemory(FireCrackerInfo useInstance, ResourceRequest resourceRequest) {
        fireCrackerRepository.updateRemainMemoryIfVersionFuture(useInstance, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), resourceRequest);
    }

    private void releaseResource(FireCrackerInfo useInstance, ResourceRequest resourceRequest, long now) {
        try {
            fireCrackerRepository.releaseResource(useInstance, resourceRequest, now);
        } catch (ConditionalCheckFailedException e) {
            logger.warn("Version modified, retrying releaseResource.", e);
            releaseResource(useInstance, resourceRequest, now);
        }
    }

    private ResourceResponse requestFireCracker(final FireCrackerInfo usedInstance, final ResourceRequest resourceRequest) {
        ResponseEntity<ResourceResponse> responseEntity = restTemplate.postForEntity(buildRequestURI(usedInstance.getIp()), resourceRequest, ResourceResponse.class);
        HttpStatusCode statusCode = responseEntity.getStatusCode();
        releaseResource(usedInstance, resourceRequest, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));

        if (statusCode.is2xxSuccessful()) {
            return responseEntity.getBody();
        } else {
            throw new FailedFireCracker(responseEntity.getBody().getData());
        }
    }

    private URI buildRequestURI(String ip) {
        try {
            return new URI("http", null, ip, port, "/instance-start", null, null);
        } catch (URISyntaxException e) {
            // 예외 처리
            throw new IllegalArgumentException("Invalid IP address: " + ip, e);
        }
    }
}