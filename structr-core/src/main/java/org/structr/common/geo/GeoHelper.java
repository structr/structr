/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.common.geo;


import java.util.logging.Level;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Location;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.PropertyMap;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import org.structr.core.graph.search.DistanceSearchAttribute;

//~--- classes ----------------------------------------------------------------

/**
 * Helper class to create location nodes from coordinates or by using
 * online geocoding service.
 *
 * @author Axel Morgner
 */
public class GeoHelper {

	private static final Logger logger                    = Logger.getLogger(GeoHelper.class.getName());
	private static Class<GeoCodingProvider> providerClass = null;

	/**
	 * Creates a Location entity for the given geocoding result and returns it.
	 * 
	 * @param coords
	 * @return a Location entity for the given geocoding result
	 * 
	 * @throws FrameworkException 
	 */
	public static Location createLocation(final GeoCodingResult coords) throws FrameworkException {

		final PropertyMap props = new PropertyMap();
		double latitude         = coords.getLatitude();
		double longitude        = coords.getLongitude();
		String type             = Location.class.getSimpleName();

		props.put(AbstractNode.type,  type);
		props.put(Location.latitude,  latitude);
		props.put(Location.longitude, longitude);

		StructrTransaction transaction = new StructrTransaction<AbstractNode>() {

			@Override
			public AbstractNode execute() throws FrameworkException {
				return Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(props);
			}
		};

		return (Location) Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(transaction);
	}

	public static GeoCodingResult geocode(DistanceSearchAttribute distanceSearch) throws FrameworkException {
		
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
		
		GeoCodingProvider provider = getGeoCodingProvider();
		if (provider != null) {
			
			String language = Services.getConfigurationValue(Services.GEOCODING_LANGUAGE, "de");
			
			return provider.geocode(street, house, postalCode, city, state, country, language);
		}
		
		return null;
	}
	
	private static GeoCodingProvider getGeoCodingProvider() {

		try {

			if (providerClass == null) {
				
				String geocodingProvider = Services.getConfigurationValue(Services.GEOCODING_PROVIDER, GoogleGeoCodingProvider.class.getName());
				providerClass = (Class<GeoCodingProvider>)Class.forName(geocodingProvider);
			}

			return providerClass.newInstance();
		
		} catch (Throwable t) {
			
			logger.log(Level.WARNING, "Unable to instantiate geocoding provider: {0}", t.getMessage() );
		}
		
		return null;
	}
}
