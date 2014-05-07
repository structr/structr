package org.structr.cloud.transmission;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.structr.cloud.ClientConnection;
import org.structr.cloud.CloudService;
import static org.structr.cloud.CloudService.LIVE_PACKET_COUNT;
import org.structr.cloud.ExportContext;
import org.structr.cloud.FileNodeChunk;
import org.structr.cloud.FileNodeDataContainer;
import org.structr.cloud.FileNodeEndChunk;
import org.structr.cloud.NodeDataContainer;
import org.structr.cloud.PushNodeRequestContainer;
import org.structr.cloud.RelationshipDataContainer;
import org.structr.common.SyncState;
import org.structr.common.Syncable;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.web.entity.File;

/**
 *
 * @author Christian Morgner
 */
public class PushTransmission extends AbstractTransmission<Boolean> {

	private final ExportSet exportSet = new ExportSet();
	private int sequenceNumber        = 0;

	public PushTransmission(final Syncable sourceNode, final boolean recursive, final String userName, final String password, final String remoteHost, final int port) {

		super(userName, password, remoteHost, port);

		// create export set before first progress callback is called
		// so the client gets the correct total from the beginning
		collectExportSet(exportSet, sourceNode, SyncState.all(), recursive);
	}

	@Override
	public int getTotalSize() {
		return exportSet.getTotalSize();
	}

	@Override
	public Boolean doRemote(final ClientConnection client, final ExportContext context) throws IOException, FrameworkException {

		// send type of request
		client.send(new PushNodeRequestContainer());
		context.progress();
		client.waitForMessage();

		// reset sequence number
		sequenceNumber = 0;

		// send child nodes when recursive sending is requested
		final Set<NodeInterface> nodes = exportSet.getNodes();
		for (final NodeInterface n : nodes) {

			if (n instanceof File) {

				sendFile(context, client, (File)n, CloudService.CHUNK_SIZE);

			} else {

				client.send(new NodeDataContainer(n, sequenceNumber));
				context.progress();

				// wait for response every N elements
				if (((sequenceNumber+1) % LIVE_PACKET_COUNT) == 0) {
					client.waitForMessage();
				}

				sequenceNumber++;
			}
		}

		// reset sequence number
		sequenceNumber = 0;

		// send relationships
		Set<RelationshipInterface> rels = exportSet.getRelationships();
		for (RelationshipInterface r : rels) {

			if (nodes.contains(r.getSourceNode()) && nodes.contains(r.getTargetNode())) {

				client.send(new RelationshipDataContainer(r, sequenceNumber));
				context.progress();

				// wait for response every N elements
				if (((sequenceNumber+1) % LIVE_PACKET_COUNT) == 0) {
					client.waitForMessage();
				}

				sequenceNumber++;
			}
		}

		return true;
	}

	private void collectExportSet(final ExportSet exportSet, final Syncable start, final SyncState state, boolean recursive) {

		exportSet.add(start);

		if (recursive) {

			// collect children
			for (final Syncable child : start.getSyncData(state)) {

				if (child != null && exportSet.add(child)) {

					collectExportSet(exportSet, child, state, recursive);
				}
			}
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
	private void sendFile(final ExportContext context, final ClientConnection client, final File file, final int chunkSize) throws FrameworkException, IOException {

		// send file container first
		FileNodeDataContainer container = new FileNodeDataContainer(file);

		client.send(container);
		context.progress();

		// send chunks
		for (FileNodeChunk chunk : FileNodeDataContainer.getChunks(file, chunkSize)) {

			client.send(chunk);
			context.progress();

			// wait for response every N chunks
			if (((chunk.getSequenceNumber()+1) % LIVE_PACKET_COUNT) == 0) {
				client.waitForMessage();
			}
		}

		// mark end of file with special chunk
		client.send(new FileNodeEndChunk(container.getSourceNodeId(), container.getFileSize()));
		context.progress();

		// wait for remote end to confirm transmission
		client.waitForMessage();
	}

	private static class ExportSet {

		private final Set<NodeInterface> nodes                 = new LinkedHashSet<>();
		private final Set<RelationshipInterface> relationships = new LinkedHashSet<>();
		private int size                                       = 0;

		public boolean add(final Syncable data) {

			if (data.isNode()) {

				if (nodes.add(data.getSyncNode())) {

					size++;

					if (data.getSyncNode() instanceof File) {

						size += (((File)data.getSyncNode()).getSize().intValue() / CloudService.CHUNK_SIZE) + 2;
					}

					// node was new (added), return true
					return true;
				}

			} else {

				if (relationships.add(data.getSyncRelationship())) {

					size++;

					// rel was new (added), return true
					return true;
				}
			}

			// arriving here means node was not added, so we return false
			return false;
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

}
