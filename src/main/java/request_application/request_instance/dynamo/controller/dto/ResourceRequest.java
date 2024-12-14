package request_application.request_instance.dynamo.controller.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResourceRequest {

    private Integer requestMemory;
    private String architect;
    private String language;
    private String codeLocation;

}
