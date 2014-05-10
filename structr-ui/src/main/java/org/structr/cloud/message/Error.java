package org.structr.cloud.message;

import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class Error extends Message {

	private String message = null;
	private int errorCode  = 0;

	public Error() {}

	public Error(final int errorCode, final String message) {
		this.errorCode = errorCode;
		this.message   = message;
	}

	@Override
	public String toString() {
		return "Error(" + message + ")";
	}

	@Override
	public void onRequest(CloudConnection serverConnection, ExportContext context) throws IOException, FrameworkException {
		serverConnection.setError(errorCode, message);
		context.progress();
	}

	@Override
	public void onResponse(CloudConnection clientConnection, ExportContext context) throws IOException, FrameworkException {
		clientConnection.setError(errorCode, message);
		context.progress();
	}

	@Override
	public void afterSend(CloudConnection connection) {
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
