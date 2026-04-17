package de.otto.jlineup.lambda.acceptance;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.DockerImageCode;
import software.amazon.awscdk.services.lambda.DockerImageFunction;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

import java.util.Map;

/**
 * CDK stack that deploys the JLineup Docker Lambdas together with a dedicated
 * S3 bucket for screenshot upload/download.
 * <p>
 * Two Lambda functions are created:
 * <ul>
 *   <li><strong>Default</strong> – built from {@code lambda/Dockerfile} (Chrome + Firefox on Amazon Linux 2023)</li>
 *   <li><strong>WebKit</strong> – built from {@code lambda/Dockerfile.webkit} (WebKit on Fedora)</li>
 * </ul>
 * Used exclusively by {@link LambdaAcceptanceTest}.
 */
public class JLineupLambdaCdkStack extends Stack {

    public static final String OUTPUT_FUNCTION_NAME        = "FunctionName";
    public static final String OUTPUT_WEBKIT_FUNCTION_NAME = "WebKitFunctionName";
    public static final String OUTPUT_BUCKET_NAME          = "BucketName";

    public JLineupLambdaCdkStack(Construct scope,
                                 String   id,
                                 String   dockerContextPath,
                                 String   account,
                                 String   region) {
        super(scope, id, StackProps.builder()
                .env(Environment.builder()
                        .account(account)
                        .region(region)
                        .build())
                .build());

        // --- S3 bucket for screenshot results ---
        Bucket resultsBucket = Bucket.Builder.create(this, "ResultsBucket")
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .build();

        Map<String, String> lambdaEnv = Map.of(
                "JLINEUP_LAMBDA_S3_BUCKET", resultsBucket.getBucketName(),
                "JLINEUP_LAMBDA_S3_PREFIX", "lamba-screenshots-prefix"
        );

        // --- Default Lambda (Chrome + Firefox) from lambda/Dockerfile ---
        DockerImageFunction defaultFunction = DockerImageFunction.Builder.create(this, "JLineupDockerLambda")
                .functionName(id + "-fn")
                .code(DockerImageCode.fromImageAsset(dockerContextPath))
                .memorySize(3008)
                .ephemeralStorageSize(Size.mebibytes(1024))
                .timeout(Duration.seconds(300))
                .environment(lambdaEnv)
                .build();

        resultsBucket.grantReadWrite(defaultFunction);

        // --- WebKit Lambda from lambda/Dockerfile.webkit (Fedora-based) ---
        DockerImageFunction webkitFunction = DockerImageFunction.Builder.create(this, "JLineupWebKitLambda")
                .functionName(id + "-webkit-fn")
                .code(DockerImageCode.fromImageAsset(dockerContextPath, software.amazon.awscdk.services.lambda.AssetImageCodeProps.builder()
                        .file("Dockerfile.webkit")
                        .build()))
                .memorySize(3008)
                .ephemeralStorageSize(Size.mebibytes(1024))
                .timeout(Duration.seconds(300))
                .environment(lambdaEnv)
                .build();

        resultsBucket.grantReadWrite(webkitFunction);

        // --- Stack outputs consumed by the acceptance test ---
        CfnOutput.Builder.create(this, OUTPUT_FUNCTION_NAME)
                .value(defaultFunction.getFunctionName())
                .exportName(id + "-" + OUTPUT_FUNCTION_NAME)
                .build();

        CfnOutput.Builder.create(this, OUTPUT_WEBKIT_FUNCTION_NAME)
                .value(webkitFunction.getFunctionName())
                .exportName(id + "-" + OUTPUT_WEBKIT_FUNCTION_NAME)
                .build();

        CfnOutput.Builder.create(this, OUTPUT_BUCKET_NAME)
                .value(resultsBucket.getBucketName())
                .exportName(id + "-" + OUTPUT_BUCKET_NAME)
                .build();
    }
}

