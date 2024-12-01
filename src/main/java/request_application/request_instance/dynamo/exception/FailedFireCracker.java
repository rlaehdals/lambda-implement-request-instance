package request_application.request_instance.dynamo.exception;

public class FailedFireCracker extends RuntimeException{
    public FailedFireCracker(Object object){
        super(object.toString());
    }
}
