package org.structr.cloud.message;

import java.io.IOException;
import java.util.Iterator;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudService;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class PullChunk extends FileNodeChunk {

	public PullChunk(final String containerId, final int sequenceNumber, final long fileSize) {
		super(containerId, fileSize, sequenceNumber, CloudService.CHUNK_SIZE);
	}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		final Iterator<FileNodeChunk> chunkIterator = (Iterator<FileNodeChunk>)serverConnection.getValue(containerId);
		if (chunkIterator != null) {

			if (chunkIterator.hasNext()) {

				serverConnection.send(chunkIterator.next());

			} else {

				// chunk iterator is exhausted, remove it from context
				// so that only one FileNodeEndChunk is sent
				serverConnection.removeValue(containerId);

				// return finishing end chunk
				serverConnection.send(new FileNodeEndChunk(containerId, fileSize));
			}
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection conn) {
	}
}
