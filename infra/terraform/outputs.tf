output "instance_id" {
  description = "EC2 instance ID (use with `aws ssm start-session`)."
  value       = aws_instance.twr.id
}

output "public_ip" {
  description = "Elastic IP — point your DNS A record here."
  value       = aws_eip.twr.public_ip
}

output "public_dns" {
  description = "AWS-assigned public DNS name for the instance's Elastic IP (e.g. ec2-1-2-3-4.compute-1.amazonaws.com). Usable as-is for DOMAIN in .env if you don't own a custom domain — Let's Encrypt can issue a certificate for it just like any other public hostname."
  value       = aws_eip.twr.public_dns
}

output "sslip_domain" {
  description = "Ready-to-use sslip.io hostname that resolves to the Elastic IP without any DNS setup. Usable as-is for DOMAIN in .env — see docs/aws.md."
  value       = "${replace(aws_eip.twr.public_ip, ".", "-")}.sslip.io"
}

output "ssm_connect_command" {
  description = "Command to open a shell on the instance via SSM Session Manager (no SSH key/port needed)."
  value       = "aws ssm start-session --target ${aws_instance.twr.id} --region ${var.aws_region}"
}

output "security_group_id" {
  description = "Security group attached to the instance."
  value       = aws_security_group.twr.id
}
