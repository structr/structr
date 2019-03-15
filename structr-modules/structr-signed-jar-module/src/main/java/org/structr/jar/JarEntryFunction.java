/**
 * Copyright (C) 2010-2018 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.jar;

import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;
import org.structr.web.function.UiFunction;

/**
 *
 */
public class JarEntryFunction extends UiFunction {

	@Override
	public String getName() {
		return "jar_entry";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndMaxLengthAndAllElementsNotNull(sources, 1, 2)) {

			return new NameAndContent(sources[0].toString(), sources[1].toString());

		} else {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return "jar_entry";
	}

	@Override
	public String shortDescription() {
		return "";
	}

}
