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
package org.structr.test.files;

import org.structr.core.app.StructrApp;
import org.structr.files.sync.StorageSyncService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reaches the package-private quiescence methods of the StorageSyncService for the tests.
 *
 * This goes through reflection rather than living in the service's own package: the integration
 * tests run against the signed structr-base jar, and the JVM refuses an unsigned class in a package
 * whose other classes are signed.
 */
public final class SyncServiceProbe {

	private SyncServiceProbe() {}

	/**
	 * @return true if the service became idle within the given time, or is not running at all
	 */
	public static boolean awaitIdle(final long timeoutMillis) {

		final StorageSyncService service = service();

		return service == null || (Boolean) invoke(service, "awaitIdle", new Class<?>[] { long.class }, timeoutMillis);
	}

	/**
	 * @return what the service is busy with, for a failure message
	 */
	public static String describeActivity() {

		final StorageSyncService service = service();

		return service != null ? (String) invoke(service, "describeActivity", new Class<?>[0]) : "service not running";
	}

	/**
	 * @return the number of external change events the service has handled, or 0 if it is not running
	 */
	public static long handledEvents() {

		final StorageSyncService service = service();

		return service != null ? (Long) invoke(service, "getHandledEventCount", new Class<?>[0]) : 0L;
	}

	private static StorageSyncService service() {
		return StructrApp.getInstance().getService(StorageSyncService.class);
	}

	private static Object invoke(final StorageSyncService service, final String name, final Class<?>[] parameterTypes, final Object... arguments) {

		try {

			final Method method = StorageSyncService.class.getDeclaredMethod(name, parameterTypes);

			method.setAccessible(true);

			return method.invoke(service, arguments);

		} catch (InvocationTargetException ex) {

			throw new IllegalStateException("StorageSyncService." + name + " failed", ex.getCause());

		} catch (ReflectiveOperationException ex) {

			throw new IllegalStateException("StorageSyncService." + name + " is not reachable", ex);
		}
	}
}
