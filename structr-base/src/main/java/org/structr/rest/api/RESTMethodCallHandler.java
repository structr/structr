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
package org.structr.rest.api;

import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedScriptException;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.Tx;
import org.structr.rest.RestMethodResult;
import org.structr.schema.action.EvaluationHints;

/**
 */
public abstract class RESTMethodCallHandler extends RESTCallHandler {

	protected AbstractMethod method = null;

	public RESTMethodCallHandler(final RESTCall call, final AbstractMethod method) {

		super(call);

		this.method = method;
	}

	// ----- protected methods -----
	protected RestMethodResult executeMethod(final SecurityContext securityContext, final GraphObject entity, final Arguments arguments) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			if (method.shouldReturnRawResult()) {

				securityContext.enableReturnRawResult();
			}

			final RestMethodResult result = wrapInResult(method.execute(securityContext, entity, arguments, new EvaluationHints()));

			tx.success();

			return result;

		} catch (UnlicensedScriptException ex) {
			return new RestMethodResult(500, "Call to unlicensed function, see server log file for more details.");
		}
	}
}
