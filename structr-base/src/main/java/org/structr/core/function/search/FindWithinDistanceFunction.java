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
package org.structr.core.function.search;

import org.structr.common.error.FrameworkException;
import org.structr.core.function.AdvancedScriptingFunction;
import org.structr.docs.Signature;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindWithinDistanceFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_AROUND_FIND = "Usage: ${within_distance(latitude, longitude, meters). Example: ${find('Location', and(within_distance(51, 7, 10)))}";

	@Override
	public String getName() {
		return "find.within_distance";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 3);

			if (sources.length == 3) {

				final double latitude  = this.getDoubleOrNull(sources[0]);
				final double longitude = this.getDoubleOrNull(sources[1]);
				final double distance  = this.getDoubleOrNull(sources[2]);

				return new LocationPredicate(latitude, longitude, distance);
			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());
		}

		return null;
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_AROUND_FIND;
	}

	@Override
	public String getShortDescription() {
		return "Returns a query predicate that can be used with find() or search().";
	}

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public List<Signature> getSignatures() {
		return null;
	}
}
