You can download Structr UI as a Debian package (.deb) from Maven Central.

Releases: <a href="https://oss.sonatype.org/content/repositories/releases/org/structr/structr-ui/">https://oss.sonatype.org/content/repositories/releases/org/structr/structr-ui/</a>

Snapshots: <a href="https://oss.sonatype.org/content/repositories/snapshots/org/structr/structr-ui/">https://oss.sonatype.org/content/repositories/snapshots/org/structr/structr-ui/</a>.

- Download the latest .deb file (~ 60 MB).
- After download, install it with <pre class="code">sudo dpkg -i structr-ui-<version>.deb</pre>
- Structr UI will be installed in <code>/usr/lib/structr-ui</code>, log file is <code>/var/log/structr-ui.log</code>.
- Then start Structr UI with <pre class="code">sudo service structr-ui start</pre>
- Go to <a href="http://localhost:8082/structr#pages"> http://localhost:8082/structr#pages</a>
- Login with the credentials <code>admin/admin</code>. </li>
