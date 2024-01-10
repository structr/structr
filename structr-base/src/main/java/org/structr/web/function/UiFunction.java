/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.function;

import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.property.StringProperty;
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 */
public abstract class UiFunction extends Function<Object, Object> {

	protected String getFromUrl(final ActionContext ctx, final String requestUrl, final String charset, final String username, final String password) throws IOException, FrameworkException {

		return HttpHelper.get(requestUrl, charset, username, password, ctx.getHeaders(), ctx.isValidateCertificates());
	}

	protected byte[] getBinaryFromUrl(final ActionContext ctx, final String requestUrl, final String charset, final String username, final String password) throws IOException, FrameworkException {

		return HttpHelper.getBinary(requestUrl, charset, username, password, ctx.getHeaders(), ctx.isValidateCertificates());
	}

	protected GraphObjectMap headFromUrl(final ActionContext ctx, final String requestUrl, final String username, final String password) throws IOException, FrameworkException {

		final Map<String, String> headers = HttpHelper.head(requestUrl, password, username, ctx.getHeaders(), ctx.isValidateCertificates());

		final GraphObjectMap map = new GraphObjectMap();

		for (final Entry<String, String> entry : headers.entrySet()) {

			map.put(new StringProperty(entry.getKey()), entry.getValue());
		}

		return map;
	}
}
