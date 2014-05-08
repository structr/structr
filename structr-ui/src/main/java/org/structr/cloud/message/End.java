package org.structr.cloud.message;

import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudContext;

/**
 *
 * @author Christian Morgner
 */
public class End implements Message {

	public End() {}

	@Override
	public Message process(CloudConnection connection, final CloudContext context) {

		context.commitTransaction();
		context.endTransaction();

		connection.closeConnection();

		return null;
	}

	@Override
	public void postProcess(CloudConnection connection, final CloudContext context) {
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
