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
package org.structr.common.geo;


import org.apache.commons.collections.map.LRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.search.DistanceSearchAttribute;
import org.structr.core.property.PropertyMap;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.LocationTraitDefinition;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * Helper class to create location nodes from coordinates or by using
 * online geocoding service.
 */
public class GeoHelper {

	private static final Logger logger                         = LoggerFactory.getLogger(GeoHelper.class.getName());
	private static final Map<String, GeoCodingResult> geoCache = Collections.synchronizedMap(new LRUMap(10000));
	private static Class<GeoCodingProvider> providerClass      = null;
	private static GeoCodingProvider providerInstance          = null;

	/**
	 * Creates a Location entity for the given geocoding result and returns it.
	 *
	 * @param coords
	 * @return a Location entity for the given geocoding result
	 *
	 * @throws FrameworkException
	 */
	public static NodeInterface createLocation(final GeoCodingResult coords) throws FrameworkException {

		final Traits traits     = Traits.of(StructrTraits.LOCATION);
		final PropertyMap props = new PropertyMap();
		double latitude         = coords.getLatitude();
		double longitude        = coords.getLongitude();

		props.put(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), StructrTraits.LOCATION);
		props.put(traits.key(LocationTraitDefinition.LATITUDE_PROPERTY),  latitude);
		props.put(traits.key(LocationTraitDefinition.LONGITUDE_PROPERTY), longitude);

		return StructrApp.getInstance().create(StructrTraits.LOCATION, props);
	}

	public static GeoCodingResult geocode(final DistanceSearchAttribute distanceSearch) throws FrameworkException {

		String street     = distanceSearch.getStreet();
		String house      = distanceSearch.getHouse();
		String postalCode = distanceSearch.getPostalCode();
		String city       = distanceSearch.getCity();
		String state      = distanceSearch.getState();
		String country    = distanceSearch.getCountry();

		return geocode(street, house, postalCode, city, state, country);
	}

	/**
	 * Tries do find a geo location for the given address using the GeoCodingProvider
	 * specified in the configuration file.

	 * @param country the country to search for, may be null
	 * @param state the state to search for, may be null
	 * @param city the city to search for, may be null
	 * @param street the street to search for, may be null
	 * @param house the house to search for, may be null
	 *
	 * @return the geolocation of the given address, or null
	 *
	 * @throws FrameworkException
	 */
	public static GeoCodingResult geocode(final String street, final String house, String postalCode, final String city, final String state, final String country) throws FrameworkException {

		final String language  = Settings.GeocodingLanguage.getValue();
		final String cacheKey  = cacheKey(street, house, postalCode, city, state, country, language);
		GeoCodingResult result = geoCache.get(cacheKey);

		if (result == null) {

			GeoCodingProvider provider = getGeoCodingProvider();
			if (provider != null) {

				try {

					result = provider.geocode(street, house, postalCode, city, state, country, language);
					if (result != null) {

						// store in cache
						geoCache.put(cacheKey, result);
					}

				} catch (IOException ioex) {

					// IOException, try again next time
					logger.warn("Unable to obtain geocoding result using provider {}: {}", new Object[] { provider.getClass().getName(), ioex.getMessage() });
				}
			}

		}

		return result;
	}

	private static String cacheKey(final String street, final String house, String postalCode, final String city, final String state, final String country, final String language) {

		StringBuilder keyBuffer = new StringBuilder();

		if (street != null) {
			keyBuffer.append(street);
		}

		if (house != null) {
			keyBuffer.append(house);
		}

		if (postalCode != null) {
			keyBuffer.append(postalCode);
		}

		if (city != null) {
			keyBuffer.append(city);
		}

		if (state != null) {
			keyBuffer.append(state);
		}

		if (country != null) {
			keyBuffer.append(country);
		}

		if (language !=  null) {
			keyBuffer.append(language);
		}

		return keyBuffer.toString();
	}

	private static GeoCodingProvider getGeoCodingProvider() {

		if (providerInstance == null) {

			try {

				if (providerClass == null) {

					providerClass = (Class<GeoCodingProvider>)Class.forName(Settings.GeocodingProvider.getValue());
				}

				providerInstance = providerClass.getDeclaredConstructor().newInstance();

			} catch (Throwable t) {

				logger.warn("Unable to instantiate geocoding provider: {}", t.getMessage() );
			}
		}

		return providerInstance;
	}
}
