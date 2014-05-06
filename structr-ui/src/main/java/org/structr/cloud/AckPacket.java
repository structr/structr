package org.structr.cloud;

/**
 *
 * @author Christian Morgner
 */
public class AckPacket implements Message {

	private int sequenceNumber = -1;
	private String message     = null;

	public AckPacket() {
		this(null, -1);
	}

	public AckPacket(final String message) {
		this(message, -1);
	}

	public AckPacket(final String message, final int sequenceNumber) {

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
	public Message process(final ServerContext context) {

		context.ack(message, sequenceNumber);
		return null;
	}
}
