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
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.RunnableNodeService;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

/**
 * The cloud service handles networking between structr instances
 *
 * @author axel
 */
public class CloudService extends RunnableNodeService {

    private static final Logger logger = Logger.getLogger(CloudService.class.getName());
    public static final Integer BEGIN_TRANSACTION = 0;
    public static final Integer END_TRANSACTION = 1;
    /** Containing addresses of all available structr instances */
    private static final Set<InstanceAddress> instanceAddresses = new LinkedHashSet<InstanceAddress>();
    /** Local KryoNet server remote clients can connect to */
    private Server server = null;
    private final static int DefaultTcpPort = 54555;
    private final static int DefaultUdpPort = 57555;
    private int tcpPort = DefaultTcpPort;
    private int udpPort = DefaultUdpPort;
    // Map source id to target id
    private final Map<Long, Long> idMap = new HashMap<Long, Long>();
    private boolean linkNode = false;
    private final Command findNode = Services.command(FindNodeCommand.class);
    private final Command createRel = Services.command(CreateRelationshipCommand.class);
    private final Command nodeFactory = Services.command(NodeFactoryCommand.class);
    private final AbstractNode rootNode = (AbstractNode) Services.command(FindNodeCommand.class).execute(new SuperUser(), 0L);

    public CloudService() {
        super("CloudService");

        //this.setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void injectArguments(Command command) {
        if (command != null) {
            command.setArgument("server", server);
            command.setArgument("instanceAddresses", instanceAddresses);
        }
    }

    @Override
    public void initialize(Map<String, Object> context) {

        tcpPort = Integer.parseInt(Services.getTcpPort());
        udpPort = Integer.parseInt(Services.getUdpPort());

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
    public void startService() {

        // Be quiet
        Log.set(Log.LEVEL_DEBUG);

        server = new Server(4194304, 1048576);

        server.start();

        Kryo kryo = server.getKryo();

        registerClasses(kryo);

        logger.log(Level.INFO, "KryoNet server started");

        try {

            server.bind(tcpPort, udpPort);

            server.addListener(new Listener() {

                @Override
                public void received(Connection connection, Object object) {

                    logger.log(Level.FINE, "Received object {0}", object); // TODO: reduce log level

                    if (object instanceof Integer) {

                        // Control signal received
                        Integer controlSignal = (Integer) object;

                        if (BEGIN_TRANSACTION.equals(controlSignal)) {
                            linkNode = true;
                        }

                        if (END_TRANSACTION.equals(controlSignal)) {
                            idMap.clear();
                        }

                    } else if (object instanceof NodeDataContainer) {

                        final NodeDataContainer receivedNodeData = (NodeDataContainer) object;

                        Command transactionCommand = Services.command(TransactionCommand.class);
                        transactionCommand.execute(new StructrTransaction() {

                            @Override
                            public Object execute() throws Throwable {

                                storeNode(receivedNodeData, linkNode);

                                return null;
                            }
                        });

                        connection.sendTCP("Node data received");

                    } else if (object instanceof RelationshipDataContainer) {

                        final RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer) object;

                        Command transactionCommand = Services.command(TransactionCommand.class);
                        transactionCommand.execute(new StructrTransaction() {

                            @Override
                            public Object execute() throws Throwable {

                                storeRelationship(receivedRelationshipData);

                                return null;
                            }
                        });

                        connection.sendTCP("Relationship data received");

                    } else if (object instanceof List) {

                        final List<DataContainer> dataContainers = (List<DataContainer>) object;

                        Command transactionCommand = Services.command(TransactionCommand.class);
                        transactionCommand.execute(new StructrTransaction() {

                            @Override
                            public Object execute() throws Throwable {


                                boolean linkFirstNode = true;

                                for (DataContainer receivedData : dataContainers) {

                                    if (receivedData instanceof NodeDataContainer) {

                                        storeNode(receivedData, linkFirstNode);
                                        linkFirstNode = false;

                                    } else if (receivedData instanceof RelationshipDataContainer) {

                                        storeRelationship(receivedData);
                                    }


                                }

                                return null;
                            }
                        });

                        connection.sendTCP("List data received");

                    }

                }
            });

            logger.log(Level.INFO, "KryoNet server listening on TCP port {0} and UDP port {1}", new Object[]{tcpPort, udpPort});

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "KryoNet server could not bind to TCP port " + tcpPort + " or UDP port " + udpPort, ex);
        }
    }

    @Override
    public void stopService() {
        shutdown();
    }

    private AbstractNode storeNode(final DataContainer receivedData, final boolean linkToRootNode) {

        NodeDataContainer receivedNodeData = (NodeDataContainer) receivedData;

        // Create (dirty) node
        AbstractNode newNode = (AbstractNode) nodeFactory.execute(receivedNodeData);

        // Connect first node with root node
        if (linkToRootNode) {
            // TODO: Implement a smart strategy how and where to link nodes in target instance
            createRel.execute(rootNode, newNode, RelType.HAS_CHILD);

            // Reset link node flag, has to be explicetly set to true!
            linkNode = false;
            logger.log(Level.INFO, "First node {0} linked to root node", newNode.getIdString()); // TODO: reduce log level
        }

        idMap.put(receivedNodeData.getSourceNodeId(), newNode.getId());

        logger.log(Level.INFO, "New node {0} created from remote data", newNode.getIdString()); // TODO: reduce log level

        return newNode;

    }

    private StructrRelationship storeRelationship(final DataContainer receivedData) {

        RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer) receivedData;

        StructrRelationship newRelationship = null;

        long sourceStartNodeId = receivedRelationshipData.getSourceStartNodeId();
        long sourceEndNodeId = receivedRelationshipData.getSourceEndNodeId();

        long targetStartNodeId = idMap.get(sourceStartNodeId);
        long targetEndNodeId = idMap.get(sourceEndNodeId);

        // Get new start and end node
        AbstractNode targetStartNode = (AbstractNode) findNode.execute(new SuperUser(), targetStartNodeId);
        AbstractNode targetEndNode = (AbstractNode) findNode.execute(new SuperUser(), targetEndNodeId);
        String name = receivedRelationshipData.getName();

        if (targetStartNode != null && targetEndNode != null && StringUtils.isNotEmpty(name)) {

            newRelationship = (StructrRelationship) createRel.execute(targetStartNode, targetEndNode, name);
            logger.log(Level.INFO, "New {3} relationship {0} created from remote data between {1} and {2}", new Object[]{newRelationship.getId(), targetStartNodeId, targetEndNodeId, name}); // TODO: reduce log level

        }

        return newRelationship;
    }

    public static void registerClasses(Kryo kryo) {

        kryo.register(HashMap.class);
        kryo.register(LinkedList.class);

        // structr classes
        kryo.register(NodeDataContainer.class);
        kryo.register(FileNodeDataContainer.class);
        kryo.register(RelationshipDataContainer.class);

        // Neo4j array types
        kryo.register(String[].class);
        kryo.register(char[].class);
        kryo.register(byte[].class);
        kryo.register(boolean[].class);
        kryo.register(int[].class);
        kryo.register(long[].class);
        kryo.register(short[].class);
        kryo.register(float[].class);
        kryo.register(double[].class);

    }
}
