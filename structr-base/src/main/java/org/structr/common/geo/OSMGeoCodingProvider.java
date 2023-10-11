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
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
			// Protect against external entity expansion
			reader.setIncludeExternalDTDDeclarations(false);
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
				try { data.put("road",		 root.element("place").element("road").getTextTrim()); } catch (Throwable t) {}
				try { data.put("house_number",	 root.element("place").element("house_number").getTextTrim()); } catch (Throwable t) {}

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

			String curData = null;

			curData = data.get("postalCode");
			if(curData != null){
				this.addressComponents.add(new OSMAddressComponent(curData, Type.postal_code));
			}
			curData = data.get("state");
			if(curData != null){
				this.addressComponents.add(new OSMAddressComponent(curData, Type.administrative_area_level_1));
			}
			curData = data.get("state_district");
			if(curData != null){
				this.addressComponents.add(new OSMAddressComponent(curData, Type.administrative_area_level_3));
			}
			curData = data.get("countryRegion");
			if(curData != null){
				this.addressComponents.add(new OSMAddressComponent(curData, Type.country));
			}
			curData = data.get("city");
			if(curData != null){
				this.addressComponents.add(new OSMAddressComponent(curData, Type.locality));
			}
			curData = data.get("road");
			if(curData != null){
				this.addressComponents.add(new OSMAddressComponent(curData, Type.route));
			}
			curData = data.get("house_number");
			if(curData != null){
				this.addressComponents.add(new OSMAddressComponent(curData, Type.street_number));
			}

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
			return new Double[]{latitude, longitude};
		}
	}

	private static class OSMAddressComponent implements AddressComponent {

		GeoCodingResult.Type type		 = null;
		String value					 = null;

		public OSMAddressComponent(String value, GeoCodingResult.Type type) {

			this.type = type;
			this.value  = value;
		}

		@Override
		public String getValue() {
			return value;
		}

		@Override
		public GeoCodingResult.Type getType() {
			return type;
		}
	}
}