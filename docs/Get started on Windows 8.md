# Get startet on Windows 8
This Document will help you to run and develop structr on windows 8. 

##The first Steps
Structr based on Maven. Basically you only need to install Maven and Java.

###Using Maven
To use Maven in the PowerShell: [Download](http://maven.apache.org/download.cgi) and unzip the distribution archive to the directory you wish, i.e. C:\Program Files\Apache Software Foundation and follow the Installation instructions on the Maven Download site for Windows.

    Note: You only need to add environment variables for Maven and Java(JAVA_HOME). 
    Where to add: Go to start and type in "env". After the window is opened, select "edit the system environment variables".
###Using cURL
To use cURl in the Powershell: [Download](http://curl.haxx.se/download.html) the windows version and place the file in your "windows/system32" directory. That's it.

###Starting structr
For more information: read the Backend User's Guide.

####structr-ui admin seed
>coming soon [new seed.bat file]

##Troubleshooting
###org.structr.Ui not found error
This error occurs with "mvn exec:exec" and "run.bat"!
There is a problem within the pom file. Find the plugin 'org.codehaus.mojo' (the first plugin) and replace the symbol ':' with ';':

from

    <argument>target/lib/*:target/${project.artifactId}-${project.version}.jar</argument>

to: 

    <argument>target/lib/*;target/${project.artifactId}-${project.version}.jar</argument>

It should look like this: 

    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>1.2.1</version>
        <configuration>
            <executable>java</executable>
            <arguments>
                <argument>-server</argument>
                <argument>-d64</argument>
                <argument>-Dfile.encoding=utf-8</argument>
                <argument>-XX:+UseNUMA</argument>
                <argument>-Xms1g</argument>
                <argument>-Xmx1g</argument>
                <argument>-classpath</argument>
                <argument>target/lib/*;target/${project.artifactId}-${project.version}.jar</argument>
                <argument>org.structr.Ui</argument>
            </arguments>
        </configuration>
    </plugin>

use Maven to recompile: 'mvn clean install -DskipTests' and try 'mvn exec:exec'. 

###Problems with the folder path
structr-ui creates absolute path to the structr-ui project, i.e. E:\structr\structr-ui. In some cases you need to change them: Open the struct.conf file and change all structr path to a relative path except the base.path. 

It should look like this: (e.g. E:/structr)

    # resources
    resources = target/structr-ui-0.7.1.jar

    # JSON output nesting depth
    json.depth = 4

    # base directory
    base.path = E:/structr/structr-ui

    # temp files directory
    tmp.path = tmp

    # database files directory
    database.path = db

    # binary files directory
    files.path = files

    # log database file
    log.database.path = logDb.dat

    ...

###java.net.BindException: Address already in use: bind
Check the taskmanager and kill all java processes.
