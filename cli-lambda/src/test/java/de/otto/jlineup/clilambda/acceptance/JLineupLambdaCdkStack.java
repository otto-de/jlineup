package de.otto.jlineup.clilambda.acceptance;

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
 * CDK stack that deploys the JLineup Docker Lambda together with a dedicated S3 bucket.
 *
 * <p>This is a copy of the stack definition from the {@code jlineup-lambda} module's
 * acceptance test package.  It must be kept in sync with
 * {@code de.otto.jlineup.lambda.acceptance.JLineupLambdaCdkStack} so that both test
 * modules deploy and reference the same logical stack.
 *
 * <p>Used exclusively by {@link CliLambdaAcceptanceTest}.
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
                .code(DockerImageCode.fromImageAsset(dockerContextPath))
                .memorySize(3008)
                .ephemeralStorageSize(Size.mebibytes(1024))
                .timeout(Duration.seconds(300))
                .environment(Map.of(
                        "JLINEUP_LAMBDA_S3_BUCKET", resultsBucket.getBucketName(),
                        "JLINEUP_LAMBDA_S3_PREFIX", "lamba-screenshots-prefix"
                ))
                .build();

        resultsBucket.grantReadWrite(function);

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
