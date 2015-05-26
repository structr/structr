# Structr on Windows: Manual installation Guide

## About this document

If you want to run Structr on Windows, follow these instructions for a full manual installation and start.

For evaluating a Windows-based installation, you can use a virtual machine provided by Microsoft: http://dev.modern.ie/tools/vms/

## Prerequisites

Make sure you have Java JDK 7 installed on your machine.  The standard JRE package which is installed by default on most computers is not sufficient because it doesn't contain the Java Compiler (javac.exe).

1. Check Java version

You can check the Java version with

    C:\>where java


2. Download and install JDK 7

If you get ``C:\Windows\System32\java.exe`` as response, go to http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html and download the JDK version for your operating system (32 or 64 bit).

Follow the installation instructions from here:
http://docs.oracle.com/javase/7/docs/webnotes/install/windows/jdk-installation-windows.html


3. Install and configure JDK

Now you need to activate the installed Java version.

Open the Java Runtime Environment Settings from the Java Control Panel, found in Control Panel > Programs:

![Java Runtime Environment Settings](img-015.png "Fig. 1: Java Runtime Environment Settings")

Click on "Find" and navigate to the location of the newly installed JDK:

![Select Java Runtime Environment location](img-016.png "Fig. 2: Select Java Runtime Environment location")

Click "Next", select the entry found, click "Finish" to confirm the selection.

![Confirm JRE Finder](img-017.png "Fig. 3: Confirm JRE Finder")

Deactivate the old and activate the new Java location:

![Activate new location](img-018.png "Fig. 4: Activate new location")


4. Change %PATH% variable

In order to make the new Java version available for running Structr, it's good practice to add it to the global %PATH% environment variable.

Go to "Control Panel > System and Security > System", click the "Advanced" tab and click on "Environment Variables".

![Environment Variables](img-019.png "Fig. 5: Environment Variables")

![Path Variable](img-020.png "Fig. 6: Path Variable")

Click on "Edit" and change the value to reflect the new JDK location:

![Edit System Variable](img-021.png "Fig. 7: Edit System Variable")
![System variables after editing](img-022.png "Fig. 8: System variables after editing")

To confirm new settings, open a new Command Prompt window (cmd) and type

	C:\Users\IEUser\>where java

and

     C:\Users\IEUser\>where javac

Both commands should return the paths to the JDK executables java.exe and javac.exe, similar to these:

C:\Program Files\Java\jdk1.7.0_79\bin\java.exe
C:\Program Files\Java\jdk1.7.0_79\bin\javac.exe

![Check Paths](img-023.png "Fig. 9: Check Paths")

Important Note: Unless you have javac.exe available in the path, you're not running a full JDK, and Structr will not run at all, showing errors when trying to compile the dynamic schema at startup (you will see a lot of NodeExtender exceptions in the log).

## Install Structr

1. Download and install Structr

Download the latest Structr distribution file (...dist.zip) from here:

https://oss.sonatype.org/content/repositories/snapshots/org/structr/structr-ui/


2. Unzip the distribution archive

Move the file to the location you want Structr to run from, here we choose the Desktop folder.

Unzip the file (typically with "Extract here").


3. Unzip the static resources

Enter the directory which was created:

    C:\Users\IEUser>cd structr-ui-1.1-SNAPSHOT-201505231136.f596a

Extract the "structr/" folder from the JAR file with the "jar" command:

    C:\Users\IEUser\structr-ui-1.1-SNAPSHOT-201505231136.f596a>jar xvf structr-ui-1.1-SNAPSHOT-201505231136.f596a.jar structr

This will create a folder named "structr" containing the static resources needed for the backend UI.

Note: The folder name may vary, depending on the version you downloaded. The folder and JAR file names contain the module name (structr-ui), the version (1.1-SNAPSHOT), the build date (201505231136) and the first five characters of the git commit uuid (f596a).


4. Start Structr

Start Structr with the following command:

    java -cp lib/*;structr-ui-1.1-SNAPSHOT-201505231136.f596a.jar org.structr.Server

This will start a new java process, including all libraries in the lib folder and the structr JAR file itself, starting the main class "org.structr.Server".

A new configuration file named "structr.conf" will be automatically created upon first startup.

You should see startup messages similar to the following figure:

![Startup Log](img-027.png "Fig. 10: Startup Log")

You can stop the Structr process with "Ctrl-C".


## Verify installation

To verifiy Structr is running properly, go to http://localhost:8082/ and check if you can see the Welcome page.

![Welcome Page](img-030.png "Fig. 11: Welcome Page")

Go to http://localhost:8082/structr/ and login with admin/admin.

![Login Page](img-028.png "Fig. 12: Login Page")

After login, you should see the following:

![Backend UI Pages](img-032.png "Fig. 13: Backend UI Pages")