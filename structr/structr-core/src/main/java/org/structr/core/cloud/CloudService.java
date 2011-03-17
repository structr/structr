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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.IndexNodeCommand;
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
    /** Containing addresses of all available structr instances */
    private static final Set<InstanceAddress> instanceAddresses = new LinkedHashSet<InstanceAddress>();
    /** Local KryoNet server remote clients can connect to */
    private Server server = null;
    private final static int DefaultTcpPort = 54555;
    private final static int DefaultUdpPort = 57555;
    private int tcpPort = DefaultTcpPort;
    private int udpPort = DefaultUdpPort;

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

        server = new Server();
        server.start();

        Kryo kryo = server.getKryo();

        registerClasses(kryo);

        logger.log(Level.INFO, "KryoNet server started");

        try {

            server.bind(tcpPort, udpPort);

            server.addListener(new Listener() {

                @Override
                public void received(Connection connection, Object object) {

                    logger.log(Level.INFO, "Received object {0}", object);

                    final AbstractNode rootNode = (AbstractNode) Services.command(FindNodeCommand.class).execute(new SuperUser(), 0L);

                    if (object instanceof NodeDataContainer) {

                        NodeDataContainer receivedNodeData = (NodeDataContainer) object;

                        // Create (dirty) node
                        final AbstractNode localNode = (AbstractNode) Services.command(NodeFactoryCommand.class).execute(receivedNodeData);

                        Command transactionCommand = Services.command(TransactionCommand.class);
                        transactionCommand.execute(new StructrTransaction() {

                            @Override
                            public Object execute() throws Throwable {

                                // Commit to database
                                localNode.commit(new SuperUser());

                                logger.log(Level.INFO, "New node {0} created from remote data", localNode.getIdString());

                                // Connect with root node
                                // TODO: Implement a smart strategy how to link nodes in target instance
                                Services.command(CreateRelationshipCommand.class).execute(rootNode, localNode, RelType.HAS_CHILD);


                                // Update index
                                Services.command(IndexNodeCommand.class).execute(localNode);

                                return null;
                            }
                        });

                        connection.sendTCP("Node data received");

                    } else if (object instanceof List) {

                        final List<DataContainer> dataContainers = (List<DataContainer>) object;

                        Command transactionCommand = Services.command(TransactionCommand.class);
                        transactionCommand.execute(new StructrTransaction() {

                            @Override
                            public Object execute() throws Throwable {

                                // Map source id to target id
                                Map<Long, Long> idMap = new HashMap<Long, Long>();
                                Command findNode = Services.command(FindNodeCommand.class);
                                Command createRel = Services.command(CreateRelationshipCommand.class);

                                boolean linkFirstNode = true;

                                for (DataContainer receivedData : dataContainers) {

                                    if (receivedData instanceof NodeDataContainer) {

                                        NodeDataContainer receivedNodeData = (NodeDataContainer) receivedData;

                                        // Create (dirty) node
                                        AbstractNode localNode = (AbstractNode) Services.command(NodeFactoryCommand.class).execute(receivedNodeData);

                                        // Commit to database
                                        localNode.commit(null);

                                        // Connect first node with root node
                                        if (linkFirstNode) {
                                            // TODO: Implement a smart strategy how and where to link nodes in target instance
                                            createRel.execute(rootNode, localNode, RelType.HAS_CHILD);
                                            linkFirstNode = false;
                                            logger.log(Level.INFO, "First node {0} linked to root node", localNode.getIdString());
                                        }

                                        idMap.put(receivedNodeData.getSourceNodeId(), localNode.getId());

                                        logger.log(Level.INFO, "New node {0} created from remote data", localNode.getIdString());

                                    } else if (receivedData instanceof RelationshipDataContainer) {

                                        RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer) receivedData;

                                        long sourceStartNodeId = receivedRelationshipData.getSourceStartNodeId();
                                        long sourceEndNodeId = receivedRelationshipData.getSourceEndNodeId();

                                        long targetStartNodeId = idMap.get(sourceStartNodeId);
                                        long targetEndNodeId = idMap.get(sourceEndNodeId);

                                        // Get new start and end node
                                        AbstractNode targetStartNode = (AbstractNode) findNode.execute(new SuperUser(), targetStartNodeId);
                                        AbstractNode targetEndNode = (AbstractNode) findNode.execute(new SuperUser(), targetEndNodeId);
                                        String name = receivedRelationshipData.getName();
                                        
                                        StructrRelationship newRelationship = (StructrRelationship) createRel.execute(targetStartNode, targetEndNode, name);
                                        logger.log(Level.INFO, "New {3} relationship {0} created from remote data between {1} and {2}", new Object[]{newRelationship.getId(), targetStartNodeId, targetEndNodeId, name});

                                    }


                                }

                                return null;
                            }
                        });

                        connection.sendTCP("Node data received");

                    }




//                    } else if (object instanceof RelationshipDataContainer) {
//
//                        RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer) object;
//
//                        Command transactionCommand = Services.command(TransactionCommand.class);
//                        transactionCommand.execute(new StructrTransaction() {
//
//                            @Override
//                            public Object execute() throws Throwable {
//
//                                Service.command(CreateRelationshipCommand.class).execute();
//
//                                // Commit to database
//                                localNode.commit(null);
//
//                                // Connect with root node
//                                // TODO: Implement a smart strategy how to link nodes in target instance
//                                AbstractNode rootNode = (AbstractNode) Services.command(FindNodeCommand.class).execute(new SuperUser(), 0L);
//                                Services.command(CreateRelationshipCommand.class).execute(rootNode, localNode, RelType.HAS_CHILD);
//
//                                logger.log(Level.INFO, "New node {0} created from remote data", localNode.getIdString());
//
//                                // Update index
//                                Services.command(IndexNodeCommand.class).execute(localNode);
//
//                                return null;
//                            }
//                        });
//
//                        connection.sendTCP("Relationship data received");
//
//
//                    }
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

    public static void registerClasses(Kryo kryo) {
        kryo.register(HashMap.class);
        kryo.register(LinkedList.class);
        kryo.register(NodeDataContainer.class);
        kryo.register(RelationshipDataContainer.class);
    }
}
