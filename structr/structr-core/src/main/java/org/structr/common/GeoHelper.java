/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Location;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Helper class to create location nodes from coordinates or by using
 * online geocoding service
 *
 * @author Axel Morgner
 */
public class GeoHelper {

	private static final Logger logger = Logger.getLogger(GeoHelper.class.getName());

	//~--- methods --------------------------------------------------------

	public static Location createLocation(final Coordinates coords) throws FrameworkException {

		double latitude                 = coords.getLatitude();
		double longitude                = coords.getLongitude();
		String type                     = Location.class.getSimpleName();
		final Map<String, Object> props = new HashMap<String, Object>();

		props.put(AbstractNode.Key.type.name(), type);
		props.put(Location.Key.latitude.name(), latitude);
		props.put(Location.Key.longitude.name(), longitude);

		StructrTransaction transaction = new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {
				return Services.command(SecurityContext.getSuperUserInstance(), CreateNodeCommand.class).execute(props);
			}
		};

		return (Location) Services.command(SecurityContext.getSuperUserInstance(), TransactionCommand.class).execute(transaction);
	}

	public static Coordinates geocode(final String address) throws FrameworkException {

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
			URL mapsUrl                  = new URL("http://maps.google.com/maps/api/geocode/" + protocol + "?sensor=false&address=" + encodedAddress);
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
		boolean ok    = "OK".equals(status);

		if (!ok) {

			logger.log(Level.WARNING, "Status not OK for address {0}: {1}", new Object[] { address, status });

			return null;

		} else {

			String latitude  = root.element("result").element("geometry").element("location").element("lat").getTextTrim();
			String longitude = root.element("result").element("geometry").element("location").element("lng").getTextTrim();
			double lat       = Double.parseDouble(latitude);
			double lon       = Double.parseDouble(longitude);

			logger.log(Level.INFO, "Coordinates found for address {0}: lat= {1}, lon={2}", new Object[] { address, lat, lon });

			return new Coordinates(lat, lon);

		}
	}

	public static void main(String[] args) {

		String address = "Hanauer Landstr. 291a";

		try {
			geocode(address);
		} catch (Exception ex) {
			Logger.getLogger(GeoHelper.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	//~--- inner classes --------------------------------------------------

	public static class Coordinates {

		private double latitude;
		private double longitude;

		//~--- constructors -------------------------------------------

		public Coordinates(final double latitude, final double longitude) {

			this.latitude  = latitude;
			this.longitude = longitude;
		}

		//~--- get methods --------------------------------------------

		/**
		 * @return the latitude
		 */
		public double getLatitude() {
			return latitude;
		}

		/**
		 * @return the longitude
		 */
		public double getLongitude() {
			return longitude;
		}

		//~--- set methods --------------------------------------------

		/**
		 * @param latitude the latitude to set
		 */
		public void setLatitude(double latitude) {
			this.latitude = latitude;
		}

		/**
		 * @param longitude the longitude to set
		 */
		public void setLongitude(double longitude) {
			this.longitude = longitude;
		}
	}
}
