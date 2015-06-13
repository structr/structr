package org.structr.xmpp.handler;

import org.structr.xmpp.XMPPContext;

/**
 *
 * @author Christian Morgner
 */
public interface TypeHandler<T> {

	public void handle(final XMPPContext.StructrXMPPConnection connection, final T packet);
}
