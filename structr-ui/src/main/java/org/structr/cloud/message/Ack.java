package org.structr.cloud.message;

import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudContext;

/**
 *
 * @author Christian Morgner
 */
public class Ack implements Message {

	private int sequenceNumber = -1;
	private String message     = null;

	public Ack() {
		this(null, -1);
	}

	public Ack(final String message) {
		this(message, -1);
	}

	public Ack(final String message, final int sequenceNumber) {

		this.sequenceNumber = sequenceNumber;
		this.message        = message;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "AckPacket(" + message + ")";
	}

	@Override
	public Message process(CloudConnection connection, final CloudContext context) {

		context.ack(message, sequenceNumber);
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
