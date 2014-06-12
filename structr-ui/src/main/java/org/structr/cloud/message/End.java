package org.structr.cloud.message;

import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class End extends Message {

	public End() {}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {

		serverConnection.commitTransaction();
		serverConnection.endTransaction();

		serverConnection.send(this);
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
		clientConnection.close();
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
