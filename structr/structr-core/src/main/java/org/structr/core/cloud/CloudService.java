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
import com.esotericsoftware.kryo.serialize.FieldSerializer;
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
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.RunnableNodeService;

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

	private final List<CloudTransmission> activeTransmissions = new LinkedList<CloudTransmission>();
	private final CloudServiceListener rootListener = new CloudServiceListener();

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

		registerClasses(kryo, rootListener);

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
	
	public static void registerClasses(Kryo kryo, CipherProvider keyProvider)
	{

		kryo.register(HashMap.class);
		kryo.register(LinkedList.class);

		// structr classes
		kryo.register(NodeDataContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, NodeDataContainer.class), keyProvider));
		kryo.register(FileNodeChunk.class, new EncryptingCompressor(new FieldSerializer(kryo, FileNodeChunk.class), keyProvider));
		kryo.register(FileNodeEndChunk.class, new EncryptingCompressor(new FieldSerializer(kryo, FileNodeEndChunk.class), keyProvider));
		kryo.register(FileNodeDataContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, FileNodeDataContainer.class), keyProvider));
		kryo.register(RelationshipDataContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, RelationshipDataContainer.class), keyProvider));
		kryo.register(PullNodeRequestContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, PullNodeRequestContainer.class), keyProvider));
		kryo.register(PushNodeRequestContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, PushNodeRequestContainer.class), keyProvider));

		// Neo4j array types
		kryo.register(String[].class, new EncryptingCompressor(new FieldSerializer(kryo, String[].class), keyProvider));
		kryo.register(char[].class, new EncryptingCompressor(new FieldSerializer(kryo, char[].class), keyProvider));
		kryo.register(byte[].class, new EncryptingCompressor(new FieldSerializer(kryo, byte[].class), keyProvider));
		kryo.register(boolean[].class, new EncryptingCompressor(new FieldSerializer(kryo, boolean[].class), keyProvider));
		kryo.register(int[].class, new EncryptingCompressor(new FieldSerializer(kryo, int[].class), keyProvider));
		kryo.register(long[].class, new EncryptingCompressor(new FieldSerializer(kryo, long[].class), keyProvider));
		kryo.register(short[].class, new EncryptingCompressor(new FieldSerializer(kryo, short[].class), keyProvider));
		kryo.register(float[].class, new EncryptingCompressor(new FieldSerializer(kryo, float[].class), keyProvider));
		kryo.register(double[].class, new EncryptingCompressor(new FieldSerializer(kryo, double[].class), keyProvider));
	}

}
