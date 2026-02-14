# SSH Access

Structr includes a built-in SSH server that provides command-line access to the Admin Console and filesystem. Administrators can connect via SSH to execute scripts, run queries, and manage files without using the web interface.

## Overview

The SSH service provides two main capabilities:

- **Admin Console** - An interactive command-line interface for executing JavaScript, StructrScript, Cypher queries, and administrative commands
- **Filesystem Access** - SFTP and SSHFS access to Structr's virtual filesystem

SSH access is restricted to admin users. Non-admin users receive an authentication error when attempting to connect.

## Enabling the SSH Service

The SSH service is not enabled by default. To activate it:

1. Open the Configuration Interface
2. Enable the `SSHService` in the list of configured services
3. Save the configuration
4. Navigate to the Services tab
5. Start the SSHService

When the service starts successfully, you see log entries like:

```
INFO  org.structr.files.ssh.SSHService - Setting up SSH server..
INFO  org.structr.files.ssh.SSHService - Initializing host key generator..
INFO  org.structr.files.ssh.SSHService - Configuring SSH server..
INFO  org.structr.files.ssh.SSHService - Starting SSH server on port 8022
INFO  org.structr.files.ssh.SSHService - Initialization complete.
```

On first startup, Structr generates an SSH host key and stores it locally. This key identifies your Structr instance to SSH clients.

## Configuration

Configure the SSH service in `structr.conf`:

| Setting | Default | Description |
|---------|---------|-------------|
| `sshservice.port` | 8022 | The port the SSH server listens on |

Remember that `structr.conf` only contains settings that differ from defaults. If you want to use port 8022, you do not need to add this setting.

## Setting Up User Access

SSH authentication uses public key authentication. Each user who needs SSH access must have their public key configured in Structr.

To add a public key for a user:

1. Open the Security area in the Admin UI
2. Select the user
3. Open the Edit dialog
4. Navigate to the Advanced tab
5. Paste the user's public key into the `publicKey` field
6. Save the changes

The public key is typically found in `~/.ssh/id_rsa.pub` or `~/.ssh/id_ed25519.pub` on the user's machine. The entire contents of this file should be pasted into the field.

> **Note:** Only users with `isAdmin = true` can connect via SSH. Non-admin users receive the error "SSH access is only allowed for admin users!" when attempting to connect.

## Connecting via SSH

Connect to Structr using a standard SSH client:

```bash
ssh -p 8022 admin@localhost
```

Replace `admin` with your username, `localhost` with your server address, and `8022` with your configured port.

On first connection, you are prompted to verify the server's host key fingerprint:

```
The authenticity of host '[localhost]:8022 ([127.0.0.1]:8022)' can't be established.
RSA key fingerprint is SHA256:9YVTKL8x/PUhOdQUPdDmwdCDqZmDzbE5NuXlY16jQeI.
Are you sure you want to continue connecting (yes/no/[fingerprint])? yes
```

After confirming, you see the welcome message and enter the Admin Console:

```
Welcome to the Structr 6.2-SNAPSHOT JavaScript console. Use <Shift>+<Tab> to switch modes.
admin@Structr/>
```

## Admin Console

The Admin Console provides an interactive environment for executing commands. It supports multiple modes, each with different capabilities.

### Switching Modes

Use `Console.setMode()` to switch between modes:

```javascript
Console.setMode('JavaScript')   // Default mode
Console.setMode('StructrScript')
Console.setMode('Cypher')
Console.setMode('AdminShell')
Console.setMode('REST')
```

You can also press `Shift+Tab` to cycle through available modes.

### JavaScript Mode

The default mode. Execute JavaScript code with full access to Structr's scripting API:

```javascript
admin@Structr/> $.find('User')
admin@Structr/> $.find('Project', { status: 'active' })
admin@Structr/> $.create('Task', { name: 'New Task' })
```

### StructrScript Mode

Execute StructrScript expressions:

```
admin@Structr/> find('User')
admin@Structr/> size(find('Project'))
```

### Cypher Mode

Execute Neo4j Cypher queries directly:

```cypher
admin@Structr/> MATCH (n:User) RETURN n
admin@Structr/> MATCH (p:Project)-[:HAS_TASK]->(t:Task) RETURN p.name, count(t)
```

### AdminShell Mode

Access administrative commands. Type `help` to see available commands:

```
admin@Structr/> Console.setMode('AdminShell')
Mode set to 'AdminShell'. Type 'help' to get a list of commands.
admin@Structr/> help
```

### REST Mode

Execute REST-style operations. Type `help` to see available commands:

```
admin@Structr/> Console.setMode('REST')
Mode set to 'REST'. Type 'help' to get a list of commands.
admin@Structr/> help
```

## Filesystem Access

You can mount Structr's virtual filesystem on your local machine using SSHFS. This allows you to browse and edit files using standard file management tools.

### Mounting with SSHFS

Install SSHFS on your system if not already available, then mount the filesystem:

```bash
sshfs admin@localhost:/ mountpoint -p 8022
```

Replace:

- `admin` with your username
- `localhost` with your server address
- `mountpoint` with your local mount directory
- `8022` with your configured SSH port

After mounting, you can navigate the Structr filesystem like any local directory:

```bash
cd mountpoint
ls -la
```

### Unmounting

To unmount the filesystem:

```bash
fusermount -u mountpoint   # Linux
umount mountpoint          # macOS
```

## Troubleshooting

### Connection Refused

If you cannot connect:

- Verify the SSHService is running in the Services tab
- Check that the port is not blocked by a firewall
- Confirm you are using the correct port (default: 8022)

```bash
# Check if the port is listening
netstat -tlnp | grep 8022
```

### Authentication Failures

If authentication fails:

- Verify the public key is correctly entered in the user's `publicKey` field
- Ensure the user has `isAdmin = true`
- Check that you are using the matching private key on the client

```bash
# Test with verbose output to see authentication details
ssh -v -p 8022 admin@localhost
```

### "SSH access is only allowed for admin users!"

This error indicates the user exists and authenticated successfully, but does not have admin privileges. Set `isAdmin = true` on the user to grant SSH access.

## Security Considerations

SSH access provides powerful administrative capabilities. Consider these security practices:

- **Limit admin users** - Only grant admin status to users who genuinely need it
- **Protect private keys** - Users should secure their private keys with passphrases
- **Use strong keys** - Prefer Ed25519 or RSA keys with at least 4096 bits
- **Monitor access** - Review server logs for SSH connection attempts
- **Firewall the port** - Restrict SSH port access to trusted networks if possible

## Related Topics

- User Management - Managing users and the `publicKey` property
- Configuration - Service configuration in structr.conf
- Admin Console - Detailed documentation of console commands and modes
