package de.otto.jlineup.clilambda.acceptance;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Size;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.AssetImageCodeProps;
import software.amazon.awscdk.services.lambda.DockerImageCode;
import software.amazon.awscdk.services.lambda.DockerImageFunction;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        // Read Chrome major version from .chrome-version file in project root
        String chromeMajorVersion = readChromeMajorVersion(dockerContextPath);
        // Read Amazon Linux 2023 version from .al2023-version file in project root
        String al2023Version = readAl2023Version(dockerContextPath);
        Map<String, String> dockerBuildArgs = Map.of(
                "CHROME_MAJOR_VERSION", chromeMajorVersion,
                "AMI_LINUX_2023_VERSION", al2023Version
        );

        // --- S3 bucket for screenshot results ---
        Bucket resultsBucket = Bucket.Builder.create(this, "ResultsBucket")
                .removalPolicy(RemovalPolicy.DESTROY)
                .autoDeleteObjects(true)
                .build();

        // --- Docker Image Lambda built from lambda/Dockerfile ---
        DockerImageFunction function = DockerImageFunction.Builder.create(this, "JLineupDockerLambda")
                .functionName(id + "-fn")
                .code(DockerImageCode.fromImageAsset(dockerContextPath, AssetImageCodeProps.builder()
                        .buildArgs(dockerBuildArgs)
                        .build()))
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

    /**
     * Reads the Chrome major version from the .chrome-version file in the project root.
     * The file contains the full version (e.g., "150.0.7871.128"), and we extract the major version.
     */
    private String readChromeMajorVersion(String dockerContextPath) {
        try {
            // dockerContextPath is the lambda directory, so go up one level to find .chrome-version
            Path chromeVersionFile = Path.of(dockerContextPath).getParent().resolve(".chrome-version");
            String fullVersion = Files.readString(chromeVersionFile).trim();
            // Extract major version (first segment before the dot)
            String majorVersion = fullVersion.split("\\.")[0];
            return majorVersion;
        } catch (IOException e) {
            // Fall back to default if file not found
            return "150";
        }
    }

    /**
     * Reads the Amazon Linux 2023 version from the .al2023-version file in the project root.
     */
    private String readAl2023Version(String dockerContextPath) {
        try {
            Path versionFile = Path.of(dockerContextPath).getParent().resolve(".al2023-version");
            return Files.readString(versionFile).trim();
        } catch (IOException e) {
            // Fall back to default if file not found
            return "2023.12.20260710";
        }
    }
}
