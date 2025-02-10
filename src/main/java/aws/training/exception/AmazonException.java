package aws.training.exception;

import com.amazonaws.services.sns.model.AmazonSNSException;

public class AmazonException extends AmazonSNSException {
    private StatusCodesEnum codesEnum;

    public AmazonException(StatusCodesEnum codesEnum, String message, Throwable cause) {
        super(message);
        this.codesEnum = codesEnum;
        initCause(cause);
    }
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
