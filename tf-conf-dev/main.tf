provider "aws" {
  region = var.aws_region
}


resource "aws_sqs_queue" "battle_queue_in" {
  name = "dev-battle-input-queue"
}

resource "aws_sqs_queue" "battle_queue_out" {
  name = "dev-battle-output-queue"
}

//resource "aws_sqs_queue" "battle_queue_error" {
//  name = "dev-battle-output-queue"
//}

resource "aws_s3_bucket" "lambda" {
  bucket = "dev-battle-runner"
  acl = "public-read"
}

resource "aws_s3_bucket_object" "lambda" {
  bucket = aws_s3_bucket.lambda.bucket
  key    = "lambda.zip"
  source = "../../logic/target/debug/lambda.zip"
}

resource "aws_lambda_function" "battle_runner" {
  depends_on = [aws_s3_bucket_object.lambda]
  s3_bucket = aws_s3_bucket.lambda.id
  s3_key = aws_s3_bucket_object.lambda.key
  function_name = "dev-battle-runner"
  runtime = "provided"
  timeout = var.lambda_timeout
  memory_size = var.lambda_memory_size
  handler = "doesnt.matter"
  role = aws_iam_role.lambda.arn
  environment {
    variables = {
      RUST_BACKTRACE = 1
    }
  }
}

resource "aws_lambda_event_source_mapping" "battle_queue_in" {
  event_source_arn = aws_sqs_queue.battle_queue_in.arn
  function_name = aws_lambda_function.battle_runner.arn
}

resource "aws_lambda_function_event_invoke_config" "battle_queue_out" {
  function_name = aws_lambda_function.battle_runner.function_name

  destination_config {
    on_success {
      destination = aws_sqs_queue.battle_queue_out.arn
    }
  }
}


resource "aws_iam_policy" "lambda" {
  name = "dev-lambda-policy"
  path = "/"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "sqs:ReceiveMessage",
                "sqs:DeleteMessage",
                "sqs:SendMessage",
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
  role = aws_iam_role.lambda.name
  policy_arn = aws_iam_policy.lambda.arn
}

resource "aws_iam_role" "lambda" {
  name = "dev-lambda-iam"

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

