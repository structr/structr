/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.cloud;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.minlog.Log;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.File;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.User;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author axel
 */
public class PushNodes extends CloudServiceCommand {

    private static final Logger logger = Logger.getLogger(PushNodes.class.getName());

    @Override
    public Object execute(Object... parameters) {

        User user = null;
        AbstractNode node = null;
        String remoteHost = null;
        int remoteTcpPort = 0;
        int remoteUdpPort = 0;
        boolean recursive = false;

        Command findNode = Services.command(FindNodeCommand.class);

        switch (parameters.length) {
            case 0:
                throw new UnsupportedArgumentError("No arguments supplied");

            case 6:

                if (parameters[0] instanceof User) {
                    user = (User) parameters[0];
                }

                if (parameters[1] instanceof Long) {
                    long id = ((Long) parameters[1]).longValue();
                    node = (AbstractNode) findNode.execute(null, id);

                } else if (parameters[1] instanceof AbstractNode) {
                    node = ((AbstractNode) parameters[1]);

                } else if (parameters[1] instanceof String) {
                    long id = Long.parseLong((String) parameters[1]);
                    node = (AbstractNode) findNode.execute(null, id);
                }

                if (parameters[2] instanceof String) {
                    remoteHost = (String) parameters[2];
                }

                if (parameters[3] instanceof Integer) {
                    remoteTcpPort = (Integer) parameters[3];
                }

                if (parameters[4] instanceof Integer) {
                    remoteUdpPort = (Integer) parameters[4];
                }

                if (parameters[5] instanceof Boolean) {
                    recursive = (Boolean) parameters[5];
                }

                pushNodes(user, node, remoteHost, remoteTcpPort, remoteUdpPort, recursive);

            default:

        }

        return null;

    }

    private void pushNodes(final User user, final AbstractNode node, final String remoteHost, final int remoteTcpPort, final int remoteUdpPort, final boolean recursive) {

        List<DataContainer> transportSet = new LinkedList<DataContainer>();
        Set<RelationshipDataContainer> transportRelationships = new HashSet<RelationshipDataContainer>();

        int maxSize = 4096;

        if (recursive) {

            List<AbstractNode> nodes = node.getAllChildren(user);

            //Set<StructrRelationship> relationships = new HashSet<StructrRelationship>();

            for (AbstractNode n : nodes) {

                DataContainer container;
                if (n instanceof File) {
                    container = new FileNodeDataContainer(n);
                } else {
                    container = new NodeDataContainer(n);
                }
                
                maxSize = Math.max(maxSize, container.getEstimatedSize());

                transportSet.add(container);

                // Collect all relationships whose start and end nodes are contained in the above list
                List<StructrRelationship> rels = n.getOutgoingRelationships();

                for (StructrRelationship r : rels) {

                    AbstractNode startNode = r.getStartNode();
                    AbstractNode endNode = r.getEndNode();

                    if (nodes.contains(startNode) && nodes.contains(endNode)) {
                        transportRelationships.add(new RelationshipDataContainer(r));
                    }
                }
            }

            // After all nodes are through, add relationships
            transportSet.addAll(transportRelationships);

        } else {

            // If not recursive, add only the node itself
            transportSet.add(new NodeDataContainer(node));

        }



        // Be quiet
        Log.set(Log.LEVEL_DEBUG);

        Client client = new Client(maxSize*8, maxSize*2);

        client.start();

        logger.log(Level.INFO, "KryoNet client started, buffer sizes {0}, {1}", new Object[]{maxSize*8, maxSize*2});

        Kryo kryo = client.getKryo();

        CloudService.registerClasses(kryo);


        try {

            client.connect(5000, remoteHost, remoteTcpPort, remoteUdpPort);
            logger.log(Level.INFO, "Connected to structr instance on {0} (tcp port: {1}, udp port: {2})", new Object[]{remoteHost, remoteTcpPort, remoteUdpPort});

            int size = transportSet.size();

            if (size < 16) {

                // For small transfers, send all nodes and relationships of the transport set to remote server in one transaction
                
                client.sendTCP(transportSet);

            } else {

                // More than 16 nodes => send one by one

                client.sendTCP(CloudService.BEGIN_TRANSACTION); // mark start of transaction

                for (DataContainer container : transportSet) {

                    client.sendTCP(container);

                }
                client.sendTCP(CloudService.END_TRANSACTION); // mark end of transaction

            }

            logger.log(Level.INFO, "Transport set with {0} nodes/relationships was sent", transportSet.size()); // TODO: Reduce log level, when stable


            // TODO: Has the client really to be closed after each transport?
            //client.close();

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error while sending nodes to remote instance", ex);
        }

    }
}
