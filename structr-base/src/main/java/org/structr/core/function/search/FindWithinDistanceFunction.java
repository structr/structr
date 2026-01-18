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
package org.structr.core.function.search;

import org.structr.common.error.FrameworkException;
import org.structr.core.function.AdvancedScriptingFunction;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.schema.action.ActionContext;

import java.util.List;

public class FindWithinDistanceFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "find.withinDistance";
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{ $.predicate.withinDistance(latitude, longitude, meters) }}. Example: ${{ $.find('Location', $.predicate.and($.predicate.withinDistance(51, 7, 10))) }}"),
			Usage.structrScript("Usage: ${withinDistance(latitude, longitude, meters). Example: ${find('Location', and(withinDistance(51, 7, 10)))}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns a query predicate that can be used with find() or search().";
	}

	@Override
	public String getLongDescription() {
		return "";
	}

	@Override
	public boolean isHidden() {
		return true;
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("latitude, longitude, distance");
	}

	@Override
	public List<Parameter> getParameters() {
		return List.of(
			Parameter.mandatory("latitude", "latitude of the center point"),
			Parameter.mandatory("longitude", "longitude of the center point"),
			Parameter.mandatory("distance", "circumference of the circle around the center point")
		);
	}
}
