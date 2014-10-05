The REST resource URL for a given type corresponds to the name of that type, i.e. the type `File` corresponds to the `/files` resource. Structr implements a "best guess" strategy to map singular type names to plural resource URLs, however, the best guess is not always the correct one. The following examples illustrate this strategy:

Good type names (TM) are upper camel-case type names that do not end with a single letter "s". (This means that two "s" are ok, since Structr will not append a third "s" to the end of the resource URL).
<table>
<tr><th>Type name</th><th>Resource URL</th></tr>
<tr><td>File</td><td>/files</td></tr>
<tr><td>User</td><td>/users</td></tr>
<tr><td>MailTemplate</td><td>/mail_templates</td></tr>
<tr><td>Entry</td><td>/entries</td></tr>
<tr><td>Access</td><td>/access</td></tr>
</table>

Not-so-good type names (TM) are all uppercase names (like URL, see below why), or names that end with a single "s", because the best-guess strategy does not handle that case very well. Also the word "series" is not handled correctly by the current implementation, so please be aware. (Sorry!)
<table>
<tr><th>Type name</th><th>Resource URL</th></tr>
<tr><td>Series</td><td>/sery</td></tr>
<tr><td>URL</td><td>/u_r_l</td></tr>
<tr><td>Hippopotamus</td><td>/hippopotamuss</td></tr>
</table>
