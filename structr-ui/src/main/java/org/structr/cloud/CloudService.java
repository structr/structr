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
package org.structr.cloud;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.StructrConf;
import org.structr.common.SyncState;
import org.structr.common.Syncable;
import org.structr.common.error.FrameworkException;
import org.structr.core.Command;
import org.structr.core.RunnableService;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.Tx;
import org.structr.web.entity.File;

/**
 * The cloud service handles networking between structr instances
 *
 * @author axel
 */
public class CloudService extends Thread implements RunnableService {

	private static final Logger logger = Logger.getLogger(CloudService.class.getName());

	public static final int CHUNK_SIZE        = 32768;
	public static final int BUFFER_SIZE       = CHUNK_SIZE * 16;

	private final static int DefaultTcpPort = 54555;

	private ServerSocket serverSocket = null;
	private boolean running           = false;
	private int tcpPort               = DefaultTcpPort;

	public CloudService() {
		super("CloudService");
	}

	@Override
	public void injectArguments(Command command) {
	}

	@Override
	public void initialize(StructrConf config) {
		tcpPort = Integer.parseInt(config.getProperty(Services.TCP_PORT, "54555"));
	}

	@Override
	public void shutdown() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	public void startService() {

		try {

			serverSocket = new ServerSocket(tcpPort);

			running = true;
			start();

			logger.log(Level.INFO, "CloudService successfully started.");

		} catch (IOException ioex) {

			logger.log(Level.WARNING, "Unable to start CloudService." , ioex);
		}
	}

	@Override
	public void run() {

		while (running) {

			try {

				// start a new thread for the connection
				new ServerConnection(serverSocket.accept()).start();

			} catch (IOException ioex) {

				ioex.printStackTrace();
			}
		}
	}

	@Override
	public void stopService() {
		shutdown();
	}

	@Override
	public boolean runOnStartup() {
		return true;
	}

	// ----- public static methods -----
	public static void pushNodes(final String userName, String password, final Syncable sourceNode, final String remoteTargetNodeId, final String remoteHost, final int remoteTcpPort, final boolean recursive) throws FrameworkException {
		pushNodes(null, userName, password, sourceNode, remoteTargetNodeId, remoteHost, remoteTcpPort, recursive);
	}

	public static void pushNodes(final CloudListener listener, final String userName, String password, final Syncable sourceNode, final String remoteTargetNodeId, final String remoteHost, final int remoteTcpPort, final boolean recursive) throws FrameworkException {

		// construct an ExportContext with a total progress size of 4
		final ExportContext context = new ExportContext(listener, 4);
		ClientConnection client     = null;
		ExportSet exportSet         = null;

		try (final Tx tx = StructrApp.getInstance().tx()) {

			client = new ClientConnection(new Socket(remoteHost, remoteTcpPort));

			// create export set before first progress callback is called
			// so the client gets the correct total from the beginning
			if (recursive) {

				exportSet = getExportSet((Syncable)sourceNode, SyncState.all());
				context.increaseTotal(exportSet.getTotalSize());
			}

			// notify listener
			context.transmissionStarted();

			client.start();

			// mark start of transaction
			client.send(new BeginPacket());
			context.progress();

			final Message ack = client.waitForMessage();
			if (!(ack instanceof AckPacket)) {
				return;
			}

			// send authentication container
			client.send(new AuthenticationContainer(userName));
			context.progress();

			// wait for authentication container reply from server
			final Message authMessage = client.waitForMessage();
			if (authMessage instanceof AuthenticationContainer) {

				// send type of request
				client.send(new PushNodeRequestContainer(remoteTargetNodeId));
				context.progress();
				client.waitForMessage();

				// send child nodes when recursive sending is requested
				if (recursive && exportSet != null) {

					final Set<NodeInterface> nodes = exportSet.getNodes();

					for (final NodeInterface n : nodes) {

						if (n instanceof File) {

							sendFile(context, client, (File)n, CloudService.CHUNK_SIZE);

						} else {

							client.send(new NodeDataContainer(n));
							context.progress();
							client.waitForMessage();

						}
					}

					Set<RelationshipInterface> rels = exportSet.getRelationships();
					for (RelationshipInterface r : rels) {

						if (nodes.contains(r.getSourceNode()) && nodes.contains(r.getTargetNode())) {

							client.send(new RelationshipDataContainer(r));
							context.progress();
							client.waitForMessage();
						}
					}

				} else {

					// send start node
					if (sourceNode instanceof File) {

						sendFile(context, client, (File)sourceNode, CloudService.CHUNK_SIZE);

					} else {

						if (sourceNode.isNode()) {

							// If not recursive, add only the node itself
							client.send(new NodeDataContainer(sourceNode.getSyncNode()));
							context.progress();
							client.waitForMessage();

						} else {

							// If not recursive, add only the relationship itself
							client.send(new RelationshipDataContainer(sourceNode.getSyncRelationship()));
							context.progress();
							client.waitForMessage();

						}
					}

				}

			} else {

				// notify listener
				if (context != null) {
					context.transmissionAborted();
				}

			}

			// mark end of transaction
			client.send(new EndPacket());
			context.progress();
			client.waitForMessage();

			// wait for server to close connection here..
			client.waitForClose(2000);
			client.shutdown();

			// notify listener
			if (context != null) {
				context.transmissionFinished();
			}

		} catch (Throwable t) {

			t.printStackTrace();
		}
	}

	/**
	 * Splits the given file and sends it over the client connection. This method first creates a <code>FileNodeDataContainer</code> and sends it to the remote end. The file from disk is then
	 * split into multiple instances of <code>FileChunkContainer</code> while being sent. To finalize the transfer, a <code>FileNodeEndChunk</code> is sent to notify the receiving end of the
	 * successful transfer.
	 *
	 * @param client the client to send over
	 * @param file the file to split and send
	 * @param chunkSize the chunk size for a single chunk
	 *
	 * @return the number of objects that have been sent over the network
	 */
	private static void sendFile(final ExportContext context, final ClientConnection client, final File file, final int chunkSize) throws FrameworkException {

		// send file container first
		FileNodeDataContainer container = new FileNodeDataContainer(file);

		client.send(container);
		context.progress();

		// send chunks
		for (FileNodeChunk chunk : FileNodeDataContainer.getChunks(file, chunkSize)) {

			client.send(chunk);
			context.progress();

			// wait for remote end to confirm transmission
			client.waitForMessage();
		}

		// mark end of file with special chunk
		client.send(new FileNodeEndChunk(container.getSourceNodeId(), container.getFileSize()));
		context.progress();

		// wait for remote end to confirm transmission
		client.waitForMessage();
	}

	private static ExportSet getExportSet(final Syncable source, final SyncState state) {

		final ExportSet exportSet = new ExportSet();

		collectExportSet(exportSet, source, state);

		return exportSet;
	}

	private static void collectExportSet(final ExportSet exportSet, final Syncable start, final SyncState state) {

		exportSet.add(start);

		// collect children
		for (final Syncable child : start.getSyncData(state)) {

			if (child != null) {

				exportSet.add(start);
				collectExportSet(exportSet, child, state);
			}
		}
	}

	private static class ExportSet {

		private final Set<NodeInterface> nodes                 = new LinkedHashSet<>();
		private final Set<RelationshipInterface> relationships = new LinkedHashSet<>();
		private int size                                       = 0;

		public void add(final Syncable data) {

			if (data.isNode()) {

				if (nodes.add(data.getSyncNode())) {

					size++;

					if (data.getSyncNode() instanceof File) {

						final File file = (File)data.getSyncNode();

						size += (((File)data.getSyncNode()).getSize().intValue() / CloudService.CHUNK_SIZE) + 2;
					}
				}

			} else {

				if (relationships.add(data.getSyncRelationship())) {
					size++;
				}
			}
		}

		public Set<NodeInterface> getNodes() {
			return nodes;
		}

		public Set<RelationshipInterface> getRelationships() {
			return relationships;
		}

		public int getTotalSize() {
			return size;
		}
	}

	private static class ExportContext {

		private CloudListener listener = null;
		private int currentProgress    = 0;
		private int totalSize          = 0;

		public ExportContext(final CloudListener listener, final int totalSize) {
			this.listener  = listener;
			this.totalSize = totalSize;
		}

		public CloudListener getListener() {
			return listener;
		}

		public int getCurrentProgress() {
			return currentProgress;
		}

		public int getTotalSize() {
			return totalSize;
		}

		public void progress() {
			currentProgress++;

			if (listener != null) {
				listener.transmissionProgress(currentProgress, totalSize);
			}
		}

		public void increaseTotal(final int addTotal) {
			totalSize += addTotal;
		}

		public void transmissionStarted() {

			if (listener != null) {
				listener.transmissionStarted();
			}
		}

		public void transmissionFinished() {

			if (listener != null) {
				listener.transmissionFinished();
			}
		}

		public void transmissionAborted() {

			if (listener != null) {
				listener.transmissionAborted();
			}
		}
	}
}
