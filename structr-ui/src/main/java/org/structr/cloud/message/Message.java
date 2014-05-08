package org.structr.cloud.message;

import java.io.Serializable;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudContext;

/**
 *
 * @author Christian Morgner
 */
public interface Message<T> extends Serializable {

	public Message process(final CloudConnection connection, final CloudContext context);
	public void postProcess(final CloudConnection connection, final CloudContext context);

	public T getPayload();
}
