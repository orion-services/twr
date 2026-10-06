# GitHub Actions (hosted runners) assume this role and send an SSM command to
# the twr instance. Replaces the self-hosted runner, which needs a PAT with
# Administration access the org is rejecting (HTTP 403).

resource "aws_iam_openid_connect_provider" "github" {
  count = var.github_oidc_provider_arn == null ? 1 : 0

  url            = "https://token.actions.githubusercontent.com"
  client_id_list = ["sts.amazonaws.com"]
  # GitHub's current OIDC CA thumbprints. AWS also accepts a dummy value
  # because it now verifies the cert chain itself.
  thumbprint_list = [
    "6938fd4d98bab03faadb97b34396831e3780aea1",
    "1c58a3a8518e8759bf075b76b750d4f2df264fcd"
  ]

  tags = {
    Project = var.project_name
  }
}

locals {
  github_oidc_provider_arn = coalesce(
    var.github_oidc_provider_arn,
    try(aws_iam_openid_connect_provider.github[0].arn, "")
  )
}

resource "aws_iam_role" "github_deploy" {
  name = "${var.project_name}-github-deploy"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Federated = local.github_oidc_provider_arn
      }
      # TagSession is required by aws-actions/configure-aws-credentials.
      # Match `repository` (always present) rather than `sub`, which orgs
      # often rewrite with a custom OIDC claim template.
      Action = [
        "sts:AssumeRoleWithWebIdentity",
        "sts:TagSession",
      ]
      Condition = {
        StringEquals = {
          "token.actions.githubusercontent.com:aud"        = "sts.amazonaws.com"
          "token.actions.githubusercontent.com:repository" = var.github_repo
        }
      }
    }]
  })

  tags = {
    Project = var.project_name
  }
}

resource "aws_iam_role_policy" "github_deploy_ssm" {
  name = "${var.project_name}-github-deploy-ssm"
  role = aws_iam_role.github_deploy.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = ["ssm:SendCommand"]
        Resource = [
          "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}::document/AWS-RunShellScript",
          aws_instance.twr.arn
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:GetCommandInvocation",
          "ssm:ListCommands",
          "ssm:ListCommandInvocations"
        ]
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = ["ec2:DescribeInstances"]
        Resource = "*"
      }
    ]
  })
}
