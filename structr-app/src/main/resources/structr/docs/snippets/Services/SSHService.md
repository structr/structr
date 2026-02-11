# SSHService

Provides SSH access to an interactive Admin Console and SFTP/SSHFS access to Structr's virtual filesystem. Only admin users can connect. Authentication requires a public key configured in the user's `publicKey` property.

## Settings

| Setting | Default | Description |
|---------|---------|-------------|
| `application.ssh.port` | 8022 | Port the SSH server listens on |
| `application.ssh.forcepublickey` | true | Require public key authentication, disabling password login |
