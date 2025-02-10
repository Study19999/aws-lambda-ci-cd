package aws.training.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SNSClient {

    private String region;
    private String topicArn;
}
