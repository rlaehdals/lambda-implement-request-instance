package request_application.request_instance.dynamo.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import request_application.request_instance.dynamo.table.WarmUpInfo;

@Getter
@AllArgsConstructor
@Builder
public class ToWarmUpRequest {
    private String firecrackerInternalIP;
    private String filePath;
    private String env;

    public static ToWarmUpRequest toWarmUpRequest(final StartVMRequest resourceRequest, final WarmUpInfo warmUpInfo){
        return ToWarmUpRequest.builder()
        .env(resourceRequest.getEnv())
        .filePath(resourceRequest.getFilePath())
        .firecrackerInternalIP(warmUpInfo.getFirecrackerInternalIP())
        .build();
    }
}
