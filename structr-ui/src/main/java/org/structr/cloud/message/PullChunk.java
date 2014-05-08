package org.structr.cloud.message;

import java.util.Iterator;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudContext;
import org.structr.cloud.CloudService;

/**
 *
 * @author Christian Morgner
 */
public class PullChunk extends FileNodeChunk {

	public PullChunk(final String containerId, final int sequenceNumber, final long fileSize) {
		super(containerId, fileSize, sequenceNumber, CloudService.CHUNK_SIZE);
	}

	@Override
	public Message process(CloudConnection connection, final CloudContext context) {

		final Iterator<FileNodeChunk> chunkIterator = (Iterator<FileNodeChunk>)context.getValue(containerId);
		if (chunkIterator != null) {

			if (chunkIterator.hasNext()) {
				return chunkIterator.next();

			} else {

				// chunk iterator is exhausted, remove it from context
				// so that only one FileNodeEndChunk is sent
				context.removeValue(containerId);

				// return finishing end chunk
				return new FileNodeEndChunk(containerId, fileSize);
			}
		}

		return null;
	}
}
