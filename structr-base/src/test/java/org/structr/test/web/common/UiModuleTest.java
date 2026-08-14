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
package org.structr.test.web.common;

import org.structr.common.SecurityContext;
import org.structr.files.url.StructrURLConnection;
import org.structr.web.common.UiModule;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import static org.testng.AssertJUnit.*;

/**
 * Tests the URL stream handler installation of the UI module.
 *
 * The handler lets libraries with no access to Structr classes read from the virtual filesystem
 * through a "structr-&lt;uuid&gt;:&lt;path&gt;" URL, where the scheme names a temporarily stored
 * SecurityContext. It used to be installed from a static initializer, which under the module
 * system was never guaranteed to run: nothing references UiModule in code, it is picked up through
 * the provides clause in module-info, so the class could stay uninitialized and the handler
 * missing - with no error anywhere until a shapefile import failed on an unknown protocol.
 */
public class UiModuleTest {

	@Test
	public void testUrlStreamHandlerIsInstalled() throws Exception {

		UiModule.installUrlStreamHandlerFactory();

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		final String protocol                 = "structr-" + UUID.randomUUID().toString().replace("-", "");

		securityContext.storeTemporary(protocol);

		try {

			final URL url                  = new URL(protocol + ":/test/shapefile.shp");
			final URLConnection connection = url.openConnection();

			assertTrue("URL with a stored SecurityContext must open a Structr connection", connection instanceof StructrURLConnection);
			assertEquals("connection must be bound to the requested path", "/test/shapefile.shp", connection.getURL().getPath());

		} finally {

			securityContext.clearTemporary(protocol);
		}
	}

	@Test
	public void testUrlWithoutStoredContextIsRejected() throws Exception {

		UiModule.installUrlStreamHandlerFactory();

		// no SecurityContext stored for this scheme, so the factory returns no handler and the JVM
		// falls back to its own, which knows no such protocol. This is what the whole of Structr
		// looks like when the handler is not installed at all, which is what the static initializer
		// risked - so it is also what proves the test above is testing something.
		final String protocol = "structr-" + UUID.randomUUID().toString().replace("-", "");

		try {

			new URL(protocol + ":/test/shapefile.shp");

			fail("URL with an unknown scheme must not resolve");

		} catch (MalformedURLException expected) {
		}
	}

	@Test
	public void testRepeatedInstallationIsHarmless() {

		// onLoad() runs once per Services lifecycle, i.e. several times in a JVM that restarts the
		// service layer - as every test class does. A JVM accepts only one stream handler factory
		// and answers a second attempt with an Error, so installing again must be a no-op.
		UiModule.installUrlStreamHandlerFactory();
		UiModule.installUrlStreamHandlerFactory();
		UiModule.installUrlStreamHandlerFactory();

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		final String protocol                 = "structr-" + UUID.randomUUID().toString().replace("-", "");

		securityContext.storeTemporary(protocol);

		try {

			assertTrue("handler must still be installed after repeated module loads", new URL(protocol + ":/test/shapefile.shp").openConnection() instanceof StructrURLConnection);

		} catch (Exception unexpected) {

			fail("repeated installation broke the handler: " + unexpected);

		} finally {

			securityContext.clearTemporary(protocol);
		}
	}
}
