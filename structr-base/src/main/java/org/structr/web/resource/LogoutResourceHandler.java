/*
 * Copyright (C) 2010-2023 Structr GmbH
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
package org.structr.web.resource;

import java.util.Map;
import org.structr.rest.api.RESTCallHandler;
import org.structr.api.config.Settings;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.exception.NotAllowedException;

public class LogoutResourceHandler extends RESTCallHandler {

	public LogoutResourceHandler(final SecurityContext securityContext, final RESTCall call) {
		super(securityContext, call);
	}

	@Override
	public RestMethodResult doPost(final Map<String, Object> propertySet) throws FrameworkException {

		if (Settings.CallbacksOnLogout.getValue() == false) {
			securityContext.disableInnerCallbacks();
		}

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx(true, true, true)) {

			securityContext.getAuthenticator().doLogout(securityContext.getRequest());

			tx.success();
		}

		return new RestMethodResult(200);
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, final int pageSize, final int page) throws FrameworkException {
		throw new NotAllowedException("GET not allowed on " + getURL());
	}

	@Override
	public RestMethodResult doPut(final Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException("PUT not allowed on " + getURL());
	}

	@Override
	public RestMethodResult doDelete() throws FrameworkException {
		throw new NotAllowedException("DELETE not allowed on " + getURL());
	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new NotAllowedException("OPTIONS not allowed on " + getURL());
	}

	@Override
	public Class getEntityClass() {
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean createPostTransaction() {
		return false;
	}

	@Override
	public String getResourceSignature() {
		return "_logout";
	}
}
