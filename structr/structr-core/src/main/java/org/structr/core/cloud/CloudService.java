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
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
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
public class CloudService extends RunnableNodeService
{
	private static final Logger logger = Logger.getLogger(CloudService.class.getName());
	
	public static final Integer BEGIN_TRANSACTION =		0;	// initialize client / server
	public static final Integer END_TRANSACTION =		1;	// finish transmission
	public static final Integer CLOSE_TRANSACTION =		2;	// close channels
	public static final Integer ACK_DATA =			3;	// confirm reception

	public static final int CHUNK_SIZE =			 32768;
	public static final int BUFFER_SIZE =			131072;

	public static final int KRYONET_LOG_LEVEL =		Log.LEVEL_INFO;

	/** Containing addresses of all available structr instances */
	private static final Set<InstanceAddress> instanceAddresses = new LinkedHashSet<InstanceAddress>();

	/** Local KryoNet server remote clients can connect to */
	private Server server = null;
	private final static int DefaultTcpPort = 54555;
	private final static int DefaultUdpPort = 57555;
	private int tcpPort = DefaultTcpPort;
	private int udpPort = DefaultUdpPort;

	private final List<PullNodeRequestContainer> pullRequests = new LinkedList<PullNodeRequestContainer>();
	private final List<CloudTransmission> activeTransmissions = new LinkedList<CloudTransmission>();
	private final CloudServiceListener rootListener = new CloudServiceListener();

	private final Command findNode = Services.command(FindNodeCommand.class);
	private final Command createRel = Services.command(CreateRelationshipCommand.class);
	private final Command nodeFactory = Services.command(NodeFactoryCommand.class);

	public CloudService()
	{
		super("CloudService");

		//this.setPriority(Thread.MIN_PRIORITY);
	}

	@Override
	public void injectArguments(Command command)
	{
		if(command != null)
		{
			command.setArgument("service", this);
			command.setArgument("server", server);
			command.setArgument("instanceAddresses", instanceAddresses);
		}
	}

	@Override
	public void initialize(Map<String, Object> context)
	{

		tcpPort = Integer.parseInt(Services.getTcpPort());
		udpPort = Integer.parseInt(Services.getUdpPort());

	}

	@Override
	public void shutdown()
	{
		if(isRunning())
		{
			server.stop();
			server.close();
			server = null;
		}
	}

	@Override
	public boolean isRunning()
	{
		return (server != null);
	}

	@Override
	public void startService()
	{
		// Be quiet
		Log.set(CloudService.KRYONET_LOG_LEVEL);

		server = new Server(CloudService.BUFFER_SIZE * 4, CloudService.BUFFER_SIZE * 2);
		Kryo kryo = server.getKryo();
		registerClasses(kryo);

		server.start();

		logger.log(Level.INFO, "KryoNet server started");

		try
		{
			server.bind(tcpPort, udpPort);
			server.addListener(rootListener);

			logger.log(Level.INFO, "KryoNet server listening on TCP port {0} and UDP port {1}", new Object[]
				{
					tcpPort, udpPort
				});

		} catch(IOException ex)
		{
			logger.log(Level.SEVERE, "KryoNet server could not bind to TCP port " + tcpPort + " or UDP port " + udpPort, ex);
		}
	}

	@Override
	public void stopService()
	{
		shutdown();
	}

	public List<CloudTransmission> getActiveTransmissions()
	{
		return(activeTransmissions);
	}

	public void registerTransmission(CloudTransmission transmission)
	{
		activeTransmissions.add(transmission);
	}

	public void unregisterTransmission(CloudTransmission transmission)
	{
		activeTransmissions.remove(transmission);
	}

	private void handlePullRequests()
	{
		Runnable r = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Thread.sleep(2000);
					
					synchronized(pullRequests)
					{
						for(Iterator<PullNodeRequestContainer> it = pullRequests.iterator(); it.hasNext();)
						{
							PullNodeRequestContainer request = it.next();

							// swap source and target nodes since we're dealing with a request from the remote's point of view!
							AbstractNode sourceNode = (AbstractNode)findNode.execute(new SuperUser(), request.getSourceNodeId());
							boolean recursive = request.isRecursive();

							Command pushNodes = Services.command(PushNodes.class);

							User remoteUser = request.getRemoteUser();
							long remoteTargetNodeId = request.getTargetNodeId();
							String remoteHostValue = request.getRemoteHost();
							Integer remoteTcpPort = request.getRemoteTcpPort();
							Integer remoteUdpPort = request.getRemoteUdpPort();

							pushNodes.execute(remoteUser, sourceNode, remoteTargetNodeId, remoteHostValue, remoteTcpPort, remoteUdpPort, recursive);

							it.remove();
						}
					}

				} catch(Throwable t)
				{
					logger.log(Level.WARNING, "Error while handling pull requests: {0}", t);
				}
			}
		};

		new Thread(r, "PullRequestThread").start();
	}

	public static void registerClasses(Kryo kryo)
	{

		kryo.register(HashMap.class);
		kryo.register(LinkedList.class);

		// structr classes
		kryo.register(NodeDataContainer.class);
		kryo.register(FileNodeChunk.class);
		kryo.register(FileNodeEndChunk.class);
		kryo.register(FileNodeDataContainer.class);
		kryo.register(RelationshipDataContainer.class);
		kryo.register(PullNodeRequestContainer.class);
		kryo.register(PushNodeRequestContainer.class);

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

	/**
	 * This class is notified when an object arrives on any of the active connections,
	 * distributing the object to the correct listener for the given connection.
	 */
	private class CloudServiceListener extends Listener
	{
		private final Map<Connection, ConnectionListener> listeners = Collections.synchronizedMap(new WeakHashMap<Connection, ConnectionListener>());

		@Override
		public void received(Connection connection, Object object)
		{
			ConnectionListener listener = listeners.get(connection);
			if(listener != null)
			{
				listener.received(connection, object);
			}
		}

		@Override
		public void connected(Connection connection)
		{
			ConnectionListener listener = new ConnectionListener(connection);
			
			listeners.put(connection, listener);
			activeTransmissions.add(listener);

		}

		@Override
		public void disconnected(Connection connection)
		{
			ConnectionListener listener = listeners.get(connection);
			if(listener != null)
			{
				activeTransmissions.remove(listener);
				listeners.remove(connection);
			}
		}
	}

	/**
	 * Acts on a single connection, not on all.
	 */
	public class ConnectionListener extends Listener implements CloudTransmission
	{
		private final Map<Long, FileNodeDataContainer> fileMap = new HashMap<Long, FileNodeDataContainer>();
		private final Map<Long, Long> idMap = new HashMap<Long, Long>();

		private AbstractNode rootNode = (AbstractNode) Services.command(FindNodeCommand.class).execute(new SuperUser(), 0L);
		private boolean transactionFinished = false;
		private boolean linkNode = false;
		private String remoteHost = null;
		private int remoteTcpPort = 0;
		private int remoteUdpPort = 0;
		private int estimatedSize = 0;

		public ConnectionListener(Connection connection)
		{
			remoteHost = connection.getRemoteAddressTCP().getAddress().getHostAddress();
			remoteTcpPort = connection.getRemoteAddressTCP().getPort();
			remoteUdpPort = connection.getRemoteAddressUDP().getPort();
		}

		/**
		 * Returns the root node of this connection.
		 * 
		 * @return the root node
		 */
		public AbstractNode getRootNode()
		{
			return(rootNode);
		}

		@Override
		public void received(Connection connection, Object object)
		{
			// close connection on first keepalive package when transaction is done
			if(transactionFinished && object instanceof KeepAlive)
			{
				logger.log(Level.INFO, "Received first KeepAlive after transaction finished, closing connection.");
				connection.close();

			} else
			if(object instanceof Integer)
			{
				// Control signal received
				Integer controlSignal = (Integer) object;

				if(BEGIN_TRANSACTION.equals(controlSignal))
				{
					linkNode = true;

				} else
				if(END_TRANSACTION.equals(controlSignal))
				{
					logger.log(Level.INFO, "Received END_TRANSACTION signal, waiting for KeepAlive to close connection.");

					// idMap must be cleared AFTER the last nodes are created
					idMap.clear();

					transactionFinished = true;

				} else
				if(CLOSE_TRANSACTION.equals(controlSignal))
				{
					logger.log(Level.INFO, "Received CLOSE_TRANSACTION signal, closing connection..");
					// close the connection
					connection.close();

					handlePullRequests();
				}

				connection.sendTCP(CloudService.ACK_DATA);

			} else if(object instanceof PullNodeRequestContainer)
			{
				synchronized(pullRequests)
				{
					pullRequests.add((PullNodeRequestContainer)object);
				}

				connection.sendTCP(CloudService.ACK_DATA);

			} else if(object instanceof PushNodeRequestContainer)
			{
				PushNodeRequestContainer request = (PushNodeRequestContainer)object;

				// set desired root node for push request
				rootNode = (AbstractNode) Services.command(FindNodeCommand.class).execute(new SuperUser(), request.getTargetNodeId());

				connection.sendTCP(CloudService.ACK_DATA);

			} else if(object instanceof FileNodeDataContainer)
			{
				final FileNodeDataContainer container = (FileNodeDataContainer)object;
				fileMap.put(container.sourceNodeId, container);

				// FIXME
				estimatedSize += container.getFileSize();

				connection.sendTCP(CloudService.ACK_DATA);

			} else if(object instanceof FileNodeEndChunk)
			{
				final FileNodeEndChunk endChunk = (FileNodeEndChunk)object;
				final FileNodeDataContainer container = fileMap.get(endChunk.getContainerId());

				if(container == null)
				{
					logger.log(Level.WARNING, "Received file end chunk for ID {0} without file, this should not happen!", endChunk.getContainerId());

				} else
				{
					container.flushAndCloseTemporaryFile();

					// commit database node
					Command transactionCommand = Services.command(TransactionCommand.class);
					transactionCommand.execute(new StructrTransaction()
					{
						@Override
						public Object execute() throws Throwable
						{
							storeNode(container, linkNode);

							return null;
						}
					});

				}

				connection.sendTCP(CloudService.ACK_DATA);

			} else if(object instanceof FileNodeChunk)
			{
				final FileNodeChunk chunk = (FileNodeChunk)object;
				FileNodeDataContainer container = fileMap.get(chunk.getContainerId());

				if(container == null)
				{
					logger.log(Level.WARNING, "Received file chunk for ID {0} without file, this should not happen!", chunk.getContainerId());
				} else
				{
					container.addChunk(chunk);
				}

				// confirm reception of chunk with sequence number
				connection.sendTCP(chunk.getSequenceNumber());

			} else if(object instanceof NodeDataContainer)
			{
				final NodeDataContainer receivedNodeData = (NodeDataContainer) object;

				Command transactionCommand = Services.command(TransactionCommand.class);
				transactionCommand.execute(new StructrTransaction()
				{
					@Override
					public Object execute() throws Throwable
					{
						storeNode(receivedNodeData, linkNode);

						return null;
					}
				});

				connection.sendTCP(CloudService.ACK_DATA);

			} else if(object instanceof RelationshipDataContainer)
			{

				final RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer) object;

				Command transactionCommand = Services.command(TransactionCommand.class);
				transactionCommand.execute(new StructrTransaction()
				{
					@Override
					public Object execute() throws Throwable
					{

						storeRelationship(receivedRelationshipData);

						return null;
					}
				});

				connection.sendTCP(CloudService.ACK_DATA);
			}
		}

		private AbstractNode storeNode(final DataContainer receivedData, final boolean linkToRootNode)
		{
			NodeDataContainer receivedNodeData = (NodeDataContainer) receivedData;
			AbstractNode newNode = (AbstractNode) nodeFactory.execute(receivedNodeData);

			// Connect first node with root node
			if(linkToRootNode)
			{
				createRel.execute(rootNode, newNode, RelType.HAS_CHILD);

				// Reset link node flag, has to be explictly set to true!
				linkNode = false;
			}

			idMap.put(receivedNodeData.getSourceNodeId(), newNode.getId());

			return(newNode);
		}

		private StructrRelationship storeRelationship(final DataContainer receivedData)
		{
			RelationshipDataContainer receivedRelationshipData = (RelationshipDataContainer) receivedData;
			StructrRelationship newRelationship = null;

			long sourceStartNodeId = receivedRelationshipData.getSourceStartNodeId();
			long sourceEndNodeId = receivedRelationshipData.getSourceEndNodeId();

			Long targetStartNodeIdValue = idMap.get(sourceStartNodeId);
			Long targetEndNodeIdValue = idMap.get(sourceEndNodeId);

			if(targetStartNodeIdValue != null && targetEndNodeIdValue != null)
			{
				long targetStartNodeId = targetStartNodeIdValue.longValue();
				long targetEndNodeId = targetEndNodeIdValue.longValue();

				// Get new start and end node
				AbstractNode targetStartNode = (AbstractNode) findNode.execute(new SuperUser(), targetStartNodeId);
				AbstractNode targetEndNode = (AbstractNode) findNode.execute(new SuperUser(), targetEndNodeId);
				String name = receivedRelationshipData.getName();

				if(targetStartNode != null && targetEndNode != null && StringUtils.isNotEmpty(name))
				{
					newRelationship = (StructrRelationship) createRel.execute(targetStartNode, targetEndNode, name);

					// add properties to newly created relationship
					for(Entry<String, Object> entry : receivedRelationshipData.getProperties().entrySet())
					{
						newRelationship.setProperty(entry.getKey(), entry.getValue());
					}
				}
			} else
			{
				logger.log(Level.WARNING, "Could not store relationship {0} -> {1}", new Object[] { sourceStartNodeId, sourceEndNodeId } );
			}

			return newRelationship;
		}

		// ----- interface CloudTransmission -----
		@Override
		public TransmissionType getTransmissionType()
		{
			return(TransmissionType.Incoming);
		}

		@Override
		public int getTransmittedObjectCount()
		{
			return(idMap.size());
		}

		@Override
		public int getEstimatedSize()
		{
			return(estimatedSize);
		}

		@Override
		public String getRemoteHost()
		{
			return(remoteHost);
		}

		@Override
		public int getRemoteTcpPort()
		{
			return(remoteTcpPort);
		}

		@Override
		public int getRemoteUdpPort()
		{
			return(remoteUdpPort);
		}

	}
}
