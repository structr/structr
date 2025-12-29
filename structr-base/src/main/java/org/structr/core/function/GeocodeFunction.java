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
package org.structr.core.function;

import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.AddressComponent;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.traits.definitions.LocationTraitDefinition;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GeocodeFunction extends AdvancedScriptingFunction {

	@Override
	public String getName() {
		return "geocode";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("street, city, country");
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

				map.put(LocationTraitDefinition.LATITUDE_PROPERTY, result.getLatitude());
				map.put(LocationTraitDefinition.LONGITUDE_PROPERTY, result.getLongitude());

				AddressComponent cur = null;

				cur = result.getAddressComponent(GeoCodingResult.Type.country);
				if(cur != null){
					map.put(LocationTraitDefinition.COUNTRY_PROPERTY, cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.postal_code);
				if(cur != null){
					map.put(LocationTraitDefinition.POSTAL_CODE_PROPERTY, cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.locality);
				if(cur != null){
					map.put(LocationTraitDefinition.CITY_PROPERTY, cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.route);
				if(cur != null){
					map.put(LocationTraitDefinition.STREET_PROPERTY, cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.street_number);
				if(cur != null){
					map.put(LocationTraitDefinition.HOUSE_NUMBER_PROPERTY, cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.administrative_area_level_1);
				if(cur != null){
					map.put(LocationTraitDefinition.STATE_PROPERTY, cur.getValue());
				}
				cur = result.getAddressComponent(GeoCodingResult.Type.administrative_area_level_3);
				if(cur != null){
					map.put(LocationTraitDefinition.STATE_DISTRICT_PROPERTY, cur.getValue());
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
	public List<Usage> getUsages() {
		return List.of(
			Usage.javaScript("Usage: ${{$.geocode(street, city, country)}}. Example: ${{$.set($.this, $.geocode($.this.street, $.this.city, $.this.country))}}"),
			Usage.structrScript("Usage: ${geocode(street, city, country)}. Example: ${set(this, geocode(this.street, this.city, this.country))}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Returns the geolocation (latitude, longitude) for the given street address using the configured geocoding provider.";
	}

	@Override
	public String getLongDescription() {
		return """
		Returns the geocoding result for the given parameters.
		See Geocoding Configuration for more information.
		This function returns a nested object with latitude / longitude that can directly be used in the `set()` method.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
				Example.structrScript("${set(this, geocode(this.street, this.city, this.country))}"),
				Example.javaScript("${{ $.set(this, $.geocode(this.street, this.city, this.country)) }}")
		);
	}

	@Override
	public List<String> getNotes() {
		return List.of(
			"An API Key (`geocoding.apikey`) has to be configured in structr.conf.",
			"Also this key is configurable through **Config -> Advanced Settings**."
		);
	}


	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("street", "street of place to geocode"),
			Parameter.mandatory("city", "city of place to geocode"),
			Parameter.mandatory("country", "country of place to geocode")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Geocoding;
	}
}
