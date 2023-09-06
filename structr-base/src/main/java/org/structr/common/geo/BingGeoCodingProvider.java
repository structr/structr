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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 *
 */
public class BingGeoCodingProvider extends AbstractGeoCodingProvider {

	private static final Logger logger = LoggerFactory.getLogger(BingGeoCodingProvider.class.getName());

	@Override
	public GeoCodingResult geocode(String street, String house, String postalCode, String city, String state, String country, String language) throws IOException {

		if (apiKey != null && !apiKey.isEmpty()) {

			StringBuilder urlBuffer = new StringBuilder("http://dev.virtualearth.net/REST/v1/Locations");

			// api key
			urlBuffer.append("?key=").append(apiKey);

			// culture for language-specific formatting
			urlBuffer.append("&c=").append(language);

			// output format: XML
			urlBuffer.append("&o=xml");

			// address line string
			urlBuffer.append("&query=");

			// house
			if (house != null && !house.isEmpty()) {
				urlBuffer.append(encodeURL(house)).append("+");
			}

			// street
			if (street != null && !street.isEmpty()) {
				urlBuffer.append(encodeURL(street)).append("+");
			}

			// city
			if (city != null && !city.isEmpty()) {
				urlBuffer.append(encodeURL(city)).append("+");
			}

			// postalCode
			if (postalCode != null && !postalCode.isEmpty()) {
				urlBuffer.append("&postalCode=").append(encodeURL(postalCode));
			}

			/* disabled because the ISO country code is required here which we don't have
			// countryRegion
			if (country != null && !country.isEmpty()) {
				urlBuffer.append("&countryRegion=").append(encodeURL(country));
			}
			*/

			// max results
			urlBuffer.append("&maxResults=1");

			String url = urlBuffer.toString();

			try {
				logger.info("Using url {}", url);

				URL mapsUrl                  = new URL(urlBuffer.toString());
				HttpURLConnection connection = (HttpURLConnection) mapsUrl.openConnection();

				connection.connect();

				Reader reader        = new InputStreamReader(connection.getInputStream());
				SAXReader saxReader  = new SAXReader();

				// skip leading 0xFEFF character if present
				if (reader.read() != 65279) {
					reader.reset();
				}

				// Protect against external entity expansion
				saxReader.setIncludeExternalDTDDeclarations(false);

				Document xmlDoc = saxReader.read(reader);

				connection.disconnect();
				reader.close();

				if (xmlDoc != null) {

					Map<String, String> data = new LinkedHashMap<>();
					Element root             = xmlDoc.getRootElement();

					try { data.put("lat",            root.element("ResourceSets").element("ResourceSet").element("Resources").element("Location").element("Point").element("Latitude").getTextTrim()); } catch (Throwable t) {}
					try { data.put("lon",            root.element("ResourceSets").element("ResourceSet").element("Resources").element("Location").element("Point").element("Longitude").getTextTrim()); } catch (Throwable t) {}
					try { data.put("postalCode",     root.element("ResourceSets").element("ResourceSet").element("Resources").element("Location").element("Address").element("PostalCode").getTextTrim()); } catch (Throwable t) {}
					try { data.put("adminDistrict",  root.element("ResourceSets").element("ResourceSet").element("Resources").element("Location").element("Address").element("AdminDistrict").getTextTrim()); } catch (Throwable t) {}
					try { data.put("adminDistrict2", root.element("ResourceSets").element("ResourceSet").element("Resources").element("Location").element("Address").element("AdminDistrict2").getTextTrim()); } catch (Throwable t) {}
					try { data.put("locality",       root.element("ResourceSets").element("ResourceSet").element("Resources").element("Location").element("Address").element("Locality").getTextTrim()); } catch (Throwable t) {}
					try { data.put("countryRegion",  root.element("ResourceSets").element("ResourceSet").element("Resources").element("Location").element("Address").element("CountryRegion").getTextTrim()); } catch (Throwable t) {}

					if (data.containsKey("lat") && data.containsKey("lon")) {

						String address =

							(StringUtils.isNotBlank(street) ? street : "") + " " +
							(StringUtils.isNotBlank(house) ? house : "") + " " +
							(StringUtils.isNotBlank(postalCode) ? postalCode : "" +
							(StringUtils.isNotBlank(city) ? city : "") + " " +
							(StringUtils.isNotBlank(state) ? state : "") + " " +
							(StringUtils.isNotBlank(country) ? country : "") + " "
						);

						return new BingGeoCodingResult(address, data);

					} else {

						logger.warn("Geocoding result did not contain location information:\n{}", xmlDoc.asXML());
					}
				}

			} catch (DocumentException dex) {

				logger.warn("Unable to use Bing geocoding provider: {}", dex.getMessage());

				// maybe not a permanent error => wrap in IOException so the request is retried later
				throw new IOException(dex);
			}

		} else {

			logger.warn("Unable to use Bing geocoding provider, missing API key. Please supply API key in structr.conf using the key geocoding.apikey.");
		}


		return null;
	}

	private static class BingGeoCodingResult implements GeoCodingResult {

		private List<AddressComponent> addressComponents = new LinkedList<>();
		private Double latitude                          = null;
		private Double longitude                         = null;
		private String address                           = null;

		public BingGeoCodingResult(String address, Map<String, String> data) {

			this.address = address;
			this.latitude = Double.parseDouble(data.get("lat"));
			this.longitude = Double.parseDouble(data.get("lon"));

			this.addressComponents.add(new BingAddressComponent(data.get("postalCode"), Type.postal_code));
			this.addressComponents.add(new BingAddressComponent(data.get("adminDistrict"), Type.administrative_area_level_1));
			this.addressComponents.add(new BingAddressComponent(data.get("adminDistrict2"), Type.administrative_area_level_3));
			this.addressComponents.add(new BingAddressComponent(data.get("countryRegion"), Type.country));
			this.addressComponents.add(new BingAddressComponent(data.get("locality"), Type.locality));
		}

		@Override
		public String getAddress() {
			return address;
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

		@Override
		public double getLatitude() {
			return latitude;
		}

		@Override
		public double getLongitude() {
			return longitude;
		}

		@Override
		public void setAddress(String address) {
			this.address = address;
		}

		@Override
		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		@Override
		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}

		@Override
		public Double[] toArray() {
			return new Double[]{ latitude, longitude };
		}
	}

	private static class BingAddressComponent implements AddressComponent {

		Type type	  = null;
		String value  = null;

		public BingAddressComponent(String value, Type type) {

			this.type = type;
			this.value  = value;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public Type getType() {
			return type;
		}

	}
}
