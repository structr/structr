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
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.File;
import org.structr.core.entity.StructrRelationship;
import org.structr.core.entity.SuperUser;
import org.structr.core.entity.User;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author axel
 */
public class PushNodes extends CloudServiceCommand
{
	private static final Logger logger = Logger.getLogger(PushNodes.class.getName());

	@Override
	public Object execute(Object... parameters)
	{
		User user = null;
		AbstractNode sourceNode = null;
		long remoteTargetNodeId = 0L;
		String remoteHost = null;
		int remoteTcpPort = 0;
		int remoteUdpPort = 0;
		boolean recursive = false;

		Command findNode = Services.command(FindNodeCommand.class);

		switch(parameters.length)
		{
			case 0:
				throw new UnsupportedArgumentError("No arguments supplied");

			case 7:

				// user
				if(parameters[0] instanceof User)
				{
					user = (User)parameters[0];
				}

				// source node
				if(parameters[1] instanceof Long)
				{
					long id = ((Long)parameters[1]).longValue();
					sourceNode = (AbstractNode)findNode.execute(null, id);

				} else if(parameters[1] instanceof AbstractNode)
				{
					sourceNode = ((AbstractNode)parameters[1]);

				} else if(parameters[1] instanceof String)
				{
					long id = Long.parseLong((String)parameters[1]);
					sourceNode = (AbstractNode)findNode.execute(null, id);
				}

				// target node
				if(parameters[2] instanceof Long)
				{
					remoteTargetNodeId = ((Long)parameters[2]).longValue();
				}

				if(parameters[3] instanceof String)
				{
					remoteHost = (String)parameters[3];
				}

				if(parameters[4] instanceof Integer)
				{
					remoteTcpPort = (Integer)parameters[4];
				}

				if(parameters[5] instanceof Integer)
				{
					remoteUdpPort = (Integer)parameters[5];
				}

				if(parameters[6] instanceof Boolean)
				{
					recursive = (Boolean)parameters[6];
				}

				pushNodes(user, sourceNode, remoteTargetNodeId, remoteHost, remoteTcpPort, remoteUdpPort, recursive);
				break;

			default:
				break;
		}

		return null;
	}

	private void pushNodes(final User user, final AbstractNode sourceNode, final long remoteTargetNodeId, final String remoteHost, final int remoteTcpPort, final int remoteUdpPort, final boolean recursive)
	{
		final CloudService cloudService = (CloudService)Services.command(GetCloudServiceCommand.class).execute();
		final SynchronizingListener listener = new SynchronizingListener();
		final Value count = new Value();

		int chunkSize = CloudService.CHUNK_SIZE;
		int writeBufferSize = CloudService.BUFFER_SIZE * 4;
		int objectBufferSize = CloudService.BUFFER_SIZE * 2;

		Client client = new Client(writeBufferSize, objectBufferSize);
		client.addListener(listener);
		client.start();

		Log.set(CloudService.KRYONET_LOG_LEVEL);

		Kryo kryo = client.getKryo();
		CloudService.registerClasses(kryo);

		try
		{
			int estimatedSize = 0;	// FIXME!

			PushTransmission transmission = new PushTransmission(remoteHost, remoteTcpPort, remoteUdpPort, count, estimatedSize);
			cloudService.registerTransmission(transmission);

			// connect
			client.connect(10000, remoteHost, remoteTcpPort, remoteUdpPort);

			// mark start of transaction
			client.sendTCP(CloudService.BEGIN_TRANSACTION);
			count.value++;

			// send type of request
			client.sendTCP(new PushNodeRequestContainer(remoteTargetNodeId));
			count.value++;

			// send child nodes when recursive sending is requested
			if(recursive)
			{
				List<AbstractNode> nodes = sourceNode.getAllChildrenForRemotePush(new SuperUser()); // FIXME: use real user here

				for(AbstractNode n : nodes)
				{
					if(n instanceof File)
					{
						sendFile(client, listener, (File)n, chunkSize, count);

					} else
					{
						NodeDataContainer container = new NodeDataContainer(n);
						client.sendTCP(container);
						count.value++;
					}
				}

				for(AbstractNode n : nodes)
				{
					// Collect all relationships whose start and end nodes are contained in the above list
					List<StructrRelationship> rels = n.getOutgoingRelationships();
					for(StructrRelationship r : rels)
					{

						if(nodes.contains(r.getStartNode()) && nodes.contains(r.getEndNode()))
						{
							client.sendTCP(new RelationshipDataContainer(r));
							count.value++;
						}
					}
				}

			} else
			{
				// send start node
				if(sourceNode instanceof File)
				{
					sendFile(client, listener, (File)sourceNode, chunkSize, count);

				} else
				{
					// If not recursive, add only the node itself
					client.sendTCP(new NodeDataContainer(sourceNode));
					count.value++;
				}

			}

			logger.log(Level.INFO, "Data transmitted, sending END_TRANSACTION signal..");

			// mark end of transaction
			client.sendTCP(CloudService.END_TRANSACTION);
			count.value++;

			logger.log(Level.INFO, "Transmission done, {0} objects sent", count);

			cloudService.unregisterTransmission(transmission);


		} catch(IOException ex)
		{
			logger.log(Level.SEVERE, "Error while sending nodes to remote instance", ex);
		}

	}

	/**
	 * Splits the given file and sends it over the client connection. This
	 * method first creates a <code>FileNodeDataContainer</code> and sends
	 * it to the remote end. The file from disk is then split into multiple
	 * instances of <code>FileChunkContainer</code> while being sent. To
	 * finalize the transfer, a <code>FileNodeEndChunk</code> is sent to
	 * notify the receiving end of the successful transfer.
	 *
	 * @param client the client to send over
	 * @param file the file to split and send
	 * @param chunkSize the chunk size for a single chunk
	 *
	 * @return the number of objects that have been sent over the network
	 */
	private void sendFile(Client client, final SynchronizingListener listener, File file, int chunkSize, Value count)
	{
		// send file container first
		FileNodeDataContainer container = new FileNodeDataContainer(file);
		client.sendTCP(container);
		count.value++;

		// send chunks
		for(FileNodeChunk chunk : FileNodeDataContainer.getChunks(file, chunkSize))
		{
			try
			{
				client.sendTCP(chunk);

				// wait for remote end to confirm transmission
				listener.waitFor(chunk.getSequenceNumber());

			} catch(Throwable t)
			{
				break;
			}

			count.value++;
		}

		// mark end of file with special chunk
		client.sendTCP(new FileNodeEndChunk(container.getSourceNodeId(), container.getFileSize()));
		count.value++;
	}

	private class SynchronizingListener extends Listener
	{
		private long lastReceived = 0;
		private int currentValue = 0;

		@Override
		public void received(Connection connection, Object object)
		{
			if(object instanceof Integer)
			{
				Integer value = (Integer)object;
				currentValue = value.intValue();

				lastReceived = System.currentTimeMillis();
			}
		}

		public void waitFor(int sequenceNumber)
		{
			try
			{
				// wait for at most 10 seconds for arrival of correct sequence number
				while(currentValue != sequenceNumber && System.currentTimeMillis() < lastReceived + TimeUnit.SECONDS.toMillis(10))
				{
					Thread.yield();
				}

			} catch(Throwable t)
			{
				logger.log(Level.WARNING, "Exception while waiting for sequence number {0}: {1}", new Object[] { sequenceNumber, t } );
			}
		}
	}

	public static class PushTransmission implements CloudTransmission
	{
		private String remoteHost = null;
		private int estimatedSize = 0;
		private Value count = null;
		private int tcpPort = 0;
		private int udpPort = 0;

		public PushTransmission(String remoteHost, int tcpPort, int udpPort, Value count, int estimatedSize)
		{
			this.remoteHost = remoteHost;
			this.tcpPort = tcpPort;
			this.udpPort = udpPort;
			this.count = count;
			this.estimatedSize = estimatedSize;
		}

		@Override
		public TransmissionType getTransmissionType()
		{
			return(TransmissionType.Outgoing);
		}

		@Override
		public int getTransmittedObjectCount()
		{
			return(count.value);
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
			return(tcpPort);
		}

		@Override
		public int getRemoteUdpPort()
		{
			return(udpPort);
		}
	}

	public static class Value
	{
		public int value = 0;
	}
}
