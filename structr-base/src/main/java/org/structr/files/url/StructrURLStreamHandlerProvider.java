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
package org.structr.files.url;

import org.structr.common.SecurityContext;

import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;

/**
 * Makes Structr's virtual filesystem readable through a URL, so that libraries with no access to
 * Structr classes can be pointed at a file in the database. The scheme of such a URL is not a
 * protocol name but a one-off key: the caller stores its SecurityContext under a random string
 * (see {@link SecurityContext#storeTemporary}) and then opens "&lt;that string&gt;:/path/to/file",
 * which is how readShapefile() hands a shapefile to GeoTools.
 *
 * Registered as a service (module-info for the module path, META-INF/services for the class path)
 * rather than installed with URL.setURLStreamHandlerFactory: the JVM accepts only one factory ever,
 * so installing one is a race nobody can win twice, and it has to be done by code that is known to
 * run early. This provider is instead looked up by the JDK the first time an unknown scheme is
 * used, which needs no initialization order and no cooperation from any module.
 */
public class StructrURLStreamHandlerProvider extends URLStreamHandlerProvider {

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {

		// fetch temporarily stored security context based on the random UUID used as a protocol string in the URL
		final SecurityContext securityContext = SecurityContext.getTemporaryStoredContext(protocol);

		if (securityContext != null) {

			return new StructrURLStreamHandler(securityContext);
		}

		// not one of ours: let the JDK carry on looking
		return null;
	}
}
