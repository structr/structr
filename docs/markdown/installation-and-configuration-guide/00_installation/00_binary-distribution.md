Structr runs on any platform with Java JDK 8 installed. All you need is contained in a single distribution file.

1. Download the binary distribution file (-dist.tar.gz) from Maven Central (~ 100 MB): [https://oss.sonatype.org/content/repositories/releases/org/structr/structr-ui/](https://oss.sonatype.org/content/repositories/releases/org/structr/structr-ui/)
2. Extract the file and enter the created directory (with bin, lib and the .jar file)
3. Start Structr UI from this directory with the following command: <pre class="code">bin/start</pre>
4. Go to <a href="http://localhost:8082/structr#pages"> http://localhost:8082/structr#pages</a>
5. Login with the credentials <code>admin/admin</code>.

<p class="info">If you can't access Structr on Mac OS X (404 error), please remove the *.sources.jar and *.javadoc.jar from the main directory and restart. There should be a subdirectory named ``structr``.</p>

