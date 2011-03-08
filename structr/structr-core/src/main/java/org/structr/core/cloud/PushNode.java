/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.cloud;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author axel
 */
public class PushNode extends CloudServiceCommand {

    private static final Logger logger = Logger.getLogger(PushNode.class.getName());

    @Override
    public Object execute(Object... parameters) {

        AbstractNode node = null;
        String remoteHost = null;

        Command findNode = Services.command(FindNodeCommand.class);

        switch (parameters.length) {
            case 0:
                throw new UnsupportedArgumentError("No arguments supplied");

            case 2:

                if (parameters[0] instanceof Long) {
                    long id = ((Long) parameters[0]).longValue();
                    node = (AbstractNode) findNode.execute(null, id);

                } else if (parameters[0] instanceof AbstractNode) {
                    node = ((AbstractNode) parameters[0]);

                } else if (parameters[0] instanceof String) {
                    long id = Long.parseLong((String) parameters[0]);
                    node = (AbstractNode) findNode.execute(null, id);
                }

                pushNode(node, remoteHost);

            default:

        }

        return null;

    }

    private void pushNode(AbstractNode node, String remoteHost) {

        Client client = new Client();
        client.start();

        logger.log(Level.INFO, "KryoNet client started");

        Kryo kryo = client.getKryo();
        kryo.register(AbstractNode.class);
        
        try {

            client.connect(5000, remoteHost, 54555, 54777);
            logger.log(Level.INFO, "Connected to KryoNet server");

            client.sendTCP(node);
            logger.log(Level.INFO, "Node {0} was sent", node.getId());
            
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error while sending node to remote instance", ex);
        }

    }
}
