/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.cloud;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.cloud.transmission.PushTransmission;
import org.structr.common.Syncable;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.rest.resource.MaintenanceParameterResource;

/**
 *
 * @author Axel Morgner
 */
public class PushNodesTestingCommand extends CloudServiceCommand implements MaintenanceCommand {

	private static final Logger logger = Logger.getLogger(PushNodesTestingCommand.class.getName());

	static {

		MaintenanceParameterResource.registerMaintenanceCommand("pushNodesTest", PushNodesTestingCommand.class);
	}

	@Override
	public void execute(final Map<String, Object> attributes) throws FrameworkException {

		final String name = (String)attributes.get("name");
		final String host = (String)attributes.get("host");

		if (name != null && host != null) {

			final App app     = StructrApp.getInstance();
			try (final Tx tx = app.tx()) {

				final NodeInterface entity = app.nodeQuery().andName(name).getFirst();
				if (entity != null && entity instanceof Syncable) {

					CloudService.doRemote(new PushTransmission((Syncable)entity, true, "admin", "admin", host, 54555), new LoggingListener());
				}
			}
		}
	}

	@Override
	public boolean requiresEnclosingTransaction() {
		return false;
	}

	private class LoggingListener implements CloudListener {

		@Override
		public void transmissionStarted() {
			logger.log(Level.INFO, "Transmission started");
		}

		@Override
		public void transmissionFinished() {
			logger.log(Level.INFO, "Transmission finished");
		}

		@Override
		public void transmissionAborted() {
			logger.log(Level.INFO, "Transmission aborted");
		}

		@Override
		public void transmissionProgress(int current, int total) {

			if ((current % 10) == 0) {
				logger.log(Level.INFO, "Transmission progress {0}/{1}", new Object[] { current, total } );
			}
		}
	}
}
