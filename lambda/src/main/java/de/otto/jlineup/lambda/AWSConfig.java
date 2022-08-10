package de.otto.jlineup.lambda;

import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.s3.S3Client;

public class AWSConfig {

    public static AwsCredentialsProvider defaultAwsCredentialsProvider(String profile) {
        return AwsCredentialsProviderChain
                .builder()
                .credentialsProviders(
                        // instance profile is also needed for people not using ecs but directly using ec2 instances!!
                        //ContainerCredentialsProvider.builder().build(),
                        //InstanceProfileCredentialsProvider.builder().build(),
                        //EnvironmentVariableCredentialsProvider.create(),
                        ProfileCredentialsProvider
                                .builder()
                                .profileName(profile)
                                .build())
                .build();
    }

    public static S3Client defaultS3Client(final AwsCredentialsProvider awsCredentialsProvider) {
        return S3Client
                .builder()
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

}
