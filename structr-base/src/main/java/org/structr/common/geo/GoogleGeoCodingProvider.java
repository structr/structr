/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.geo.GeoCodingResult.Type;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 */
public class GoogleGeoCodingProvider extends AbstractGeoCodingProvider {

	private static final Logger logger = LoggerFactory.getLogger(GoogleGeoCodingProvider.class.getName());

	private int count = 0;

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

			logger.warn("Unsupported Encoding", ex);

			return null;
		}

		Document xmlDoc;

		try {

			StringBuilder urlBuffer = new StringBuilder("https://maps.google.com/maps/api/geocode/");

			// output format XML
			urlBuffer.append("xml");

			// set the address
			urlBuffer.append("?address=").append(encodedAddress);

			// set the ouput language
			urlBuffer.append("&language=").append(language);

			// set the api key if there is any
			if (apiKey != null && !apiKey.isEmpty()) {
				urlBuffer.append("&key=").append(apiKey);
			}

			URL mapsUrl                  = new URL(urlBuffer.toString());
			HttpURLConnection connection = (HttpURLConnection) mapsUrl.openConnection();

			connection.connect();

			SAXReader reader  = new SAXReader();

			// Protect against external entity expansion
			reader.setIncludeExternalDTDDeclarations(false);

			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));

			xmlDoc = reader.read(rd);

			connection.disconnect();
			rd.close();

		} catch (final IOException ioe) {

			logger.warn("Connection to geocoding service failed", ioe);

			return null;

		} catch (final DocumentException de) {

			logger.warn("Could not read result document", de);

			return null;
		}

		Element root = xmlDoc.getRootElement();

		// List<Element> rootChildren = root.elements();
		String status = root.element("status").getTextTrim();

		if ("OK".equals(status)) {

			try {

				return new GoogleGeoCodingResult(address, root);

			} catch (final Throwable t) {

				logger.warn("Unable to find geocoding for address {}: {}", new Object[] { address, t.getMessage() });
			}

		} else if ("OVER_QUERY_LIMIT".equals(status)) {

			if (count < 3) {

				count++;

				logger.warn("Status OVER_QUERY_LIMIT for address {}, trying again after 2 seconds.", new Object[] { address });

				try {

					Thread.sleep(2000L);

				} catch (final InterruptedException ex) {}

				return geocode(street, house, postalCode, city, state, country, language);

			} else {

				logger.warn("Too many attempts with status OVER_QUERY_LIMIT for address {}, aborting.", new Object[] { address });
			}

		} else if ("REQUEST_DENIED".equals(status)) {

			final String errorMessage = root.element("error_message").getTextTrim();

			if (errorMessage != null) {

				logger.warn("Unable to gecode address {}. Error: {} - {}", address, status, errorMessage);

			} else {

				logger.warn("Unable to gecode address {}. Error: {}", address, status);

			}

		} else {

			logger.warn("Status not OK for address {}: {}", new Object[] { address, status });
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
		public AddressComponent getAddressComponent(Type type) {

			for(AddressComponent addressComponent : addressComponents) {

				if(addressComponent.getType() == type) {
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

		private Type type = null;
		private String value = null;

		public GoogleAddressComponent(Element addressComponent) {

			this.value = addressComponent.element("long_name").getTextTrim();

			Iterator<Element> typesElement = addressComponent.elementIterator("type");
			for(;typesElement.hasNext();) {

				Element typeElement = typesElement.next();
				String typeName = typeElement.getTextTrim();

				try {
					this.type = Type.valueOf(typeName);
					break;

				} catch(Throwable t) {

					logger.warn("Encountered unknown address component type {} while parsing.", typeName);
				}

			}
		}

		@Override
		public Type getType() {
			return type;
		}

		@Override
		public String getValue() {
			return value;
		}

	}

}
