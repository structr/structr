package org.structr.cloud.transmission;

import java.io.IOException;
import org.structr.cloud.ClientConnection;
import org.structr.cloud.CloudContext;
import org.structr.cloud.CloudService;
import org.structr.cloud.ExportContext;
import org.structr.cloud.message.FileNodeChunk;
import org.structr.cloud.message.FileNodeEndChunk;
import org.structr.cloud.message.Message;
import org.structr.cloud.message.PullChunk;
import org.structr.cloud.message.PullFile;
import org.structr.cloud.message.PullNode;
import org.structr.cloud.message.PullNodeRequestContainer;
import org.structr.cloud.message.PullRelationship;
import org.structr.common.error.FrameworkException;
import org.structr.web.entity.File;

/**
 *
 * @author Christian Morgner
 */
public class PullTransmission extends AbstractTransmission<Boolean> {

	private boolean recursive  = false;
	private String rootNodeId  = null;

	public PullTransmission(final String rootNodeId, final boolean recursive, final String userName, final String password, final String remoteHost, final int port) {

		super(userName, password, remoteHost, port);

		this.rootNodeId = rootNodeId;
		this.recursive  = recursive;
	}

	@Override
	public int getTotalSize() {
		return 2;
	}

	@Override
	public Boolean doRemote(final ClientConnection client, final ExportContext context) throws IOException, FrameworkException {

		final CloudContext cloudContext = new CloudContext();

		// send type of request
		client.send(new PullNodeRequestContainer(rootNodeId, recursive));
		context.progress();

		final Message msg = client.waitForMessage();
		if (msg instanceof PullNodeRequestContainer) {

			final PullNodeRequestContainer response = (PullNodeRequestContainer)msg;
			final int numNodes                      = response.getNumNodes();
			final int numRels                       = response.getNumRels();
			final String key                        = response.getKey();

			context.increaseTotal(numNodes + numRels);

			for (int i=0; i<numNodes; i++) {

				client.send(new PullNode(key, i));
				final Message nodeMessage = client.waitForMessage();
				if (nodeMessage instanceof PullNode) {

					final PullNode pullNode = (PullNode)nodeMessage;
					if (File.class.isAssignableFrom(pullNode.getType())) {

						// pull file chunks
						client.send(new PullFile(key, i));
						final Message fileMessage = client.waitForMessage();
						if (fileMessage instanceof PullFile) {

							final PullFile pullFile = (PullFile)fileMessage;
							Message chunkMessage    = null;
							int sequenceNumber      = 0;

							context.increaseTotal((Long.valueOf(pullFile.getFileSize() / CloudService.CHUNK_SIZE).intValue()));

							cloudContext.beginFile(pullFile);
							context.progress();

							do {
								for (int j=0; j<CloudService.LIVE_PACKET_COUNT; j++) {
									client.send(new PullChunk(pullFile.getSourceNodeId(), sequenceNumber++, pullFile.getFileSize()));
								}

								for (int j=0; j<CloudService.LIVE_PACKET_COUNT; j++) {

									chunkMessage = client.waitForMessage();
									if (chunkMessage instanceof FileNodeChunk) {

										cloudContext.fileChunk((FileNodeChunk)chunkMessage);
										context.progress();

									} else {

										break;
									}
								}

							} while (!(chunkMessage instanceof FileNodeEndChunk));

							// end node chunk
							if (chunkMessage instanceof FileNodeEndChunk) {
								cloudContext.finishFile((FileNodeEndChunk)chunkMessage);
								context.progress();
							}
						}

					} else {

						cloudContext.storeNode(pullNode);
					}
				}
			}

			for (int i=0; i<numRels; i++) {

				client.send(new PullRelationship(key, i));
				final Message relMessage = client.waitForMessage();
				if (relMessage instanceof PullRelationship) {

					cloudContext.storeRelationship((PullRelationship)relMessage);
				}
			}
		}

		return true;
	}
}
