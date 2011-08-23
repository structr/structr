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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.UnsupportedArgumentError;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;
import org.structr.core.node.FindNodeCommand;

/**
 *
 * @author Christian Morgner
 */
public class PullNode extends CloudServiceCommand
{
	private static final Logger logger = Logger.getLogger(PullNode.class.getName());

	@Override
	public Object execute(Object... parameters)
	{
		User user = null;
		AbstractNode localTargetNode = null;
		long remoteSourceNodeId = 0L;
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
					remoteSourceNodeId = ((Long)parameters[1]).longValue();
				}

				// target node
				if(parameters[2] instanceof Long)
				{
					long id = ((Long)parameters[2]).longValue();
					localTargetNode = (AbstractNode)findNode.execute(null, id);

				} else if(parameters[2] instanceof AbstractNode)
				{
					localTargetNode = ((AbstractNode)parameters[2]);

				} else if(parameters[2] instanceof String)
				{
					long id = Long.parseLong((String)parameters[2]);
					localTargetNode = (AbstractNode)findNode.execute(null, id);
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

				pullNodes(user, remoteSourceNodeId, localTargetNode, remoteHost, remoteTcpPort, remoteUdpPort, recursive);
				break;

			default:
				break;
		}

		return null;
	}

	private void pullNodes(final User user, final long remoteSourceNodeId, AbstractNode localTargetNode, final String remoteHost, final int remoteTcpPort, final int remoteUdpPort, final boolean recursive)
	{
		int writeBufferSize = CloudService.BUFFER_SIZE * 4;
		int objectBufferSize = CloudService.BUFFER_SIZE * 2;

		Client client = new Client(writeBufferSize, objectBufferSize);
		client.start();

		Log.set(CloudService.KRYONET_LOG_LEVEL);

		throw new UnsupportedOperationException("PullNodes is not functional right now");
		
		/*
		
		Kryo kryo = client.getKryo();
		CloudService.registerClasses(kryo, new CipherProviderImpl(null));

		try
		{
			client.connect(5000, remoteHost, remoteTcpPort, remoteUdpPort);

			// mark start of transaction
			client.sendTCP(CloudService.BEGIN_TRANSACTION);
			count.value++;

			String localHost = Services.getServerIP();
			int serviceTcpPort = Integer.parseInt(Services.getTcpPort());
			int serviceUdpPort = Integer.parseInt(Services.getUdpPort());

			// tell remote instance to push some nodes :)
			PullNodeRequestContainer container = new PullNodeRequestContainer(user, remoteSourceNodeId, localTargetNode.getId(), localHost, serviceTcpPort, serviceUdpPort, recursive);
			client.sendTCP(container);
			count.value++;

			// mark end of transaction
			client.sendTCP(CloudService.END_TRANSACTION);
			count.value++;

			// mark end of transaction
			client.sendTCP(CloudService.CLOSE_TRANSACTION);
			count.value++;

			cloudService.unregisterTransmission(transmission);

		} catch(IOException ex)
		{
			logger.log(Level.SEVERE, "Error while sending nodes to remote instance", ex);
		}
		*/
	}
}
