package org.structr.cloud;

import java.io.IOException;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public interface CloudTransmission<T> {

	public String getUserName();
	public String getPassword();
	public String getRemoteHost();
	public int getRemotePort();

	public int getTotalSize();

	public T doRemote(final CloudConnection<T> client) throws IOException, FrameworkException;
}
