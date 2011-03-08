/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.cloud;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.SingletonService;
import org.structr.core.entity.AbstractNode;

/**
 * The cloud service handles networking between structr instances
 *
 * @author axel
 */
public class CloudService implements SingletonService {

    private static final Logger logger = Logger.getLogger(CloudService.class.getName());
    /** Containing addresses of all available structr instances */
    private static final Set<InstanceAddress> instanceAddresses = new LinkedHashSet<InstanceAddress>();
    /** Local KryoNet server remote clients can connect to */
    private Server server = null;

    @Override
    public void injectArguments(Command command) {
        if (command != null) {
            command.setArgument("server", server);
            command.setArgument("instanceAddresses", instanceAddresses);
        }
    }

    @Override
    public void initialize(Map<String, Object> context) {

        int tcpPort = Integer.parseInt(Services.getTcpPort());
        int udpPort = Integer.parseInt(Services.getUdpPort());

        server = new Server();
        server.start();

        Kryo kryo = server.getKryo();
        kryo.register(AbstractNode.class);

        logger.log(Level.INFO, "KryoNet server started");

        try {

            server.bind(tcpPort, udpPort);

            server.addListener(new Listener() {

                @Override
                public void received(Connection connection, Object object) {

                    logger.log(Level.INFO, "Received object {0}", object);

                    if (object instanceof AbstractNode) {

                        AbstractNode receivedNode = (AbstractNode) object;

                        //receivedNode.commit(new SuperUser());
                        //logger.log(Level.INFO, "Commited node {0}", receivedNode);

                        connection.sendTCP("Node " + receivedNode.getName() + " received");
                    }

                }
            });


            logger.log(Level.INFO, "KryoNet server listening on TCP port {0} and UDP port {1}", new Object[]{tcpPort, udpPort});

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "KryoNet server could not bind to TCP port " + tcpPort + " or UDP port " + udpPort, ex);
        } finally {
            server.stop();
            server.close();
            server = null;
        }
    }

    @Override
    public void shutdown() {
        if (isRunning()) {
            server.stop();
            server.close();
            server = null;
        }
    }

    @Override
    public boolean isRunning() {
        return (server != null);
    }

    @Override
    public String getName() {
        return CloudService.class.getSimpleName();
    }
}
