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
package org.structr.common.geo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.LoggerFactory;

public class OSMGeoCodingProvider extends AbstractGeoCodingProvider{

	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSMGeoCodingProvider.class.getName());

	@Override
	public GeoCodingResult geocode(String street, String house, String postalCode, String city, String state, String country, String language) throws IOException {

		String address =

                (StringUtils.isNotBlank(house) ? house : "") + " " +
                (StringUtils.isNotBlank(street) ? street : "") + " " +
                (StringUtils.isNotBlank(postalCode) ? postalCode : " " +
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
			String protocol = "xml";
			URL mapsUrl = new URL("http://nominatim.openstreetmap.org/search?q=" + encodedAddress + "&format=" + protocol + "&accept-language=" + language + "&addressdetails=1&limit=1");
			HttpURLConnection connection = (HttpURLConnection) mapsUrl.openConnection();

			SAXReader reader = new SAXReader();
			BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			xmlDoc = reader.read(rd);

			connection.disconnect();
			rd.close();

			if (xmlDoc != null) {

				Map<String, String> data = new LinkedHashMap<>();
				Element root = xmlDoc.getRootElement();

				try { data.put("lat",            root.element("place").attributeValue("lat")); } catch (Throwable t) {}
				try { data.put("lon",            root.element("place").attributeValue("lon")); } catch (Throwable t) {}
				try { data.put("postalCode",     root.element("place").element("postcode").getTextTrim()); } catch (Throwable t) {}
				try { data.put("state",          root.element("place").element("state").getTextTrim()); } catch (Throwable t) {}
				try { data.put("state_district", root.element("place").element("state_district").getTextTrim()); } catch (Throwable t) {}
				try { data.put("city",           root.element("place").element("city").getTextTrim()); } catch (Throwable t) {}
				try { data.put("countryRegion",  root.element("place").element("country").getTextTrim()); } catch (Throwable t) {}

				if (data.containsKey("lat") && data.containsKey("lon")) {

					return new OSMGeoCodingProvider.OSMGeoCodingResult(address, data);

				} else {

					logger.warn("Geocoding result did not contain location information:\n{}", xmlDoc.asXML());
				}
			}

		} catch (IOException ioe) {

			logger.warn("Connection to geocoding service failed", ioe);

			return null;

		} catch (DocumentException de) {

			logger.warn("Could not read result document", de);

			return null;
		}

		return null;
	}


	private static class OSMGeoCodingResult implements GeoCodingResult {

		private List <AddressComponent> addressComponents = new LinkedList<>();
		private String address = null;
		private double latitude;
		private double longitude;

		public OSMGeoCodingResult(String address, Map<String, String> data) {

			this.address = address;
			this.latitude = Double.parseDouble(data.get("lat"));
			this.longitude = Double.parseDouble(data.get("lon"));

			this.addressComponents.add(new OSMAddressComponent(data.get("postalCode"), Type.postal_code));
			this.addressComponents.add(new OSMAddressComponent(data.get("state"), Type.administrative_area_level_1));
			this.addressComponents.add(new OSMAddressComponent(data.get("state_district"), Type.administrative_area_level_3));
			this.addressComponents.add(new OSMAddressComponent(data.get("countryRegion"), Type.country));
			this.addressComponents.add(new OSMAddressComponent(data.get("city"), Type.locality));
		}

		public OSMGeoCodingResult(final double latitude, final double longitude) {

			this.latitude = latitude;
			this.longitude = longitude;
		}


		@Override
		public String getAddress() {
			return address;
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
			return new Double[]{latitude, longitude};
		}
	}

	private static class OSMAddressComponent implements AddressComponent {

		Set<GeoCodingResult.Type> types   = new LinkedHashSet<GeoCodingResult.Type>();
		String longValue  = null;
		String shortValue = null;

		public OSMAddressComponent(String value, GeoCodingResult.Type... types) {

			this.types.addAll(Arrays.asList(types));
			this.longValue  = value;
			this.shortValue = value;
		}

		@Override
		public String getLongValue() {
			return longValue;
		}

		@Override
		public String getShortValue() {
			return shortValue;
		}

		@Override
		public Set<GeoCodingResult.Type> getTypes() {
			return types;
		}
	}
}