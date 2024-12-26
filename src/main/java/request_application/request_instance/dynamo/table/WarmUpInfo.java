package request_application.request_instance.dynamo.table;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import request_application.request_instance.dynamo.controller.dto.StartVMRequest;
import request_application.request_instance.dynamo.controller.dto.CommonResponse;
import request_application.request_instance.dynamo.controller.dto.StartVMResponse;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;


@Getter
@DynamoDbBean
@DynamoDbImmutable(builder = WarmUpInfo.WarmUpInfoBuilder.class)
@Builder
@AllArgsConstructor
public class WarmUpInfo {

    private String ip;
    private String arn;
    private String firecrackerInternalIP;
    private long versionId;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("arn")
    public String getArn() {
        return arn;
    }

    @DynamoDbAttribute("ip")
    public String getIp() {
        return ip;
    }

    @DynamoDbAttribute("firecrackerInternalIP")
    public String getFirecrackerInternalIP() {
        return firecrackerInternalIP;
    }

    @DynamoDbAttribute("versionId")
    public long getVersionId(){
        return versionId;
    }

    public static WarmUpInfo of(final StartVMRequest request, FireCrackerInfo fireCrackerInfo,final StartVMResponse startVMResponse){
        return WarmUpInfo.builder()
                .ip(fireCrackerInfo.getIp())
                .arn(request.getArn())
                .firecrackerInternalIP(startVMResponse.getFirecrackerInternalIP())
                .versionId(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))
                .build();
    }
}
