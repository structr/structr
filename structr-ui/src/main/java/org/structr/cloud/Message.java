package org.structr.cloud;

import java.io.Serializable;

/**
 *
 * @author Christian Morgner
 */
public interface Message<T> extends Serializable {

	public Message process(final ServerContext context);
	public void postProcess(final ServerContext context);

	public T getPayload();
}
