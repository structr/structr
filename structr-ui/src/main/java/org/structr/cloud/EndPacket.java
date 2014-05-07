package org.structr.cloud;

/**
 *
 * @author Christian Morgner
 */
public class EndPacket implements Message {

	public EndPacket() {}

	@Override
	public Message process(final ServerContext context) {

		context.commitTransaction();
		context.endTransaction();

		context.closeConnection();

		return null;
	}

	@Override
	public void postProcess(final ServerContext context) {
	}

	@Override
	public Object getPayload() {
		return null;
	}
}
