/*
 * Copyright (C) 2010-2025 Structr GmbH
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
import org.structr.rest.common.HttpHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

import java.util.Map;

/**
 *
 */
public abstract class UiFunction extends Function<Object, Object> {

	protected Map<String, Object> getFromUrl(final ActionContext ctx, final String requestUrl, final String charset, final String username, final String password) throws FrameworkException {

		return HttpHelper.get(requestUrl, charset, username, password, ctx.getHeaders(), ctx.isValidateCertificates());
	}

	protected Map<String, Object> getBinaryFromUrl(final ActionContext ctx, final String requestUrl, final String charset, final String username, final String password) throws FrameworkException {

		return HttpHelper.getBinary(requestUrl, charset, username, password, ctx.getHeaders(), ctx.isValidateCertificates());
	}

	protected Map<String, Object> headFromUrl(final ActionContext ctx, final String requestUrl, final String username, final String password) throws FrameworkException {

		return HttpHelper.head(requestUrl, password, username, ctx.getHeaders(), ctx.isValidateCertificates());
	}
}
