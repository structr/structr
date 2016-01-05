package org.structr.core.parser.function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.structr.common.error.FrameworkException;
import org.structr.common.geo.GeoCodingResult;
import org.structr.common.geo.GeoHelper;
import org.structr.core.GraphObject;
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
	public Object apply(final ActionContext ctx, final GraphObject entity, final Object[] sources) throws FrameworkException {

		if (arrayHasLengthAndAllElementsNotNull(sources, 3)) {

			final Gson gson = new GsonBuilder().create();
			final String street = sources[0].toString();
			final String city = sources[1].toString();
			final String country = sources[2].toString();

			GeoCodingResult result = GeoHelper.geocode(street, null, null, city, null, country);
			if (result != null) {

				final Map<String, Object> map = new LinkedHashMap<>();

				map.put("latitude", result.getLatitude());
				map.put("longitude", result.getLongitude());

				return serialize(gson, map);
			}

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
