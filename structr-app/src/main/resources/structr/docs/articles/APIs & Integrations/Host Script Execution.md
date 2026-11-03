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

A key added to `structr.conf` by hand takes effect after reloading the configuration file in the Configuration Interface, or after a restart. A key created in the Configuration Interface takes effect immediately.

## The Scripts Folder

All scripts must be placed in the `scripts` folder within your Structr installation directory. The location is controlled by the `scripts.path` setting, which defaults to `scripts` relative to `base.path`.

Scripts must be executable:

```bash
chmod +x scripts/generate-pdf.sh
```

For security, Structr does not follow symbolic links and does not allow directory traversal (paths containing `..`). The settings `scripts.path.allowsymboliclinks` and `scripts.path.allowpathtraversal` (both `false` by default) disable these checks, which is not recommended.

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

When streaming to an HTTP response, ensure the page has the correct content type set and the `pageCreatesRawData` flag enabled. When the output is a file, Structr closes it after the script has ended, which stores the content.

### Two Ways to Pass Parameters

Both functions accept the parameters in two ways, and both are fully supported:

| Mode | exec() | execBinary() |
|------|--------|--------------|
| As one list, optionally followed by log behavior and input | `exec(key, [p1, p2], logBehavior, input)` | `execBinary(output, key, [p1, p2], logBehavior, input)` |
| As arguments of their own | `exec(key, p1, p2)` | `execBinary(output, key, p1, p2)` |

```javascript
$.exec('my.script', ['param1', 'param2'], 1);
$.exec('my.script', 'param1', 'param2');
```

Only the list mode can pass a log behavior or an input. A single parameter that is itself a list has to be given in the list mode. In both modes, every parameter is either a value or an object with `value` and `mask` (see [Parameter Masking](#parameter-masking)), and a `null` parameter is rejected.

### How Parameters Reach the Script

Each parameter is passed to the script as one command-line argument, exactly as given. Structr starts the script directly, without a shell in between, so quotes, spaces, `$(...)`, backticks or `$VARIABLES` in a value reach the script as plain text and are never executed. An empty string is passed as an empty argument. A script without a shebang line such as `#!/bin/sh` is run with `/bin/sh`.

Parameters are encoded with the platform encoding of the Structr process. With a UTF-8 locale (for example `LANG=C.UTF-8`), non-ASCII characters like umlauts arrive unchanged; with the plain `C` locale, Java replaces them with `?`. If you cannot control the locale, pass such values as input, which is not affected by it.

### Parameter Masking

When passing sensitive values like passwords or API keys, you can mask them in the log output:

**JavaScript:**
```javascript
$.exec('my.script', [
    'username',
    { value: 'SECRET_API_KEY', mask: true }
]);
```

The masked parameter appears as `***` in the log while the actual value is passed to the script. Masking only affects Structr's log: the value is still a command-line argument, which other users on the host can see with `ps` while the script runs. Pass secrets on standard input instead.

### Standard Input

The optional fourth argument of `exec()` (the fifth of `execBinary()`) is written to the script's standard input. Unlike a parameter, it never appears in the process list of the host, Structr never logs it, and it is not limited in size or affected by the locale. That makes it the right channel for secrets as well as for larger data.

| Input | Written to stdin as |
|-------|---------------------|
| String | its UTF-8 bytes |
| Byte array | the bytes, unchanged |
| File | the file's content, unchanged |
| `null` or omitted | nothing, stdin is empty |

**JavaScript:**
```javascript
// a secret
$.exec('my.login.script', ['admin'], null, password);

// structured data
$.exec('my.import.script', [], null, JSON.stringify(records));

// a file, converted and streamed into another file
$.execBinary(pdfFile, 'convert.to.pdf', [], null, uploadedFile);
```

Passing `null` as the log behavior keeps the default. The script reads its input from stdin, and hands a secret on through a pipe rather than as an argument:

```bash
#!/bin/sh
read -r PASSWORD
printf '%s' "$PASSWORD" | some-tool --user "$1" --password-stdin
```

Structr writes the input while the script runs, so a script can read a large input and write a large output at the same time, and closes the standard input afterwards: a script that reads all of it (for example with `cat`) gets its end. A script that does not read its input still runs normally.

### Output and Exit Codes

`exec()` returns everything the script wrote to standard output, also when it exits with an error. `execBinary()` copies the standard output to the file or response and returns what the script wrote to standard error. When a script exits with an error, Structr logs the exit code together with the error output.

After the script has ended, Structr waits for its remaining output, but for at most ten seconds. This matters only for a script that starts a background process which keeps the output open: redirect the output of such a process (`some-daemon > /dev/null 2>&1 &`), so that the call returns right away.

### Log Behavior

You can control how script execution is logged by passing a log behavior after the parameter list:

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
- Parameters are never interpreted by a shell, but the script itself decides what it does with them. Validate user input before passing it, and quote parameters inside the script (`"$1"`), especially when it hands them on to other commands.
- Run Structr with a user account that has only the permissions necessary for its operation. Scripts execute with the same permissions as the Structr process.

## Best Practices

- Pass larger or structured data as input rather than as parameters. For output that may contain special characters, encode it as Base64.
- Combine host scripts with the Cron service to run them on a schedule. Register the script in `structr.conf`, then call it from a scheduled function.
- Scripts should do one thing well. Complex logic is better implemented in Structr's scripting environment where you have access to the full API.
- Pass passwords, tokens and keys on standard input, not as parameters.
- Use the log behavior parameter to avoid logging sensitive data while still maintaining an audit trail for debugging.

Example for passing data as input and decoding Base64 output:

```javascript
// Pass data as input
$.exec('my.script', [], null, complexInput);

// Decode output
let result = $.base64_decode($.exec('my.script'));
let data = $.from_json(result);
```

## Related Topics

- Scheduled Tasks - Running scripts automatically on a schedule
- Configuration - Setting up structr.conf
