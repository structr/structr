package org.structr.cloud.transmission;

import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.message.Message;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class SingleTransmission<T> extends AbstractTransmission<T> {

	private Message<T> packet = null;

	public SingleTransmission(final Message<T> packet, final String userName, final String password, final String remoteHost, final int port) {

		super(userName, password, remoteHost, port);

		this.packet = packet;
	}

	@Override
	public int getTotalSize() {
		return 1;
	}

	@Override
	public T doRemote(CloudConnection<T> client) throws IOException, FrameworkException {

		client.send(packet);

		client.waitForTransmission();

		return client.getPayload();
	}
}
