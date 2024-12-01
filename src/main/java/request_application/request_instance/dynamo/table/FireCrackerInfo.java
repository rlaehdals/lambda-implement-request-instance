package request_application.request_instance.dynamo.table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;


@Getter
@DynamoDbBean
@DynamoDbImmutable(builder = FireCrackerInfo.FireCrackerInfoBuilder.class)
@Builder
@AllArgsConstructor
public class FireCrackerInfo {

    private String ip;
    private Integer remainMemory;

    private long versionId;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("ip")
    public String getIp() {
        return ip;
    }
    @DynamoDbAttribute("remainMemory")
    public Integer getRemainMemory() {
        return remainMemory;
    }

    @DynamoDbAttribute("versionId")
    public long getVersionId(){
        return versionId;
    }
}
