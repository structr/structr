/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud.transmission;

import java.io.IOException;
import org.structr.cloud.CloudConnection;
import org.structr.cloud.CloudTransmission;
import org.structr.cloud.message.PullNodeRequestContainer;
import org.structr.common.error.FrameworkException;

/**
 *
 *
 */
public class PullTransmission implements CloudTransmission<Boolean> {

	private boolean recursive  = false;
	private String rootNodeId  = null;

	public PullTransmission(final String rootNodeId, final boolean recursive) {

		this.rootNodeId = rootNodeId;
		this.recursive  = recursive;
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
