package org.structr.cloud.message;

import java.io.IOException;
import java.io.Serializable;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.ExportContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeServiceCommand;

/**
 *
 * @author Christian Morgner
 */
public abstract class Message<T> implements Serializable {

	private String id = NodeServiceCommand.getNextUuid();

	public abstract void onRequest(final CloudConnection serverConnection, final ExportContext context) throws IOException, FrameworkException;
	public abstract void onResponse(final CloudConnection clientConnection, final ExportContext context) throws IOException, FrameworkException;
	public abstract void afterSend(final CloudConnection connection);

	public abstract T getPayload();

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getId() + ")";
	}

	protected void setId(final String id) {
		this.id = id;
	}

	protected Ack ack() {

		// create ack for this packet
		final Ack ack = new Ack();
		ack.setId(this.getId());

		return ack;
	}
}
