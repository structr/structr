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
package org.structr.test.core.service;

import org.structr.api.service.InitializationCallback;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.Tx;
import org.structr.test.common.StructrTest;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * A re-run callback must be able to open a transaction.
 *
 * That is the whole point of opting in: the callback exists to seed data into the database that just
 * replaced the one it seeded before. It is also exactly what deadlocked. The re-run was fired from inside
 * startService, which runs under activateService's WRITE lock on the reloading lock; the callback thread
 * then asked getDatabaseService() for the READ lock of that same lock and parked, while the thread holding
 * the write lock waited for the callback to finish. Neither could move, and the wizard request hung with
 * no exception and no timeout, on every fresh install.
 *
 * The timeout matters: without it a regression here does not fail the suite, it hangs it.
 */
public class InitializationCallbackRerunDeadlockTest extends StructrTest {

	@Test(timeOut = 120000)
	public void testARerunCallbackCanOpenATransaction() {

		final AtomicReference<String> failure = new AtomicReference<>(null);
		final AtomicBoolean opened            = new AtomicBoolean(false);

		final Services services = Services.getInstance();

		services.registerInitializationCallback(new InitializationCallback() {

			@Override
			public void initializationDone() {

				try (final Tx tx = StructrApp.getInstance().tx()) {

					opened.set(true);

					tx.success();

				} catch (Throwable t) {

					failure.set(t.toString());
				}
			}

			@Override
			public boolean rerunAfterDatabaseChange() {

				return true;
			}
		});

		try {

			// the wizard's path when a database connection is activated
			services.activateService(NodeService.class, services.getNameOfActiveService(NodeService.class));

		} catch (FrameworkException fex) {

			fex.printStackTrace();
			org.testng.AssertJUnit.fail("Unable to activate the node service: " + fex.getMessage());
		}

		assertNull("the re-run callback could not open a transaction: " + failure.get(), failure.get());
		assertTrue("the re-run callback did not run at all", opened.get());
	}
}
