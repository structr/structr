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
import com.esotericsoftware.kryo.serialize.ArraySerializer;
import com.esotericsoftware.kryo.serialize.BooleanSerializer;
import com.esotericsoftware.kryo.serialize.ByteSerializer;
import com.esotericsoftware.kryo.serialize.CharSerializer;
import com.esotericsoftware.kryo.serialize.CollectionSerializer;
import com.esotericsoftware.kryo.serialize.DateSerializer;
import com.esotericsoftware.kryo.serialize.DoubleSerializer;
import com.esotericsoftware.kryo.serialize.FieldSerializer;
import com.esotericsoftware.kryo.serialize.FloatSerializer;
import com.esotericsoftware.kryo.serialize.IntSerializer;
import com.esotericsoftware.kryo.serialize.LongSerializer;
import com.esotericsoftware.kryo.serialize.MapSerializer;
import com.esotericsoftware.kryo.serialize.ShortSerializer;
import com.esotericsoftware.kryo.serialize.StringSerializer;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import java.io.IOException;
import java.util.Date;
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
public class CloudService extends RunnableNodeService {

	private static final Logger logger = Logger.getLogger(CloudService.class.getName());
	public static final Integer BEGIN_TRANSACTION = 0;			// initialize client / server
	public static final Integer END_TRANSACTION = 1;			// finish transmission
	public static final Integer CLOSE_TRANSACTION = 2;			// close channels
	public static final Integer ACK_DATA = 3;			// confirm reception
	public static final int CHUNK_SIZE = 2048;
	public static final int BUFFER_SIZE = CHUNK_SIZE * 16;
	public static final int KRYONET_LOG_LEVEL = Log.LEVEL_NONE;
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

	public CloudService() {
		super("CloudService");

		//this.setPriority(Thread.MIN_PRIORITY);
	}

	@Override
	public void injectArguments(Command command) {
		if (command != null) {
			command.setArgument("service", this);
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
		Log.set(CloudService.KRYONET_LOG_LEVEL);

		server = new Server(CloudService.BUFFER_SIZE * 4, CloudService.BUFFER_SIZE * 2);
		Kryo kryo = server.getKryo();

		registerClasses(kryo);

		server.start();

		logger.log(Level.INFO, "KryoNet server started");

		try {
			server.bind(tcpPort, udpPort);
			server.addListener(rootListener);

			logger.log(Level.INFO, "KryoNet server listening on TCP port {0} and UDP port {1}", new Object[]{
					tcpPort, udpPort
				});

		} catch (IOException ex) {
			logger.log(Level.SEVERE, "KryoNet server could not bind to TCP port " + tcpPort + " or UDP port " + udpPort, ex);
		}
	}

	@Override
	public void stopService() {
		shutdown();
	}

	@Override
	public boolean runOnStartup() {
		return (true);
	}

	public List<CloudTransmission> getActiveTransmissions() {
		return (activeTransmissions);
	}

	public void registerTransmission(CloudTransmission transmission) {
		activeTransmissions.add(transmission);
	}

	public void unregisterTransmission(CloudTransmission transmission) {
		activeTransmissions.remove(transmission);
	}

	public static void registerClasses(Kryo kryo) {
		kryo.register(AuthenticationContainer.class);

		// Java classes
		kryo.register(String.class, new EncryptingCompressor(new StringSerializer()));
		kryo.register(long.class, new EncryptingCompressor(new LongSerializer()));
		kryo.register(Long.class, new EncryptingCompressor(new LongSerializer()));
		kryo.register(int.class, new EncryptingCompressor(new IntSerializer()));
		kryo.register(Integer.class, new EncryptingCompressor(new IntSerializer()));
		kryo.register(short.class, new EncryptingCompressor(new ShortSerializer()));
		kryo.register(Short.class, new EncryptingCompressor(new ShortSerializer()));
		kryo.register(char.class, new EncryptingCompressor(new CharSerializer()));
		kryo.register(Character.class, new EncryptingCompressor(new CharSerializer()));
		kryo.register(byte.class, new EncryptingCompressor(new ByteSerializer()));
		kryo.register(Byte.class, new EncryptingCompressor(new ByteSerializer()));
		kryo.register(float.class, new EncryptingCompressor(new FloatSerializer()));
		kryo.register(Float.class, new EncryptingCompressor(new FloatSerializer()));
		kryo.register(double.class, new EncryptingCompressor(new DoubleSerializer()));
		kryo.register(Double.class, new EncryptingCompressor(new DoubleSerializer()));
		kryo.register(boolean.class, new EncryptingCompressor(new BooleanSerializer()));
		kryo.register(Boolean.class, new EncryptingCompressor(new BooleanSerializer()));
		kryo.register(Date.class, new EncryptingCompressor(new DateSerializer()));

		kryo.register(HashMap.class, new EncryptingCompressor(new MapSerializer(kryo)));
		kryo.register(LinkedList.class, new EncryptingCompressor(new CollectionSerializer(kryo)));

		// structr classes
		kryo.register(NodeDataContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, NodeDataContainer.class)));
		kryo.register(FileNodeChunk.class, new EncryptingCompressor(new FieldSerializer(kryo, FileNodeChunk.class)));
		kryo.register(FileNodeEndChunk.class, new EncryptingCompressor(new FieldSerializer(kryo, FileNodeEndChunk.class)));
		kryo.register(FileNodeDataContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, FileNodeDataContainer.class)));
		kryo.register(RelationshipDataContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, RelationshipDataContainer.class)));
		kryo.register(PullNodeRequestContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, PullNodeRequestContainer.class)));
		kryo.register(PushNodeRequestContainer.class, new EncryptingCompressor(new FieldSerializer(kryo, PushNodeRequestContainer.class)));

		// Neo4j array types
		kryo.register(String[].class, new EncryptingCompressor(new ArraySerializer(kryo)));
		kryo.register(char[].class, new EncryptingCompressor(new ArraySerializer(kryo)));
		kryo.register(byte[].class, new EncryptingCompressor(new ArraySerializer(kryo)));
		kryo.register(boolean[].class, new EncryptingCompressor(new ArraySerializer(kryo)));
		kryo.register(int[].class, new EncryptingCompressor(new ArraySerializer(kryo)));
		kryo.register(long[].class, new EncryptingCompressor(new ArraySerializer(kryo)));
		kryo.register(short[].class, new EncryptingCompressor(new ArraySerializer(kryo)));
		kryo.register(float[].class, new EncryptingCompressor(new ArraySerializer(kryo)));
		kryo.register(double[].class, new EncryptingCompressor(new ArraySerializer(kryo)));
	}

}
