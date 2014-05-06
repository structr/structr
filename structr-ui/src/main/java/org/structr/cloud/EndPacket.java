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

		return new EndPacket();
	}

	@Override
	public void postProcess(final ServerContext context) {
	}
}
