mkdir plugins
java -cp lib\*;plugins\* -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false -Duser.timezone=Europe/Berlin -Duser.country=US -Duser.language=en -Dfile.encoding=UTF-8 -Dorg.apache.sshd.registerBouncyCastle=false -Dorg.neo4j.io.pagecache.implSingleFilePageSwapper.channelStripePower=0 -server -Xmx8g -XX:+UseNUMA -XX:+UseG1GC org.structr.Server

