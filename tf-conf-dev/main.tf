provider "aws" {
  access_key                  = "mock_access_key"
  secret_key                  = "mock_secret_key"
  region                      = "us-east-1"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true
  s3_force_path_style = true

  endpoints {
    apigateway     = "http://localhost:4566"
    cloudformation = "http://localhost:4566"
    cloudwatch     = "http://localhost:4566"
    dynamodb       = "http://localhost:4566"
    es             = "http://localhost:4566"
    firehose       = "http://localhost:4566"
    iam            = "http://localhost:4566"
    kinesis        = "http://localhost:4566"
    lambda         = "http://localhost:4566"
    route53        = "http://localhost:4566"
    redshift       = "http://localhost:4566"
    s3             = "http://localhost:4566"
    secretsmanager = "http://localhost:4566"
    ses            = "http://localhost:4566"
    sns            = "http://localhost:4566"
    sqs            = "http://localhost:4566"
    ssm            = "http://localhost:4566"
    stepfunctions  = "http://localhost:4566"
    sts            = "http://localhost:4566"
  }
}

resource "aws_sqs_queue" "battle_queue_in" {
  name = "battle_input_queue"
}

resource "aws_sqs_queue" "battle_queue_out" {
  name = "battle_output_queue"
}

resource "aws_s3_bucket" "lambda_files" {
  bucket = "robot-runner-lambda"
  acl = "public-read"
}

resource "aws_s3_bucket_object" "object" {
  bucket = aws_s3_bucket.lambda_files.bucket
  key    = "lambda.zip"
  source = "../../logic/target/debug/lambda.zip"
}

resource "aws_lambda_function" "battle_runner" {
  depends_on = [aws_s3_bucket.lambda_files, aws_s3_bucket_object.object]
  s3_bucket = aws_s3_bucket.lambda_files.id
  s3_key = aws_s3_bucket_object.object.key
  function_name = "battle-runner"
  runtime = "provided"
  timeout = var.lambda_timeout
  memory_size = var.lambda_memory_size
  handler = "doesnt.matter"
  role = aws_iam_role.iam_for_lambda.arn
  environment {
    variables = {
      BATTLE_QUEUE_OUT_URL = aws_sqs_queue.battle_queue_out.id
    }
  }
}

resource "aws_iam_policy" "lambda" {
  name = "lambda_logging"
  path = "/"
  description = "IAM policy for logging from a lambda"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "sqs:ReceiveMessage",
                "sqs:DeleteMessage",
                "sqs:GetQueueAttributes",
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": "*"
      }
  ]
}
EOF
}

resource "aws_iam_role_policy_attachment" "lambda" {
  role = aws_iam_role.iam_for_lambda.name
  policy_arn = aws_iam_policy.lambda.arn
}

resource "aws_iam_role" "iam_for_lambda" {
  name = "iam_for_lambda"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

resource "aws_lambda_event_source_mapping" "input_queue_mapping" {
  event_source_arn = aws_sqs_queue.battle_queue_in.arn
  function_name = aws_lambda_function.battle_runner.arn
}

