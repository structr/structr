/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.advanced;

import org.structr.api.service.RunnableService;
import org.structr.api.service.Service;
import org.structr.core.Services;
import org.structr.test.web.StructrUiTest;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

/**
 * Verifies that individual services can be stopped and started again at runtime
 * via the same API the config servlet uses (Services.shutdownService(name) /
 * startService(name)), without the service layer being torn down.
 *
 * This specifically guards the getInstance() "refuse to resurrect after full
 * shutdown" backstop: a per-service restart must never null the singleton or set
 * the shutdown flag, so restarting a service must not trip that guard.
 */
public class ServiceRestartTest extends StructrUiTest {

	@Test
	public void testRuntimeServiceRestartViaConfigServletApi() {

		final Services services = Services.getInstance();

		assertTrue("Service layer must be initialized at start", services.isInitialized());

		// the services touched by the CI-break fixes; restart each the way the
		// config servlet's Start/Stop/Restart buttons do
		for (final String serviceTypeName : new String[] { "AgentService", "StorageSyncService", "HttpService" }) {

			final Class serviceType = services.getServiceClassForName(serviceTypeName);
			assertNotNull("Service class '" + serviceTypeName + "' should be resolvable", serviceType);

			final Service service = services.getService(serviceType, "default");
			if (!(service instanceof RunnableService runnable) || !runnable.isRunning()) {

				// not running in this test's service set; skip
				continue;
			}

			final String serviceName = serviceTypeName + ".default";

			try {

				// stop (config servlet: ?stop=...)
				services.shutdownService(serviceName);

				// the service layer as a whole must remain up throughout
				assertNotNull("Singleton must not be nulled by a per-service stop", Services.peekInstance());
				assertTrue("Service layer must stay initialized during a per-service restart", services.isInitialized());

				// start again (config servlet: ?start=...)
				services.startService(serviceName);

				final Service restarted = services.getService(serviceType, "default");

				assertTrue("Service '" + serviceName + "' must be a RunnableService after restart", restarted instanceof RunnableService);
				assertTrue("Service '" + serviceName + "' must be running again after restart", ((RunnableService) restarted).isRunning());

			} catch (Exception ex) {

				ex.printStackTrace();
				fail("Runtime restart of '" + serviceName + "' failed: " + ex.getMessage());
			}
		}

		// the getInstance() backstop must not have fired: the singleton is intact
		// and getInstance() still returns it without throwing
		assertNotNull("Singleton must still exist after restarts", Services.peekInstance());
		assertSame("getInstance() must return the live singleton after restarts", Services.peekInstance(), Services.getInstance());
		assertTrue("Service layer must still be initialized after restarts", Services.getInstance().isInitialized());
	}
}
