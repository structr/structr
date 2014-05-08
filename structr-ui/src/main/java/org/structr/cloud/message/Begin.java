package org.structr.cloud.message;

import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudContext;

/**
 *
 * @author Christian Morgner
 */
public class Begin implements Message {

	public Begin() {}

	@Override
	public Message process(CloudConnection connection, final CloudContext context) {

		context.beginTransaction();

		return new Ack("Begin");
	}

	@Override
	public void postProcess(CloudConnection connection, final CloudContext context) {
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
