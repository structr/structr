#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import org.structr.server.Structr;
import org.structr.server.StructrServer;

public class Server implements StructrServer {

        public static void main(String[] args) {

                try {

                        Structr.createServer(Server.class, "${artifactId} ${version}")

                                .start(true);


                } catch(Exception ex) {

                        ex.printStackTrace();
                }
        }
}
