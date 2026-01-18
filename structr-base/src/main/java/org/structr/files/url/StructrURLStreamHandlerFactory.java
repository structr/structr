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
import java.net.URLStreamHandlerFactory;

/**
 */
public class StructrURLStreamHandlerFactory implements URLStreamHandlerFactory {

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {

		// fetch temporarily stored security context based on the random UUID used as a protocol string in the URL
		final SecurityContext ctx = SecurityContext.getTemporaryStoredContext(protocol);
		if (ctx != null) {

			return new StructrURLStreamHandler(ctx);
		}

		return null;
	}
}