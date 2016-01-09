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
package org.structr.common.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.structr.common.geo.GeoCodingResult.Type;

/**
 *
 *
 */
public class GoogleGeoCodingProvider extends AbstractGeoCodingProvider {

	private static final Logger logger = Logger.getLogger(GoogleGeoCodingProvider.class.getName());

	@Override
	public GeoCodingResult geocode(final String street, final String house, String postalCode, final String city, final String state, final String country, final String language) throws IOException {

		String address =
			
			(StringUtils.isNotBlank(street) ? street : "") + " " +
			(StringUtils.isNotBlank(house) ? house : "") + " " +
			(StringUtils.isNotBlank(postalCode) ? postalCode : "" +
			(StringUtils.isNotBlank(city) ? city : "") + " " +
			(StringUtils.isNotBlank(state) ? state : "") + " " +
			(StringUtils.isNotBlank(country) ? country : "") + " "
		);
		
		String encodedAddress;

		try {
			encodedAddress = URLEncoder.encode(address, "UTF-8");
		} catch (UnsupportedEncodingException ex) {

			logger.log(Level.WARNING, "Unsupported Encoding", ex);

			return null;
		}

		Document xmlDoc;

		try {

			String protocol              = "xml";    // "xml" or "json"
			URL mapsUrl                  = new URL("http://maps.google.com/maps/api/geocode/" + protocol + "?sensor=false&address=" + encodedAddress + "&language=" + language);
			HttpURLConnection connection = (HttpURLConnection) mapsUrl.openConnection();

			connection.connect();

			SAXReader reader  = new SAXReader();
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));

			xmlDoc = reader.read(rd);

			connection.disconnect();
			rd.close();

		} catch (IOException ioe) {

			logger.log(Level.WARNING, "Connection to geocoding service failed", ioe);

			return null;

		} catch (DocumentException de) {

			logger.log(Level.WARNING, "Could not read result document", de);

			return null;
		}

		Element root = xmlDoc.getRootElement();

		// List<Element> rootChildren = root.elements();
		String status = root.element("status").getTextTrim();
		if ("OK".equals(status)) {
			
			try {
				return new GoogleGeoCodingResult(address, root);
				
			} catch(Throwable t) {
				
				logger.log(Level.WARNING, "Unable to find geocoding for address {0}: {1}", new Object[] { address, t.getMessage() });
			}

		} else {

			logger.log(Level.WARNING, "Status not OK for address {0}: {1}", new Object[] { address, status });
		}

		return null;
	}

	//~--- inner classes --------------------------------------------------

	public static class GoogleGeoCodingResult implements GeoCodingResult {

		private List<AddressComponent> addressComponents = new LinkedList<>();
		private String address = null;
		private double latitude;
		private double longitude;
		
		//~--- constructors -------------------------------------------

		public GoogleGeoCodingResult(String address, Element root) {
			
			this.address = address;
			
			String latString  = root.element("result").element("geometry").element("location").element("lat").getTextTrim();
			String lonString  = root.element("result").element("geometry").element("location").element("lng").getTextTrim();

			Iterator<Element> addressComponentsElement = root.element("result").elementIterator("address_component");
			for(;addressComponentsElement.hasNext();) {

				addressComponents.add(new GoogleAddressComponent(addressComponentsElement.next()));
			}
			
			this.latitude     = Double.parseDouble(latString);
			this.longitude    = Double.parseDouble(lonString);
		}
		
		public GoogleGeoCodingResult(final double latitude, final double longitude) {

			this.latitude  = latitude;
			this.longitude = longitude;
		}

		//~--- get methods --------------------------------------------

		/**
		 * @return the latitude
		 */
		@Override
		public double getLatitude() {
			return latitude;
		}

		/**
		 * @return the longitude
		 */
		@Override
		public double getLongitude() {
			return longitude;
		}

		//~--- set methods --------------------------------------------

		/**
		 * @param latitude the latitude to set
		 */
		@Override
		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		/**
		 * @param longitude the longitude to set
		 */
		@Override
		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		@Override
		public Double[] toArray() {
			return new Double[]{ latitude, longitude };
		}

		@Override
		public String getAddress() {
			return address;
		}

		@Override
		public void setAddress(String address) {
			this.address = address;
		}

		@Override
		public AddressComponent getAddressComponent(Type... types) {
			
			for(AddressComponent addressComponent : addressComponents) {
				
				if(addressComponent.getTypes().containsAll(Arrays.asList(types))) {
					return addressComponent;
				}
			}
			
			return null;
		}
		
		@Override
		public List<AddressComponent> getAddressComponents() {
			return addressComponents;
		}
	}
	
	public static class GoogleAddressComponent implements AddressComponent {
		
		private Set<Type> types = new LinkedHashSet<Type>();
		private String shortValue = null;
		private String longValue = null;
		
		public GoogleAddressComponent(Element addressComponent) {
			
			this.shortValue = addressComponent.element("short_name").getTextTrim();
			this.longValue = addressComponent.element("long_name").getTextTrim();

			Iterator<Element> typesElement = addressComponent.elementIterator("type");
			for(;typesElement.hasNext();) {

				Element typeElement = typesElement.next();
				String typeName = typeElement.getTextTrim();
				
				try {
					this.types.add(Type.valueOf(typeName));
					
				} catch(Throwable t) {
					
					logger.log(Level.WARNING, "Encountered unknown address component type {0} while parsing.", typeName);
				}
				
			}
		}

		@Override
		public Set<Type> getTypes() {
			return types;
		}

		@Override
		public String getShortValue() {
			return shortValue;
		}

		@Override
		public String getLongValue() {
			return longValue;
		}
	}

}
