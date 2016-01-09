/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.parser.function;

import java.util.Arrays;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.Function;

/**
 *
 */
public class CallFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_CALL    = "Usage: ${call(key [, payloads...]}. Example ${call('myEvent')}";
	public static final String ERROR_MESSAGE_CALL_JS = "Usage: ${{Structr.call(key [, payloads...]}}. Example ${{Structr.call('myEvent')}}";

	@Override
	public String getName() {
		return "call()";
	}

	@Override
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasMinLengthAndAllElementsNotNull(sources, 1)) {

			final String key = sources[0].toString();

			if (sources.length > 1) {

				return Actions.call(key, Arrays.copyOfRange(sources, 1, sources.length));

			} else {

				return Actions.call(key);
			}
		}

		return "";
	}


	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_CALL_JS : ERROR_MESSAGE_CALL);
	}

	@Override
	public String shortDescription() {
		return "Calls the given exported / dynamic method on the given entity";
	}

}
