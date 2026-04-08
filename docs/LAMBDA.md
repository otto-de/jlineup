# AWS Lambda instructions

JLineup can delegate the browser work to AWS Lambda instead of running a browser locally.
Each screenshot context (one URL + path + device configuration) is executed as a separate
Lambda invocation in parallel, which can significantly speed up large JLineup runs.

The Lambda function itself is a Docker image that runs inside AWS. You deploy it once and
then point either the CLI or the web server at it. Both variants discover the S3 bucket
automatically from the Lambda function's own configuration, so you do not need to configure
the bucket on the caller side.

## How it works

```
  CLI or Web Server
  ──────────────────
  1. Fan out one Lambda invocation per screenshot context (all in parallel)
  2. Wait for all invocations to complete
  3. Download results from S3
  4. Generate the comparison report locally
                          │
              ┌───────────▼────────────┐
              │   Lambda function      │
              │  (Docker image, 3 GB)  │
              │  ┌──────────────────┐  │
              │  │  Headless Chrome │  │
              │  └──────────────────┘  │
              │  Take screenshots      │
              │  Upload to S3          │
              └────────────────────────┘
```

## Deploying the Lambda function

The Lambda function is packaged as a Docker image. You need to build it, push it to Amazon
ECR, and create a Lambda function from it.

### Prerequisites

- Docker
- AWS CLI configured with sufficient permissions (ECR, Lambda, S3, IAM)
- An S3 bucket for storing screenshots

### Build and push the Docker image

```bash
# Build the JLineup Lambda module first
./gradlew :jlineup-lambda:build assemble -x test

# Build the Docker image
cd lambda
docker build -t jlineup-lambda .

# Push to ECR (replace placeholders with your values)
aws ecr get-login-password --region eu-central-1 | \
  docker login --username AWS --password-stdin <account-id>.dkr.ecr.eu-central-1.amazonaws.com

docker tag jlineup-lambda:latest \
  <account-id>.dkr.ecr.eu-central-1.amazonaws.com/jlineup-lambda:latest

docker push <account-id>.dkr.ecr.eu-central-1.amazonaws.com/jlineup-lambda:latest
```

### Create the Lambda function

Create a Lambda function from the ECR image with the following recommended settings:

| Setting                    | Recommended value                                      |
|----------------------------|--------------------------------------------------------|
| Runtime                    | Container image                                        |
| Handler                    | `de.otto.jlineup.lambda.JLineupHandler::handleRequest` |
| Memory                     | 2048 MB                                                |
| Ephemeral storage (`/tmp`) | 1024 MB                                                |
| Timeout                    | 300 seconds                                            |
| Architecture               | x86_64                                                 |

The Lambda function needs an IAM role with:
- `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject`, `s3:ListBucket` on your S3 bucket

### Configure environment variables on the Lambda function

| Variable                   | Required | Description                                             |
|----------------------------|----------|---------------------------------------------------------|
| `JLINEUP_LAMBDA_S3_BUCKET` | **Yes**  | Name of the S3 bucket where screenshots are stored      |
| `JLINEUP_LAMBDA_S3_PREFIX` | No       | Optional key prefix within the bucket (e.g. `jlineup/`) |

The caller discovers these values automatically at runtime by reading the Lambda function's
own configuration via the AWS API — you do not need to set them on the CLI or web server.

## Using Lambda with the CLI

Download the CLI Lambda variant, which bundles both the CLI and the Lambda client:

`wget https://repo1.maven.org/maven2/de/otto/jlineup-cli-lambda/5.2.1/jlineup-cli-lambda-5.2.1.jar -O jlineup.jar`

The CLI Lambda variant works exactly like the regular CLI, with these additional flags:

| Flag                     | Short | Default          | Description                                      |
|--------------------------|-------|------------------|--------------------------------------------------|
| `--lambda-function-name` | `-F`  | `jlineup-lambda` | Name or ARN of the Lambda function to invoke     |
| `--lambda-aws-profile`   | `-P`  | `default`        | AWS credentials profile                          |
| `--lambda-aws-region`    | `-L`  | `eu-central-1`   | AWS region where the Lambda function is deployed |

### Example

```bash
# Before run — delegates all screenshot taking to Lambda
java -jar jlineup.jar \
  --config lineup.json \
  --step before \
  --lambda-function-name my-jlineup-lambda \
  --lambda-aws-profile my-aws-profile

# After run — same flags
java -jar jlineup.jar \
  --config lineup.json \
  --step after \
  --lambda-function-name my-jlineup-lambda \
  --lambda-aws-profile my-aws-profile
```

The report is generated locally after all Lambda invocations have completed and results
have been downloaded from S3. The output structure is identical to a local run.

### Environment variables (CLI)

Instead of CLI flags you can also use environment variables:

| Variable                       | Description                          |
|--------------------------------|--------------------------------------|
| `JLINEUP_LAMBDA_FUNCTION_NAME` | Name or ARN of the Lambda function   |
| `JLINEUP_AWS_PROFILE`          | AWS credentials profile              |
| `JLINEUP_LAMBDA_AWS_REGION`    | AWS region (default: `eu-central-1`) |

## Using Lambda with the web server

Download the web Lambda variant, which bundles both the web server and the Lambda client:

`wget https://repo1.maven.org/maven2/de/otto/jlineup-web-lambda/5.2.1/jlineup-web-lambda-5.2.1.jar -O jlineup-web.jar`

Start it the same way as the regular web server:

```bash
java -jar jlineup-web.jar
```

Configure the Lambda function name via Spring application properties or an environment
variable. All jobs submitted via the REST API will then delegate screenshot taking to Lambda.

### Configuration

**`application.yml`:**
```yaml
jlineup:
  lambda:
    function-name: my-jlineup-lambda
    aws-region: us-east-1  # optional, defaults to eu-central-1
```

**Or via environment variable:**
```bash
export JLINEUP_LAMBDA_FUNCTION_NAME=my-jlineup-lambda
export JLINEUP_LAMBDA_AWS_REGION=us-east-1  # optional
java -jar jlineup-web.jar
```

The web server otherwise behaves identically to the regular web server — see
[WEB.md](WEB.md) for REST API documentation and all other configuration options.

## IAM permissions required by the caller

The machine running the CLI or the web server needs AWS credentials with the following
permissions:

```json
{
  "Effect": "Allow",
  "Action": [
    "lambda:InvokeFunction",
    "lambda:GetFunction",
    "s3:GetObject",
    "s3:ListBucket",
    "s3:DeleteObject"
  ],
  "Resource": [
    "arn:aws:lambda:*:*:function:my-jlineup-lambda",
    "arn:aws:s3:::my-jlineup-bucket",
    "arn:aws:s3:::my-jlineup-bucket/*"
  ]
}
```

`lambda:GetFunction` is used to read the S3 bucket name and prefix from the Lambda
function's own environment variables. `s3:DeleteObject` is used to clean up the uploaded
screenshots from S3 after they have been downloaded.
