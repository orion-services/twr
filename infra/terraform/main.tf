####
# Low-cost, single-EC2-instance infrastructure for twr.
#
# Everything (app + Postgres/pgvector + Redis + Ollama) runs as Docker Compose
# services on one Graviton (ARM) EC2 instance. No RDS, ElastiCache or ALB — those
# are the main cost drivers we're avoiding here. HTTPS termination is handled by
# Caddy (see ../../Caddyfile) directly on the instance, and admin access is done
# via SSM Session Manager instead of an SSH key pair, so the security group does
# not need an open port 22.
####

data "aws_vpc" "selected" {
  id      = var.vpc_id
  default = var.vpc_id == null ? true : null
}

data "aws_subnets" "selected" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.selected.id]
  }
}

data "aws_caller_identity" "current" {}
data "aws_partition" "current" {}

locals {
  subnet_id = coalesce(var.subnet_id, data.aws_subnets.selected.ids[0])

  github_pat_ssm_parameter_arn = "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/${trimprefix(var.github_pat_ssm_parameter, "/")}"
}

# Amazon Linux 2023, arm64 — matches the t4g (Graviton) instance family.
data "aws_ami" "al2023_arm64" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-2023.*-arm64"]
  }

  filter {
    name   = "architecture"
    values = ["arm64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# ------------------------------------------------------------------
# Security group — only 80/443 inbound (Caddy). No SSH port by default;
# use SSM Session Manager for shell access (see outputs.tf).
# ------------------------------------------------------------------
resource "aws_security_group" "twr" {
  name        = "${var.project_name}-sg"
  description = "twr: allow HTTP/HTTPS from the internet, admin access via SSM only"
  vpc_id      = data.aws_vpc.selected.id

  ingress {
    description = "HTTP (Caddy ACME challenge + redirect to HTTPS)"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS (Caddy)"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  dynamic "ingress" {
    for_each = length(var.allowed_ssh_cidr_blocks) > 0 ? [1] : []
    content {
      description = "Optional SSH (only if allowed_ssh_cidr_blocks is set)"
      from_port   = 22
      to_port     = 22
      protocol    = "tcp"
      cidr_blocks = var.allowed_ssh_cidr_blocks
    }
  }

  egress {
    description = "Allow all outbound (image pulls, OpenAI/WhatsApp APIs, scraping)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "${var.project_name}-sg"
    Project = var.project_name
  }
}

# ------------------------------------------------------------------
# IAM role — grants SSM Session Manager access, plus read access to the
# single SSM parameter holding the GitHub PAT used to register the
# self-hosted Actions runner at boot (see user_data.sh.tftpl).
# ------------------------------------------------------------------
resource "aws_iam_role" "twr_instance" {
  name = "${var.project_name}-instance-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })

  tags = {
    Project = var.project_name
  }
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.twr_instance.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# The default aws/ssm KMS key's policy already allows decryption through SSM
# for principals in the account, so no extra kms:Decrypt statement is needed.
resource "aws_iam_role_policy" "github_runner_pat" {
  name = "${var.project_name}-github-runner-pat"
  role = aws_iam_role.twr_instance.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ssm:GetParameter"]
      Resource = local.github_pat_ssm_parameter_arn
    }]
  })
}

resource "aws_iam_instance_profile" "twr_instance" {
  name = "${var.project_name}-instance-profile"
  role = aws_iam_role.twr_instance.name
}

# ------------------------------------------------------------------
# EC2 instance
# ------------------------------------------------------------------
resource "aws_instance" "twr" {
  ami                    = data.aws_ami.al2023_arm64.id
  instance_type          = var.instance_type
  subnet_id              = local.subnet_id
  vpc_security_group_ids = [aws_security_group.twr.id]
  iam_instance_profile   = aws_iam_instance_profile.twr_instance.name
  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    project_name         = var.project_name
    aws_region           = var.aws_region
    github_repo          = var.github_repo
    github_runner_labels = var.github_runner_labels
    github_pat_parameter = var.github_pat_ssm_parameter
  })
  # Re-run user_data if it changes (Terraform otherwise ignores user_data updates
  # on existing instances). Remove this if you don't want instance replacement on
  # every script tweak.
  user_data_replace_on_change = true

  root_block_device {
    volume_size           = var.root_volume_size_gb
    volume_type           = "gp3"
    delete_on_termination = true
  }

  metadata_options {
    http_tokens = "required" # enforce IMDSv2
  }

  tags = {
    Name    = var.project_name
    Project = var.project_name
  }

  # user_data reads the GitHub PAT from SSM on first boot.
  depends_on = [aws_iam_role_policy.github_runner_pat]
}

# Extra EBS volume for Postgres/Redis/Ollama data, kept independent from the
# root volume/instance lifecycle.
resource "aws_ebs_volume" "data" {
  availability_zone = aws_instance.twr.availability_zone
  size              = var.data_volume_size_gb
  type              = var.data_volume_type

  tags = {
    Name    = "${var.project_name}-data"
    Project = var.project_name
  }
}

resource "aws_volume_attachment" "data" {
  device_name = "/dev/sdf" # surfaces as /dev/nvme1n1 on Nitro instances (t4g); handled in user_data.sh
  volume_id   = aws_ebs_volume.data.id
  instance_id = aws_instance.twr.id
}

# ------------------------------------------------------------------
# Elastic IP — stable public address to point DNS at.
# ------------------------------------------------------------------
resource "aws_eip" "twr" {
  instance = aws_instance.twr.id
  domain   = "vpc"

  tags = {
    Name    = "${var.project_name}-eip"
    Project = var.project_name
  }
}
