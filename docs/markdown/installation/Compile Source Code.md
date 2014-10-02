Structr is open source, and the complete source code is available on GitHub: <a href="https://github.com/structr/structr">https://github.com/structr/structr</a>

Here you can download the repo as one large ZIP file: <a href="https://github.com/structr/structr/archive/master.zip">master.zip</a>

To build Structr from the sources, do the following:

    git clone https://github.com/structr/structr.git
    cd structr
    mvn clean install -DskipTests
    cd structr-ui
    mvn exec:exec
