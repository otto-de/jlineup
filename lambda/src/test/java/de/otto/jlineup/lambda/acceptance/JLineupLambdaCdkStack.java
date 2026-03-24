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
 * CDK stack that deploys the JLineup Docker Lambda from the {@code lambda/Dockerfile}
 * together with a dedicated S3 bucket for screenshot upload/download.
 * Used exclusively by {@link LambdaAcceptanceTest}.
 */
public class JLineupLambdaCdkStack extends Stack {

    public static final String OUTPUT_FUNCTION_NAME = "FunctionName";
    public static final String OUTPUT_BUCKET_NAME   = "BucketName";

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

        // --- Docker Image Lambda built from lambda/Dockerfile ---
        DockerImageFunction function = DockerImageFunction.Builder.create(this, "JLineupDockerLambda")
                .functionName(id + "-fn")
                // Docker build context = lambda/ module dir (contains Dockerfile + build/classes + build/output/lib)
                .code(DockerImageCode.fromImageAsset(dockerContextPath))
                .memorySize(3008)
                // Lambda's /tmp is used for working dir and Chrome profile – 1 GB avoids "no space left" errors
                .ephemeralStorageSize(Size.mebibytes(1024))
                .timeout(Duration.seconds(300))
                .environment(Map.of(
                        "JLINEUP_LAMBDA_S3_BUCKET", resultsBucket.getBucketName(),
                        "JLINEUP_LAMBDA_S3_PREFIX", "lamba-screenshots-prefix"
                ))
                .build();

        resultsBucket.grantReadWrite(function);

        // --- Stack outputs consumed by the acceptance test ---
        CfnOutput.Builder.create(this, OUTPUT_FUNCTION_NAME)
                .value(function.getFunctionName())
                .exportName(id + "-" + OUTPUT_FUNCTION_NAME)
                .build();

        CfnOutput.Builder.create(this, OUTPUT_BUCKET_NAME)
                .value(resultsBucket.getBucketName())
                .exportName(id + "-" + OUTPUT_BUCKET_NAME)
                .build();
    }
}

