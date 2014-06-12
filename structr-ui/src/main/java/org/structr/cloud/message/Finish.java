package org.structr.cloud.message;

import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class Finish extends Message {

	public Finish() {}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {
		serverConnection.send(new End());
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
