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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class StoreFunction extends CoreFunction {

	@Override
	public String getName() {
		return "store";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllLanguages("key, value");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (sources != null && sources.length == 2 && sources[0] != null) {

				if (sources[1] == null) {

					ctx.getContextStore().remove(sources[0].toString());

				} else {

					ctx.store(sources[0].toString(), sources[1]);
				}
			}

			return "";

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
			Usage.structrScript("Usage: ${store(key, value)}. Example: ${store('tmpUser', this.owner)}"),
			Usage.javaScript("Usage: ${{Structr.store(key, value)}}. Example: ${{Structr.store('tmpUser', Structr.get('this').owner)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Stores the given value with the given key in the temporary store";
	}
}
