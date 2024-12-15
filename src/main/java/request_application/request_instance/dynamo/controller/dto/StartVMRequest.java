package request_application.request_instance.dynamo.controller.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StartVMRequest {

    private Integer requestMemory;
    private String architect;
    private String language;
    private String arn;
    private String bucketName;
    private String filePath;
    private String env;

}
