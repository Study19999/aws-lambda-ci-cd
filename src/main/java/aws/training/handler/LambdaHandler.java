package aws.training.handler;

import aws.training.client.SNSClient;
import aws.training.exception.AmazonException;
import aws.training.exception.StatusCodesEnum;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.AmazonSNSException;
import com.amazonaws.services.sns.model.PublishRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.logging.Logger;

@Slf4j
@Getter
@AllArgsConstructor
public class LambdaHandler implements RequestHandler<SQSEvent, Void> {

    private static final String AWS_REGION = "REGION";
    private static final String SNS_TOPIC_ARN = "SNS_TOPIC_ARN";

    private final SNSClient snsClient;
    private final AmazonSNS sns;

    public LambdaHandler() {
        String region = System.getenv(AWS_REGION);
        String snsTopicArn = System.getenv(SNS_TOPIC_ARN);

        this.snsClient = SNSClient.builder()
                .region(region)
                .topicArn(snsTopicArn)
                .build();
        this.sns = AmazonSNSClient.builder()
                .withRegion(region)
                .build();
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        if (event.getRecords().isEmpty()) {
            throw new NullPointerException("No message to process!");
        } else {
            for (SQSEvent.SQSMessage message : event.getRecords()) {
                log.info("Processing messages");
                sendMessageToTopic(message.getBody());
                log.info(StatusCodesEnum.OK + ", Successfully processed message: " + message.getMessageId());
            }
        }
        return null;
    }

    private void sendMessageToTopic(String message){
        try {
            var publishRequest = new PublishRequest()
                    .withMessage(message)
                    .withTopicArn(snsClient.getTopicArn());
            sns.publish(publishRequest);
        } catch (AmazonSNSException e) {
            throw new AmazonException(StatusCodesEnum.NOT_FOUND, e.getMessage(), e);
        }
    }

}

