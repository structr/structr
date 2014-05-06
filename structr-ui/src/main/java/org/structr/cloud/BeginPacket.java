package org.structr.cloud;

/**
 *
 * @author Christian Morgner
 */
public class BeginPacket implements Message {

	public BeginPacket() {}

	@Override
	public Message process(final ServerContext context) {

		context.beginTransaction();

		return new AckPacket("Begin");
	}
}
