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
package org.structr.test.files.url;

import org.structr.common.SecurityContext;
import org.structr.files.url.StructrURLConnection;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import static org.testng.AssertJUnit.*;

/**
 * Tests that Structr's URL stream handler is found by the JDK on its own.
 *
 * The handler lets libraries with no access to Structr classes read from the virtual filesystem: a
 * caller stores its SecurityContext under a random key and opens "&lt;key&gt;:/path/to/file", which
 * is how readShapefile() hands a shapefile to GeoTools. Nothing in these tests installs anything -
 * that is the point. The provider is declared as a service (module-info on the module path,
 * META-INF/services on the class path, which is how tests run) and the JDK looks it up the first
 * time an unknown scheme is used.
 *
 * It used to be installed with URL.setURLStreamHandlerFactory from a static initializer in
 * UiModule. That was unsound twice over: the JVM accepts exactly one factory per process, and under
 * the module system nothing references UiModule in code - it is reached through a provides clause -
 * so the initializer could run late or not at all, leaving the handler missing with no error
 * anywhere until a shapefile import failed on an unknown protocol.
 */
public class StructrURLStreamHandlerProviderTest {

	@Test
	public void testHandlerIsFoundWithoutAnyInstallation() throws Exception {

		final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
		final String scheme                   = temporaryScheme();

		securityContext.storeTemporary(scheme);

		try {

			final URLConnection connection = new URL(scheme + ":/test/shapefile.shp").openConnection();

			assertTrue("a URL whose scheme names a stored SecurityContext must open a Structr connection", connection instanceof StructrURLConnection);
			assertEquals("the connection must address the requested path", "/test/shapefile.shp", connection.getURL().getPath());

		} finally {

			securityContext.clearTemporary(scheme);
		}
	}

	@Test
	public void testHandlerIsFoundRepeatedly() throws Exception {

		// the JDK caches stream handlers per scheme, and every caller brings a new one, so the
		// provider is asked again for every single URL - not only for the first
		for (int i = 0; i < 3; i++) {

			final SecurityContext securityContext = SecurityContext.getSuperUserInstance();
			final String scheme                   = temporaryScheme();

			securityContext.storeTemporary(scheme);

			try {

				assertTrue("handler must be provided for every new scheme", new URL(scheme + ":/test/file" + i + ".shp").openConnection() instanceof StructrURLConnection);

			} finally {

				securityContext.clearTemporary(scheme);
			}
		}
	}

	@Test
	public void testSchemeWithoutStoredContextIsRejected() {

		// the provider answers only for schemes it has a SecurityContext for; anything else has to
		// fall through to the JDK, which knows no such protocol
		try {

			new URL(temporaryScheme() + ":/test/shapefile.shp");

			fail("a scheme with no stored SecurityContext must not resolve");

		} catch (MalformedURLException expected) {
		}
	}

	private String temporaryScheme() {

		return "structr-" + UUID.randomUUID().toString().replace("-", "");
	}
}
