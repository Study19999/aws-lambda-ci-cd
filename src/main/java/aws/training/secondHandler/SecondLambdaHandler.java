package aws.training.secondHandler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Getter
@AllArgsConstructor
public class SecondLambdaHandler implements RequestHandler<Map<String, Object>, String> {

    private static final String AWS_REGION = "REGION";
    private static final String S3_BUCKET = "S3_BUCKET";
    private static final String DB_HOST = "DB_HOST";
    private static final String DB_DATABASE = "DB_DATABASE";
    private static final String DB_USER = "DB_USER";
    private static final String DB_PASSWORD = "DB_PASSWORD";
    private static final String TABLE_NAME = "TABLE_NAME";

    private final String s3Bucket;
    private final String dbHost;
    private final String dbUser;
    private final String dbPassword;
    private final String dbDatabase;
    private final String tableName;
    private final AmazonS3 s3Client;

    public SecondLambdaHandler() {
        String region = System.getenv(AWS_REGION);
        this.s3Bucket = System.getenv(S3_BUCKET);
        this.dbHost = System.getenv(DB_HOST);
        this.dbDatabase = System.getenv(DB_DATABASE);
        this.tableName = System.getenv(TABLE_NAME);

        this.s3Client = AmazonS3Client.builder()
                .withRegion(region)
                .build();

        this.dbUser = System.getenv(DB_USER);
        this.dbPassword = System.getenv(DB_PASSWORD);
    }

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        String source = "Unknown Source";

        if (event.containsKey("source")) {
            source = (String) event.get("source");
        }
        log.info("Invocation source: " + source);

        switch (source) {
            case "EventBridge":
                log.info("Handling periodic invocation from EventBridge");
                break;
            case "APIGateway":
                log.info("Triggered via API Gateway");
                break;
            case "WebApp":
                log.info("Invoked by the Web Application");
                break;
            case "LambdaTest":
                log.info("Invoked manually via the test event from lambda");
                break;
            default:
                log.warn("Invocation source unclear, proceed with caution");
                break;
        }

        String response = databaseSearchAndComparisonWithS3();
        log.info(response);
        return response;

    }

    private String databaseSearchAndComparisonWithS3(){
        String url = "jdbc:postgresql://" + dbHost + ":5432/" + dbDatabase;
        try (Connection connection = DriverManager.getConnection(url, dbUser, dbPassword);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT name FROM " + "public." + tableName)) {
            Set<S3ObjectSummary> objects = new HashSet<>(s3Client.listObjects(s3Bucket).getObjectSummaries());
            Set<String> resultSetList = new HashSet<>();

            while (resultSet.next()) {
                resultSetList.add(resultSet.getString("name"));
            }
            return checkIfDataIsConsistent(resultSetList, objects);
        } catch (Exception e) {
            log.error("Error processing validation: " + e.getMessage());
            return "Error processing validation: " + e.getMessage();
        }
    }

    private static String checkIfDataIsConsistent(Set<String> resultSetList, Set<S3ObjectSummary> objects){
        for (String image : resultSetList) {
            Set<?> exists = objects.stream().filter(s -> s.getKey().equals(image)).collect(Collectors.toSet());

            if (!exists.isEmpty()) {
                log.error("Data inconsistency found: " + image);
                return "Data inconsistency found: " + image;
            }
        }
        log.info("Data is consistent");
        return "Data is consistent";
    }
}
