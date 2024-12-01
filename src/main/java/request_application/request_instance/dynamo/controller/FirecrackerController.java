package request_application.request_instance.dynamo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import request_application.request_instance.dynamo.controller.dto.ResourceRequest;
import request_application.request_instance.dynamo.service.FireCrackerService;

@RestController
@RequiredArgsConstructor
public class FirecrackerController {

    private final FireCrackerService fireCrackerService;
    @PostMapping("/request")
    public ResponseEntity<Object> request(@RequestBody ResourceRequest request) throws InterruptedException {
        return ResponseEntity.ok(fireCrackerService.processRequest(request));
    }

}
