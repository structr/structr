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

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class RequestStorePutFunction extends UiAdvancedFunction {

	@Override
	public String getName() {
		return "requestStorePut";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("key, value");
	}

	@Override
	public Object apply(ActionContext ctx, Object caller, Object[] sources) throws FrameworkException {

		try {


			if (sources.length != 2) {
				throw ArgumentCountException.notEqual(sources.length, 2);
			}

			if (sources[0] == null) {
				throw new ArgumentNullException();
			}

			return ctx.getRequestStore().put(sources[0].toString(), sources[1]);
			
		} catch (ArgumentNullException pe) {

			// silently ignore null arguments
			return null;

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	@Override
	public List<Usage> getUsages() {
		return List.of(
			Usage.structrScript("Usage: ${requestStorePut(key,value)}. Example: ${requestStorePut('doNoTrack', true)}"),
			Usage.javaScript("Usage: ${{ $.requestStorePut(key,value); }}. Example: ${{ $.requestStorePut('doNotTrack', true); }}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Stores a value in the request level store.";
	}

	@Override
	public String getLongDescription() {
		return "";
	}
}
