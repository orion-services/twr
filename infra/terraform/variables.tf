variable "aws_region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "sa-east-1" # São Paulo
}

variable "project_name" {
  description = "Name/tag prefix used for all resources created by this stack."
  type        = string
  default     = "twr"
}

variable "instance_type" {
  description = <<-EOT
    EC2 instance type. t4g.* (Graviton/ARM) is cheaper than equivalent x86 (t3.*)
    instances. Production calls OpenAI for chat and embeddings, so t4g.small
    (2 vCPU / 2 GiB) is enough for twr + Postgres + Redis. t4g.small is included
    in the AWS free trial (750 hours/month) through 2026-12-31. Bump to
    t4g.medium if the box swaps heavily outside of deploys.
  EOT
  type        = string
  default     = "t4g.small" # 2 vCPU / 2 GiB RAM
}

variable "root_volume_size_gb" {
  description = "Size (GiB) of the root EBS volume (OS + Docker images)."
  type        = number
  default     = 20
}

variable "data_volume_size_gb" {
  description = <<-EOT
    Size (GiB) of the extra EBS data volume. Mounted at /data by user_data.sh and
    used for Postgres, Redis and Docker's data-root (image build cache). 10 GiB
    is the practical floor; smaller fills up on the first deploy.
  EOT
  type        = number
  default     = 10
}

variable "data_volume_type" {
  description = "EBS volume type for the data volume. gp3 is the cheapest general-purpose option."
  type        = string
  default     = "gp3"
}

variable "allowed_ssh_cidr_blocks" {
  description = <<-EOT
    Optional list of CIDR blocks allowed to SSH into the instance (port 22).
    Left empty by default — admin access is done via SSM Session Manager instead,
    so no inbound SSH port needs to be opened at all. Only set this if you specifically
    need SSH (e.g. for tooling that doesn't support SSM).
  EOT
  type        = list(string)
  default     = []
}

variable "vpc_id" {
  description = "VPC to deploy into. Leave null to use the account's default VPC."
  type        = string
  default     = null
}

variable "subnet_id" {
  description = "Subnet to deploy the instance into. Leave null to use the default VPC's default subnet."
  type        = string
  default     = null
}

variable "github_repo" {
  description = "GitHub repository (owner/name) the deploy workflow and optional self-hosted runner use."
  type        = string
  default     = "orion-services/twr"
}

variable "github_runner_labels" {
  description = "Comma-separated labels used only if the optional self-hosted runner still registers at boot."
  type        = string
  default     = "twr-prod"
}

variable "github_pat_ssm_parameter" {
  description = <<-EOT
    Name of the SSM Parameter Store SecureString holding a GitHub PAT. Still read
    at boot by the optional self-hosted runner registration. Deploy itself uses a
    GitHub-hosted runner and SSM, so this parameter is no longer required for CI.
  EOT
  type        = string
  default     = "/twr/github-runner-pat"
}

variable "github_oidc_provider_arn" {
  description = <<-EOT
    ARN of an existing GitHub Actions OIDC provider in this account. Leave null
    to create one. Set this if the account already has
    token.actions.githubusercontent.com (only one provider per URL is allowed).
  EOT
  type        = string
  default     = null
}
