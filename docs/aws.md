# Deploy do twr na AWS (infra de baixo custo, EC2 única)

Este guia cobre o deploy do twr em uma única instância EC2, rodando o app,
Postgres+pgvector e Redis via Docker Compose, com HTTPS automático via Caddy.
É a opção mais barata: sem RDS, ElastiCache ou ALB. Em produção o chat
(`gpt-6.1-sol`) e os embeddings (`text-embedding-3-small`) usam a API da
OpenAI — não é preciso rodar Ollama nem o modelo local de embedding na
instância (isso só é usado em desenvolvimento local).

A infraestrutura é criada na região **São Paulo (`sa-east-1`)**, e o app roda
em **Java 25** (Amazon Corretto 25 no host para o build, imagem
`ubi9/openjdk-25-runtime` no container).

Custo aproximado (região `sa-east-1`): uma `t4g.small` (2 vCPU / 2 GiB). O
compute dessa família entra no trial da AWS de até 750 horas/mês até 31 de
dezembro de 2026, então uma instância 24/7 fica sem cobrança de CPU nesse
período. A instância usa crédito de CPU `standard`: passou da linha de base, a
CPU desacelera, em vez de gerar cobrança de crédito extra. Depois do trial, a
mesma máquina fica em torno de US$ 20/mês sob demanda (ou cerca de US$ 11/mês
com Reserved de 1 ano). Os 30 GiB de EBS gp3 (20 GiB de root + 10 GiB de
dados) ficam em torno de US$ 3 a 4/mês. O IPv4 público em uso custa cerca de
US$ 0,005/hora (~US$ 3,60/mês), associado ou não a um Elastic IP. Piso com o
site no ar o tempo todo, até o fim de 2026: disco + IPv4, cerca de US$ 7/mês.

## Pré-requisitos

- Conta AWS com permissão para criar EC2, IAM roles, EIP e Security Groups
- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html) configurado (`aws configure`)
- [Session Manager plugin do AWS CLI](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html) instalado (necessário para `aws ssm start-session`)
- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.5
- Um domínio (ou subdomínio) que você controla, para apontar ao Elastic IP e emitir o certificado HTTPS — **opcional**: se não tiver um, veja "Não quero um domínio próprio" no passo 2

## 0. Guardar o token do GitHub no SSM (passo único)

A instância registra sozinha o runner do GitHub Actions no primeiro boot
(veja o passo 8). Para isso ela lê um GitHub PAT do SSM Parameter Store.

1. No GitHub, crie um **fine-grained personal access token** com acesso
   somente ao repositório `orion-services/twr` e a permissão
   **Administration: Read and write**.
2. Salve o token no SSM, na mesma região da infraestrutura:

```bash
aws ssm put-parameter \
  --region sa-east-1 \
  --name /twr/github-runner-pat \
  --type SecureString \
  --value '<GITHUB_PAT>'
# para trocar o token depois: acrescente --overwrite
```

Se o parâmetro não existir no boot, a instância sobe normalmente, sem runner
(um aviso aparece em `/var/log/twr-user-data.log`).

## 1. Provisionar a infraestrutura com Terraform

```bash
cd infra/terraform
terraform init
terraform plan
terraform apply
```

Isso cria, em `sa-east-1`:
- 1 instância EC2 `t4g.small` (Amazon Linux 2023, ARM/Graviton, crédito de CPU `standard`, 2 GiB de swap) com Docker, Compose, buildx e Amazon Corretto 25
- Security Group com apenas as portas 80 e 443 abertas (sem porta 22 — acesso administrativo via SSM)
- IAM role com a policy `AmazonSSMManagedInstanceCore` (acesso via Session Manager) e permissão de leitura somente no parâmetro `/twr/github-runner-pat`
- Runner self-hosted do GitHub Actions (label `twr-prod`) instalado como serviço em `/opt/actions-runner`
- Volume EBS extra (10 GiB) para dados do Postgres/Redis e para as imagens Docker
- Elastic IP associado à instância

Ao final, anote os outputs:

```bash
terraform output
# instance_id, public_ip, public_dns, sslip_domain, ssm_connect_command, security_group_id
```

Para customizar (tipo de instância, região, tamanho de disco), copie
`terraform.tfvars.example` para `terraform.tfvars` e ajuste antes do `apply`.

## 2. Apontar o DNS

Crie um registro `A` no seu provedor de DNS apontando o domínio/subdomínio
escolhido (ex.: `twr.example.com`) para o `public_ip` retornado pelo
Terraform. Aguarde a propagação antes do passo 4 (o Caddy precisa resolver o
domínio para emitir o certificado Let's Encrypt).

### Não quero um domínio próprio — pode ser uma URL gerada?

Sim. O Caddy só precisa de um **hostname público que resolva para o IP da
instância** — não precisa ser um domínio que você registrou. O ACME
(Let's Encrypt) valida apenas que você controla o servidor respondendo
naquele host, não a "posse" do domínio. Duas opções prontas, sem nenhum
passo de DNS manual, usando outputs do próprio Terraform:

```bash
terraform output sslip_domain
# ex: 52-91-12-34.sslip.io  (serviço público gratuito, resolve pro IP embutido no nome)

terraform output public_dns
# ex: ec2-52-91-12-34.sa-east-1.compute.amazonaws.com  (hostname que a própria AWS já atribui ao IP)
```

Use qualquer um dos dois como `DOMAIN` no `.env` (passo 4) — o restante do
guia não muda. Pule este passo 2 (não precisa criar registro `A` em lugar
nenhum) e vá direto para o passo 3.

## 3. Conectar na instância via SSM Session Manager

Não há chave SSH nem porta 22 aberta — a conexão é feita via SSM:

```bash
aws ssm start-session --target <instance_id> --region <aws_region>
```

(o comando exato está no output `ssm_connect_command` do Terraform)

## 4. Clonar o repositório e configurar os secrets

Dentro da sessão SSM (já como `ec2-user` após `sudo su - ec2-user` ou usando
`sudo -u ec2-user -i`):

```bash
cd /opt/twr
git clone https://github.com/orion-services/twr.git .
cp .env.example .env
nano .env   # preencha DOMAIN, ACME_EMAIL, POSTGRES_PASSWORD, WHATSAPP_*, OPENAI_API_KEY
```

Variáveis obrigatórias em `.env`:
- `DOMAIN` — o domínio apontado no passo 2 (ex.: `twr.example.com`)
- `ACME_EMAIL` — e-mail usado pelo Caddy no registro do Let's Encrypt
- `POSTGRES_PASSWORD` — senha forte para o banco (compartilhado entre `twr` e `orion-users`)
- `OPENAI_API_KEY` — usada como modelo de chat em produção (`gpt-6.1-sol`); gere em
  [platform.openai.com](https://platform.openai.com/api-keys)
- `VITE_ORION_USERS_URL` — **`https://<DOMAIN>/orion-users`** (mesmo domínio/TLS do app,
  ver `Caddyfile`). Precisa do valor final de `DOMAIN` já definido, pois é embutido no
  bundle do frontend no momento do build (`docker compose build`).
- `QUARKUS_MAILER_FROM` e `QUARKUS_MAILER_USERNAME` — endereço Gmail usado pelo
  Orion Users (o padrão upstream é `devoriontest@gmail.com`)
- `QUARKUS_MAILER_PASSWORD` — senha de aplicativo do Google (nunca a senha normal
  da conta)
- `ORION_USERS_EMAIL_VALIDATION_URL` —
  **`https://<DOMAIN>/orion-users/users/validateEmail`**, link público incluído nos
  e-mails de confirmação

Variáveis opcionais (deixe em branco/padrão para desabilitar a feature ou usar o default):
- `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_VERIFY_TOKEN`, `WHATSAPP_APP_SECRET`
- `ORION_USERS_HOST_PORT` (padrão `8082`) — acesso direto ao Orion Users pela instância,
  útil para depuração manual (o Security Group só abre 80/443/22, então isso não expõe
  a porta para a internet)
- `QUARKUS_MAILER_MOCK` (padrão `false`) — use `true` apenas para desabilitar o envio
  real de e-mails
- `VITE_GOOGLE_CLIENT_ID` — habilita "Entrar com Google" na tela de login

## 5. Build e subida do stack

O `src/main/docker/Dockerfile.jvm` compila o frontend (Vue) e o backend (Quarkus) a
partir do código-fonte — não é preciso rodar `./mvnw package` manualmente antes.

```bash
sudo usermod -aG docker ec2-user   # se ainda não estiver no grupo docker (relogar depois)
newgrp docker

docker compose -p twr up -d --build
```

Isso sobe toda a stack, incluindo o `orion-users` (serviço de login/cadastro/2FA,
construído diretamente de `github.com/orion-services/users`, compartilhando o mesmo
Postgres do `twr`).

O `-p twr` fixa o nome do projeto Compose — importante para que o deploy
automático via CI (passo 8) reutilize os mesmos containers/volumes, mesmo
rodando a partir de um diretório de checkout diferente.

Acompanhe os logs até o app subir (a ingestão de documentos/scraping no
startup pode levar 1-2 minutos):

```bash
docker compose -p twr logs -f twr
```

## 6. Validar

```bash
curl -I https://twr.example.com/
```

Deve responder `200 OK` com certificado válido (emitido automaticamente pelo
Caddy). Teste também a interface web abrindo o domínio no navegador.

## 7. Configurar o webhook do WhatsApp (opcional)

Se for usar a integração com WhatsApp, configure no painel do Meta for
Developers o webhook apontando para:

```
https://twr.example.com/webhook/whatsapp
```

Usando o `WHATSAPP_VERIFY_TOKEN` definido no `.env`.

## 8. CI/CD com GitHub Actions

Todo **push na branch `main` faz rebuild e restart automático do container
`twr` na EC2**.

A abordagem usada é um **self-hosted runner do GitHub Actions rodando na
própria instância EC2** — o workflow executa localmente na máquina, com as
mesmas permissões do deploy manual. Não é preciso guardar credenciais AWS
como secret no GitHub.

### 8.1 Registro automático do runner

O runner é registrado sozinho no primeiro boot da instância pelo
`infra/terraform/user_data.sh.tftpl`, que:

1. lê o PAT do parâmetro SSM `/twr/github-runner-pat` (passo 0);
2. troca o PAT por um token de registro de curta duração na API do GitHub;
3. baixa a versão mais recente do runner Linux ARM64 em `/opt/actions-runner`;
4. registra o runner em `orion-services/twr` com o label `twr-prod` e o
   instala como serviço systemd (sobrevive a reboots).

Para conferir: **Settings** do repositório -> **Actions** -> **Runners** deve
listar o runner `twr-<hostname>` como *Idle*. Na instância, o log fica em
`/var/log/twr-user-data.log`.

Se o parâmetro não existia no boot (ou o PAT estava sem permissão
Administration), crie ou corrija o parâmetro e reexecute só o registro,
dentro da sessão SSM:

```bash
sudo bash /var/lib/cloud/instance/scripts/part-001
```

### 8.2 Workflow

Já existe em [`.github/workflows/deploy.yml`](../.github/workflows/deploy.yml):
dispara em todo push na `main` (ou manualmente, em **Actions -> Deploy to AWS
-> Run workflow**), valida a configuração do Compose, reconstrói `twr` e
`orion-users` e recria o `caddy` para carregar eventuais mudanças no
`Caddyfile`. O workflow reutiliza o `.env` já configurado em `/opt/twr/.env`.

### 8.3 Nota de segurança

Um self-hosted runner executa o código de qualquer push na branch que ele
observa, com as credenciais da própria máquina. Mantenha *branch protection*
na `main`.

## Operação do dia a dia

| Ação | Comando |
|------|---------|
| Ver logs | `docker compose -p twr logs -f [servico]` |
| Reiniciar um serviço | `docker compose -p twr restart twr` |
| Atualizar toda a aplicação (deploy manual) | `git pull && docker compose -p twr --env-file /opt/twr/.env up -d --build twr orion-users && docker compose -p twr --env-file /opt/twr/.env up -d --force-recreate caddy` |
| Parar tudo | `docker compose -p twr down` |
| Destruir a infra AWS | `cd infra/terraform && terraform destroy` |

## Exportar a tabela `message` para CSV

A tabela `message` (histórico de conversas) fica no container `postgres`,
sem porta publicada no host — só é acessível pela rede interna do Compose.
Não há SSH/scp na instância (só SSM), então o jeito mais direto de tirar um
`.csv` já no seu computador é abrir um túnel via SSM até o container e
rodar o `psql` localmente.

```bash
# 1. Dentro da sessão SSM (aws ssm start-session --target <instance_id> ...),
#    descobrir o IP do container postgres:
cd /opt/twr
docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' \
  $(docker compose -p twr ps -q postgres)
# ex: 172.20.0.3
```

```bash
# 2. No seu computador (outro terminal), abrir o túnel TCP até o container:
aws ssm start-session \
  --target <instance_id> --region <aws_region> \
  --document-name AWS-StartPortForwardingSessionToRemoteHost \
  --parameters '{"host":["<ip_do_container>"],"portNumber":["5432"],"localPortNumber":["5432"]}'
```

```bash
# 3. Ainda no seu computador, com psql instalado localmente (brew install libpq
#    ou postgresql), exportar via localhost:5432. A senha é o POSTGRES_PASSWORD
#    do /opt/twr/.env na instância:
PGPASSWORD='<POSTGRES_PASSWORD>' psql -h localhost -p 5432 -U twr -d twr \
  -c "\copy (SELECT * FROM message ORDER BY chat_id, sequence) TO 'message.csv' WITH CSV HEADER"
```

O arquivo `message.csv` é gravado diretamente na máquina local, sem precisar
copiar/colar saída de terminal.

Alternativa rápida (sem túnel, só pra espiar poucas linhas): dentro da
sessão SSM, `docker compose -p twr exec -T postgres psql -U twr -d twr -c
"\copy (SELECT * FROM message ORDER BY chat_id, sequence) TO STDOUT WITH CSV
HEADER" > /tmp/message.csv` e depois `cat /tmp/message.csv` para copiar a
saída manualmente — só vale para volumes pequenos de dados.

## Fora do escopo deste guia (próximos passos sugeridos)

- **Backups**: snapshots automáticos do volume EBS de dados (`aws_ebs_volume.data`) ou `pg_dump` agendado
- **Alta disponibilidade**: essa arquitetura é uma única instância — sem redundância. Para HA seria necessário migrar para RDS + ElastiCache + múltiplas instâncias/ECS, com custo bem maior
