package org.structr.cloud.transmission;

import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.message.PullNodeRequestContainer;
import org.structr.common.error.FrameworkException;

/**
 *
 * @author Christian Morgner
 */
public class PullTransmission extends AbstractTransmission<Boolean> {

	private boolean recursive  = false;
	private String rootNodeId  = null;

	public PullTransmission(final String rootNodeId, final boolean recursive, final String userName, final String password, final String remoteHost, final int port) {

		super(userName, password, remoteHost, port);

		this.rootNodeId = rootNodeId;
		this.recursive  = recursive;
	}

	@Override
	public int getTotalSize() {
		return 1;
	}

	@Override
	public Boolean doRemote(final CloudConnection client) throws IOException, FrameworkException {

		// send type of request
		client.send(new PullNodeRequestContainer(rootNodeId, recursive));

		// wait for end of transmission
		client.waitForTransmission();

		return true;
	}
}
