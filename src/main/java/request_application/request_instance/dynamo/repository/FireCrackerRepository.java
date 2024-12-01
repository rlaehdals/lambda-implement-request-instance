package request_application.request_instance.dynamo.repository;

import org.springframework.stereotype.Component;
import request_application.request_instance.dynamo.controller.dto.ResourceRequest;
import request_application.request_instance.dynamo.table.FireCrackerInfo;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Comparator;
import java.util.Optional;

@Component
public class FireCrackerRepository {
    private final DynamoDbTable<FireCrackerInfo> fireCrackerInfoDynamoDbTable;

    public FireCrackerRepository(DynamoDbEnhancedClient dynamoDbEnhancedClient) {
        this.fireCrackerInfoDynamoDbTable = dynamoDbEnhancedClient
                .table("firecracker", TableSchema.fromImmutableClass(FireCrackerInfo.class));
    }

    public void updateRemainMemoryIfVersionFuture(FireCrackerInfo useInstance, long versionId, ResourceRequest resourceRequest) {
        fireCrackerInfoDynamoDbTable.updateItem(buildUpdateRequestWithCondition(useInstance, versionId, resourceRequest.getRequestMemory()));
    }

    public void releaseResource(FireCrackerInfo usedInstance, ResourceRequest resourceRequest, long versionId) {
        fireCrackerInfoDynamoDbTable.updateItem(buildReleaseRequestWithCondition(usedInstance, versionId, resourceRequest.getRequestMemory()));
    }

    public Optional<FireCrackerInfo> findByRemainResourceIp(Integer requestMemory) {
        return fireCrackerInfoDynamoDbTable.scan(ScanEnhancedRequest.builder().build())
                .items()
                .stream()
                .filter(item -> item.getRemainMemory() >= requestMemory)
                .min(Comparator.comparing(FireCrackerInfo::getRemainMemory));
    }

    private UpdateItemEnhancedRequest<FireCrackerInfo> buildUpdateRequestWithCondition(FireCrackerInfo fireCrackerInfo, long versionId, int requestMemory) {
        return UpdateItemEnhancedRequest.builder(FireCrackerInfo.class)
                .item(buildRequestUpdatedItem(fireCrackerInfo, versionId, requestMemory))
                .conditionExpression(buildConditionExpression(versionId))
                .build();
    }

    private FireCrackerInfo buildRequestUpdatedItem(FireCrackerInfo fireCrackerInfo, long versionId, int requestMemory) {
        return FireCrackerInfo.builder()
                .ip(fireCrackerInfo.getIp())
                .remainMemory(fireCrackerInfo.getRemainMemory() - requestMemory)
                .versionId(versionId)
                .build();
    }

    private UpdateItemEnhancedRequest<FireCrackerInfo> buildReleaseRequestWithCondition(FireCrackerInfo fireCrackerInfo, long versionId, int requestMemory) {
        return UpdateItemEnhancedRequest.builder(FireCrackerInfo.class)
                .item(buildReleaseUpdatedItem(fireCrackerInfo, versionId, requestMemory))
                .conditionExpression(buildConditionExpression(versionId))
                .build();
    }

    private FireCrackerInfo buildReleaseUpdatedItem(FireCrackerInfo fireCrackerInfo, long versionId, int requestMemory) {
        return FireCrackerInfo.builder()
                .ip(fireCrackerInfo.getIp())
                .remainMemory(fireCrackerInfo.getRemainMemory() + requestMemory)
                .versionId(versionId)
                .build();
    }

    private Expression buildConditionExpression(long versionId) {
        return Expression.builder()
                .expression("versionId <= :newVersionId")
                .putExpressionValue(":newVersionId", AttributeValue.builder().n(Long.toString(versionId)).build())
                .build();
    }
}