package request_application.request_instance.dynamo.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import request_application.request_instance.dynamo.controller.dto.CommonResponse;
import request_application.request_instance.dynamo.controller.dto.ToWarmUpRequest;
import request_application.request_instance.dynamo.controller.dto.StartVMResponse;
import request_application.request_instance.dynamo.controller.dto.StartVMRequest;
import request_application.request_instance.dynamo.exception.FailedFireCracker;
import request_application.request_instance.dynamo.repository.FireCrackerRepository;
import request_application.request_instance.dynamo.table.FireCrackerInfo;
import request_application.request_instance.dynamo.table.WarmUpInfo;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class FireCrackerService {
    private static final Logger logger = LoggerFactory.getLogger(FireCrackerService.class);

    @Value("${firecracker.port}")
    private Integer port;

    private final FireCrackerRepository fireCrackerRepository;
    private final RestTemplate restTemplate;

    public Object processRequest(StartVMRequest startVMRequest) throws InterruptedException {
        String arn = startVMRequest.getArn();
        Optional<WarmUpInfo> existingWarmUpInfo = findWarmUpInfo(arn);
        
        return existingWarmUpInfo
                .map(warmUpInfo -> requestToWarmUpFireCracker(startVMRequest, warmUpInfo))
                .orElseGet(() -> (CommonResponse) handleRequestWithoutWarmUp(startVMRequest));
    }

    private Object handleRequestWithoutWarmUp(StartVMRequest startVMRequest) {
        FireCrackerInfo availableInstance = retryUntilAvailable(startVMRequest);
        return requestFireCracker(availableInstance, startVMRequest).getData();
    }

    private FireCrackerInfo retryUntilAvailable(StartVMRequest startVMRequest) {
        FireCrackerInfo useInstance;

        while (true) {
            try {
                useInstance = getAvailableFireCrackerInstance(startVMRequest);
                updateRemainingMemory(useInstance, startVMRequest);
                return useInstance;
            } catch (ConditionalCheckFailedException e) {
                logger.warn("Version modified, retrying updateRemainingMemory.", e);
            }
        }
    }

    private FireCrackerInfo getAvailableFireCrackerInstance(StartVMRequest startVMRequest) {
        return fireCrackerRepository.findByRemainResourceIp(startVMRequest.getRequestMemory())
                .orElseThrow(() -> new RuntimeException("No available FireCracker instance found."));
    }

    private void updateRemainingMemory(FireCrackerInfo useInstance, StartVMRequest startVMRequest) {
        fireCrackerRepository.updateRemainMemoryIfVersionFuture(useInstance,
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), startVMRequest);
    }

    private CommonResponse requestFireCracker(final FireCrackerInfo usedInstance,
                                                final StartVMRequest startVMRequest) {
        ResponseEntity<CommonResponse> responseEntity = restTemplate.postForEntity(
                buildRequestURI(usedInstance.getIp()), startVMRequest, CommonResponse.class);

        validateResponse(responseEntity);

        StartVMResponse startVMResponse = (StartVMResponse) responseEntity.getBody().getData();
        fireCrackerRepository.insertWarmUpInfo(WarmUpInfo.of(startVMRequest, usedInstance, startVMResponse));

        return responseEntity.getBody();
    }

    private CommonResponse requestToWarmUpFireCracker(final StartVMRequest startVMRequest,
                                                         final WarmUpInfo warmUpInfo) {
        ToWarmUpRequest toWarmUpRequest = ToWarmUpRequest.toWarmUpRequest(startVMRequest, warmUpInfo);
        ResponseEntity<CommonResponse> responseEntity = restTemplate.postForEntity(
                buildRequestToWarmUpURI(warmUpInfo.getIp()), toWarmUpRequest, CommonResponse.class);

        validateResponse(responseEntity);
        return responseEntity.getBody();
    }

    private void validateResponse(ResponseEntity<CommonResponse> responseEntity) {
        HttpStatusCode statusCode = responseEntity.getStatusCode();
        if (!statusCode.is2xxSuccessful()) {
            throw new FailedFireCracker(responseEntity.getBody().getData());
        }
    }

    private URI buildRequestURI(String ip) {
        return createURI(ip, "/instance-start");
    }

    private URI buildRequestToWarmUpURI(String ip) {
        return createURI(ip, "/to-warm-up");
    }

    private URI createURI(String ip, String path) {
        try {
            return new URI("http", null, ip, port, path, null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid IP address: " + ip, e);
        }
    }

    public Optional<WarmUpInfo> findWarmUpInfo(String arn) {
        return fireCrackerRepository.findWarmUpInfo(arn);
    }

}