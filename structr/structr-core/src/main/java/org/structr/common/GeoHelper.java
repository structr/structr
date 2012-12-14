/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
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

import org.structr.core.property.PropertyMap;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Location;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.GeoHelper.GeoCodingResult.Type;

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

	public static GeoCodingResult geocode(final String address) throws FrameworkException {

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
		if ("OK".equals(status)) {
			
			try {
				return new GeoCodingResult(address, root);
				
			} catch(Throwable t) {
				
				logger.log(Level.WARNING, "Unable to find geocoding for address {0}: {1}", new Object[] { address, t.getMessage() });
			}

		} else {

			logger.log(Level.WARNING, "Status not OK for address {0}: {1}", new Object[] { address, status });
		}

		return null;
	}
	
	public static void main(String[] args) {

		String address = "Hanauer Landstr. 291a";

		try {
			
			GeoCodingResult res = geocode(address);
			logger.log(Level.INFO, "result for address {0}: {1} ({2})", new Object[] {
				address,
				res.getLatitude(),
				res.getAddressComponent(Type.administrative_area_level_1).getLongValue()
			} );
			
			
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}
	}

//	public static List<AbstractNode> filterByDistance(final List<AbstractNode> nodes, final GeoCodingResult coords, final Double distance) throws FrameworkException {
//
//		List<AbstractNode> filteredList = new LinkedList<AbstractNode>();
//
//		Command graphDbCommand = Services.command(SecurityContext.getSuperUserInstance(), GraphDatabaseCommand.class);
//		GraphDatabaseService graphDb = (GraphDatabaseService) graphDbCommand.execute();
//
//		SpatialDatabaseService db = new SpatialDatabaseService(graphDb);
//		SimplePointLayer layer = (SimplePointLayer) db.createSimplePointLayer("temporaryLayer", "Longitude", "Latitude");
//
//		for (AbstractNode node : nodes) {
//			Double lat = node.getDoubleProperty(Location.Key.latitude);
//			Double lon = node.getDoubleProperty(Location.Key.longitude);
//
//			layer.add(lat, lon);
//		}
//
//		// TODO: finish implementation here??
//
//		return filteredList;
//
//	}

	//~--- inner classes --------------------------------------------------

	public static class GeoCodingResult {

		private List<AddressComponent> addressComponents = new LinkedList<AddressComponent>();
		private String address = null;
		private double latitude;
		private double longitude;

		public enum Type {
			
			street_number,
			route,
			sublocality,
			locality,
			
			/** Bundesland */
			administrative_area_level_1,
			
			/** Regierungsbezirk */
			administrative_area_level_2,
			
			/** Stadt */
			administrative_area_level_3,
			
			postal_code,
			country,
			political
		}
		
		//~--- constructors -------------------------------------------

		public GeoCodingResult(String address, Element root) {
			
			this.address = address;
			
			String latString  = root.element("result").element("geometry").element("location").element("lat").getTextTrim();
			String lonString  = root.element("result").element("geometry").element("location").element("lng").getTextTrim();

			Iterator<Element> addressComponentsElement = root.element("result").elementIterator("address_component");
			for(;addressComponentsElement.hasNext();) {

				addressComponents.add(new AddressComponent(addressComponentsElement.next()));
			}
			
			this.latitude     = Double.parseDouble(latString);
			this.longitude    = Double.parseDouble(lonString);
		}
		
		public GeoCodingResult(final double latitude, final double longitude) {

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

		public Double[] toArray() {
			return new Double[]{ latitude, longitude };
		}

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public AddressComponent getAddressComponent(Type... types) {
			
			for(AddressComponent addressComponent : addressComponents) {
				
				if(addressComponent.getTypes().containsAll(Arrays.asList(types))) {
					return addressComponent;
				}
			}
			
			return null;
		}
		
		public List<AddressComponent> getAddressComponents() {
			return addressComponents;
		}
	}
	
	public static class AddressComponent {
		
		private Set<Type> types = new LinkedHashSet<Type>();
		private String shortValue = null;
		private String longValue = null;
		
		public AddressComponent(Element addressComponent) {
			
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

		public Set<Type> getTypes() {
			return types;
		}

		public String getShortValue() {
			return shortValue;
		}

		public String getLongValue() {
			return longValue;
		}
	}
}
