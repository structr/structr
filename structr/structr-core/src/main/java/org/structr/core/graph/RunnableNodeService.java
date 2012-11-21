/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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



package org.structr.core.graph;

import org.apache.commons.lang.StringUtils;

import org.structr.core.RunnableService;
import org.structr.core.Service;
import org.structr.core.Services;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

//~--- classes ----------------------------------------------------------------

/**
 * A runnable service which runs in depends on NodeService
 *
 * Registers/unregisters on startup/shutdown with NodeService
 *
 * @author axel
 */
public abstract class RunnableNodeService extends Thread implements RunnableService {

	public RunnableNodeService() {}

	public RunnableNodeService(final String name) {
		super(name);
	}

	//~--- methods --------------------------------------------------------

	/**
	 * Register this service at the NodeService after starting
	 */
	@Override
	public void start() {

		super.start();

		List<Service> serviceList = Services.getServices();

		for (Service s : serviceList) {

			if (s instanceof NodeService) {
				((NodeService) s).registerService(this);
			}
		}
	}

	/*
	 * Unregister this service from the NodeService before interrupting
	 * the thread
	 */
	@Override
	public void interrupt() {

		List<Service> serviceList = Services.getServices();

		for (Service s : serviceList) {

			if (s instanceof NodeService) {
				((NodeService) s).unregisterService(this);
			}
		}

		super.interrupt();
	}
}
