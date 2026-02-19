# Host Script Execution

Structr can execute shell scripts on the host system, allowing your application to interact with the operating system, run external tools, and integrate with other software on the server. This opens up possibilities like generating documents with external converters, running maintenance tasks from a web interface, querying system metadata, controlling Docker containers, or integrating with legacy systems.

For security reasons, scripts must be explicitly registered in the configuration file before they can be executed. You cannot run arbitrary commands, only scripts that an administrator has approved.

## Registering Scripts

Scripts are registered in `structr.conf` using a key-value format:

```
my.pdf.generator = generate-pdf.sh
backup.database = db-backup.sh
docker.restart.app = restart-container.sh
```

The key (left side) is what you use in your code to call the script. The value (right side) is the filename of the script. Keys must be lowercase.

## The Scripts Folder

All scripts must be placed in the `scripts` folder within your Structr installation directory. The location is controlled by the `scripts.path` setting, which defaults to `scripts` relative to `base.path`.

Scripts must be executable:

```bash
chmod +x scripts/generate-pdf.sh
```

For security, Structr does not follow symbolic links and does not allow directory traversal (paths containing `..`). These restrictions can be disabled via configuration settings, but this is not recommended.

## Executing Scripts

Structr provides two functions for script execution: `exec()` for text output and `execBinary()` for binary data.

### exec()

The `exec()` function runs a script and returns its text output.

**StructrScript:**
```
${exec('my.pdf.generator')}
${exec('my.script', merge('param1', 'param2'))}
```

**JavaScript:**
```javascript
$.exec('my.pdf.generator');
$.exec('my.script', ['param1', 'param2']);
```

Parameters are passed to the script as command-line arguments. They are automatically quoted to handle spaces and special characters.

### execBinary()

The `execBinary()` function runs a script and streams its binary output directly to a file or HTTP response. This is essential when working with binary data like images, PDFs, or other generated files.

**StructrScript:**
```
${execBinary(response, 'my.pdf.generator')}
${execBinary(myFile, 'convert.image', merge('input.png'))}
```

**JavaScript:**
```javascript
$.execBinary($.response, 'my.pdf.generator');
$.execBinary(myFile, 'convert.image', ['input.png']);
```

When streaming to an HTTP response, ensure the page has the correct content type set and the `pageCreatesRawData` flag enabled.

### Parameter Masking

When passing sensitive values like passwords or API keys, you can mask them in the log output:

**JavaScript:**
```javascript
$.exec('my.script', [
    'username',
    { value: 'SECRET_API_KEY', mask: true }
]);
```

The masked parameter appears as `***` in the log while the actual value is passed to the script.

### Log Behavior

You can control how script execution is logged by passing a third parameter:

| Value | Behavior |
|-------|----------|
| 0 | Do not log the command line |
| 1 | Log only the script path |
| 2 | Log script path and parameters (with masking applied) |

The default is controlled by the `log.scriptprocess.commandline` setting.

## Security Considerations

Host script execution is a powerful feature that requires careful handling.

- Only scripts registered in `structr.conf` can be executed. This configuration-based allowlist prevents code injection attacks. Even if an attacker gains access to your application logic, they cannot execute arbitrary commands.
- By default, script paths cannot be symbolic links. This prevents attacks where a symlink points to a sensitive file outside the scripts folder.
- Paths containing `..` are rejected by default, preventing access to files outside the scripts folder.
- Always validate and sanitize any user input before passing it as a parameter to a script. Never construct script parameters directly from user input without validation.
- Run Structr with a user account that has only the permissions necessary for its operation. Scripts execute with the same permissions as the Structr process.

## Best Practices

- When passing parameters with special characters or receiving output that may contain special characters, encode the data as Base64. This prevents issues with quoting and escaping.
- Combine host scripts with the Cron service to run them on a schedule. Register the script in `structr.conf`, then call it from a scheduled function.
- Scripts should do one thing well. Complex logic is better implemented in Structr's scripting environment where you have access to the full API.
- Use the log behavior parameter to avoid logging sensitive data while still maintaining an audit trail for debugging.

Example for Base64 encoding:

```javascript
// Encode parameters
$.exec('my.script', [$.base64_encode(complexInput)]);

// Decode output
let result = $.base64_decode($.exec('my.script'));
let data = $.from_json(result);
```

## Related Topics

- Scheduled Tasks - Running scripts automatically on a schedule
- Configuration - Setting up structr.conf
