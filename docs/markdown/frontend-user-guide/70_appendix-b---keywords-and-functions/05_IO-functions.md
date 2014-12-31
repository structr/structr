<table>
<tr><th>Function</th><th>Description</th><th>Example</th></tr>

<tr><td>config(key [, defaultValue])</td><td>Returns a configuration value from the Structr configuration file (structr.conf) with an optional default value. You can use this function to evaluate custom structr.conf entries in your web application.</td><td><code>${config('my.custom.value', 10)}</code></td></tr>
<tr><td>read(filename)</td><td>Tries to read the file with the given name in the <i>exchange</i> directory, returns the whole file as a string.</td><td><code>${xml(read('test.xml'))}</code></td></tr>
<tr><td>write(filename, value)</td><td>Writes the given value into the file with the given name in the <i>exchange</i> directory, overwriting the file if it exists; use append() if the overwriting behaviour is not desired.</td><td><code>${write('test.txt', this.id)}</code></td></tr>
<tr><td>append(filename, value)</td><td>Appends the given value to the file with the given name in the <i>exchange</i> directory.</td><td><code>${append('test.txt', 'next line')}</code></td></tr>
<tr><td>print(objects...)</td><td>Prints the given object(s) to the standard output (which will end up in the Structr server log file).</td><td><code>${print('success')}</code></td></tr>

</table>
