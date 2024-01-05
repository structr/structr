/*
 * Copyright (C) 2010-2024 Structr GmbH
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
import org.structr.common.geo.AddressComponent;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class GeocodeFunction extends AdvancedScriptingFunction {

	public static final String ERROR_MESSAGE_GEOCODE = "Usage: ${geocode(street, city, country)}. Example: ${set(this, geocode(this.street, this.city, this.country))}";

	@Override
	public String getName() {
		return "geocode";
	}

	@Override
	public String getSignature() {
		return "street, city, country";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 3);

			final String street = sources[0].toString();
			final String city = sources[1].toString();
			final String country = sources[2].toString();

			final GeoCodingResult result = GeoHelper.geocode(street, null, null, city, null, country);

			if (result != null) {

				final Map<String, Object> map = new LinkedHashMap<>();

				map.put("latitude", result.getLatitude());
				map.put("longitude", result.getLongitude());

				AddressComponent cur = null;

				cur = result.getAddressComponent(GeoCodingResult.Type.country);
				if(cur != null){
					map.put("country", cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.postal_code);
				if(cur != null){
					map.put("postalCode", cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.locality);
				if(cur != null){
					map.put("city", cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.route);
				if(cur != null){
					map.put("street", cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.street_number);
				if(cur != null){
					map.put("houseNumber", cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.administrative_area_level_1);
				if(cur != null){
					map.put("state", cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.administrative_area_level_3);
				if(cur != null){
					map.put("stateDistrict", cur.getValue());
				}

				return map;
			}

		} catch (ArgumentNullException pe) {

			// silently ignore null arguments

		} catch (ArgumentCountException pe) {

			logParameterError(caller, sources, pe.getMessage(), ctx.isJavaScriptContext());
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
