package aws.training.exception;

public enum StatusCodesEnum {
    OK(200, "OK"),
    NOT_FOUND(404, "Not Found"),
    BAD_REQUEST(400, "Bad Request");


    private final int code;
    private final String message;

    StatusCodesEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
