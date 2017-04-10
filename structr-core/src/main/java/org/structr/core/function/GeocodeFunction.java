/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;

/**
 *
 */
public class GeocodeFunction extends Function<Object, Object> {

	public static final String ERROR_MESSAGE_GEOCODE = "Usage: ${geocode(street, city, country)}. Example: ${set(this, geocode(this.street, this.city, this.country))}";

	@Override
	public String getName() {
		return "geocode()";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

				final String street = sources[0].toString();
				final String city = sources[1].toString();
				final String country = sources[2].toString();

				final GeoCodingResult result = GeoHelper.geocode(street, null, null, city, null, country);

				if (result != null) {

					final Map<String, Object> map = new LinkedHashMap<>();

					map.put("latitude", result.getLatitude());
					map.put("longitude", result.getLongitude());

					return map;
				}


			}

		} catch (final IllegalArgumentException e) {

			logParameterError(caller, sources, ctx.isJavaScriptContext());

			return usage(ctx.isJavaScriptContext());

		}

		return "";
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return ERROR_MESSAGE_GEOCODE;
	}

	@Override
	public String shortDescription() {
		return "Returns the geolocation (latitude, longitude) for the given street address using the configured geocoding provider";
	}

}
