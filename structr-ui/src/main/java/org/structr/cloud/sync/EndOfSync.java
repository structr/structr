package org.structr.cloud.sync;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.message.Message;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class EndOfSync extends Message {

	@Override
	public void onRequest(CloudConnection serverConnection) throws IOException, FrameworkException {
	}

	@Override
	public void onResponse(CloudConnection clientConnection) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	protected void deserializeFrom(DataInputStream inputStream) throws IOException {
	}

	@Override
	protected void serializeTo(DataOutputStream outputStream) throws IOException {
	}
}
