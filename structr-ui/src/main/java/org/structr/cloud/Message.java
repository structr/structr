package org.structr.cloud;

import java.io.Serializable;

/**
 *
 * @author Christian Morgner
 */
public interface Message extends Serializable {

	public Message process(final ServerContext context);
}
