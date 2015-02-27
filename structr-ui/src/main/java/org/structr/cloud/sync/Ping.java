package org.structr.cloud.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudListener;
import org.structr.cloud.message.Message;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.SyncCommand;

/**
 *
 * @author Christian Morgner
 */
public class Ping extends Message {

	private String message = null;

	public Ping() { }

	public Ping(final String message) {

		this.message = message;
	}

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {

		final CloudListener listener = serverConnection.getListener();
		if (listener != null) {

			listener.transmissionProgress(message);
		}
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {
		this.message = (String)SyncCommand.deserialize(inputStream);
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {
		SyncCommand.serialize(outputStream, message);
	}
}
