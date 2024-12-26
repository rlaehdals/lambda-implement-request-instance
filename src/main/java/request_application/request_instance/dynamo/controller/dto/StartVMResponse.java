package request_application.request_instance.dynamo.controller.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class StartVMResponse {
    private List<String> output;
    private String firecrackerInternalIP;
}
