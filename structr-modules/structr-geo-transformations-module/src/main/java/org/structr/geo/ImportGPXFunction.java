/*
 * Copyright (C) 2010-2026 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObjectMap;
import org.structr.core.function.XmlFunction;
import org.structr.core.property.*;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ImportGPXFunction extends GeoFunction {

	private static final Logger logger                                   = LoggerFactory.getLogger(ImportGPXFunction.class.getName());
	public static final Property<List<GraphObjectMap>> waypointsProperty = new GenericProperty<>("waypoints");
	public static final Property<List<GraphObjectMap>> routesProperty    = new GenericProperty<>("routes");
	public static final Property<List<GraphObjectMap>> tracksProperty    = new GenericProperty<>("tracks");
	public static final Property<List<GraphObjectMap>> segmentsProperty  = new GenericProperty<>("segments");
	public static final Property<List<GraphObjectMap>> pointsProperty    = new GenericProperty<>("points");
	public static final Property<GraphObjectMap> metadataProperty        = new GenericProperty<>("metadata");
	public static final Property<GraphObjectMap> authorProperty          = new GenericProperty<>("author");
	public static final Property<Double> latitudeProperty                = new DoubleProperty("latitude");
	public static final Property<Double> longitudeProperty               = new DoubleProperty("longitude");
	public static final String ERROR_MESSAGE                             = "";

	private static final Map<String, Property> fieldMapping = new LinkedHashMap<>();

	static {

		fieldMapping.put("ele",           new DoubleProperty("altitude"));
		fieldMapping.put("time",          new StringProperty("time"));
		fieldMapping.put("name",          new StringProperty("name"));
		fieldMapping.put("desc",          new StringProperty("description"));
		fieldMapping.put("link",          new StringProperty("link"));
		fieldMapping.put("src",           new StringProperty("source"));
		fieldMapping.put("magvar",        new DoubleProperty("magvar"));
		fieldMapping.put("cmt",           new StringProperty("comment"));
		fieldMapping.put("type",          new StringProperty("type"));
		fieldMapping.put("sym",           new StringProperty("symbol"));
		fieldMapping.put("fix",           new StringProperty("fixType"));
		fieldMapping.put("sat",           new IntProperty("satelliteCount"));
		fieldMapping.put("hdop",          new DoubleProperty("horizontalDilution"));
		fieldMapping.put("vdop",          new DoubleProperty("verticalDilution"));
		fieldMapping.put("pdop",          new DoubleProperty("positionDilution"));
		fieldMapping.put("ageofdgpsdata", new DoubleProperty("dgpsAge"));
		fieldMapping.put("dgpsid",        new StringProperty("dgpsId"));
		fieldMapping.put("geoidheight",   new DoubleProperty("geoidHeight"));

	}

	@Override
	public String getName() {
		return "importGpx";
	}

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("gpxString");
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasLengthAndAllElementsNotNull(sources, 1);

			if (sources[0] instanceof String) {

				final String source = (String)sources[0];
				if (source != null) {

					// parse source, create a list of points
					final GraphObjectMap result          = new GraphObjectMap();
					final XmlFunction xmlParser          = new XmlFunction();
					final Document doc                   = (Document)xmlParser.apply(ctx, caller, sources);
					final List<GraphObjectMap> waypoints = new LinkedList<>();
					final List<GraphObjectMap> routes    = new LinkedList<>();
					final List<GraphObjectMap> tracks    = new LinkedList<>();

					if (doc != null) {

						final Element root           = doc.getDocumentElement();
						final List<Element> children = getChildren(root);

						for (final Element child : children) {

							switch (child.getTagName()) {

								case "metadata":
									final GraphObjectMap metadata = readPoint(child);
									if (metadata != null) {

										result.put(metadataProperty, metadata);
									}
									break;

								case "rte":
									readRoute(child, routes);
									break;

								case "wpt":
									readWaypoint(child, waypoints);
									break;

								case "trk":
									readTrack(child, tracks);
									break;
							}
						}

						if (!waypoints.isEmpty()) {
							result.put(waypointsProperty, waypoints);
						}

						if (!routes.isEmpty()) {
							result.put(routesProperty, routes);
						}

						if (!tracks.isEmpty()) {
							result.put(tracksProperty, tracks);
						}
					}

					return result;
				}

			} else {

				logger.warn("Invalid parameter for GPX import, expected string, got {}", sources[0].getClass().getSimpleName() );
			}

			return "Invalid parameters";

		} catch (IllegalArgumentException e) {

			boolean isJs = ctx != null ? ctx.isJavaScriptContext() : false;
			logParameterError(caller, sources, e.getMessage(), isJs);
			return usage(isJs);
		}
	}

	@Override
	public String getShortDescription() {
		return "Parses a given GPX string and returns its contents as an object with.";
	}

	@Override
	public String getLongDescription() {
		return """
		The object returned by this function has the following format. Please note that there can be additional keys in the object such as "tracks" and "segments".
		
		```
		{
			"waypoints": [
				{
					"latitude": 42.438878,
					"longitude": -71.119277,
					"name": "5066"
				},
				...
			],
			"routes": [
				{
					"name": "BELLEVUE",
					"description": "Bike Loop Bellevue",
					"points": [
						{
							"latitude": 42.43095,
							"longitude": -71.107628,
							"altitude": 23.4696,
							"time": "2001-06-02T00:18:15Z",
							"name": "BELLEVUE",
							"comment": "BELLEVUE",
							"description": "Bellevue Parking Lot",
							"symbol": "Parking Area",
							"type": "Parking"
						},
						...
					]
				}
			]
		}
		```
		""";
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("${importGpx(getContent(first(find('File', 'name', 'track.gpx')), 'utf-8')}"),
			Usage.structrScript("${importGpx('<?xml version=\"1.0\"?><gpx version=\"1.0\"><wpt lat=\"42.438878\" lon=\"-71.119277\"><name>5066</name></wpt></gpx>')}"),
			Usage.javaScript("${{ $.importGpx($.getContent($.find('File', { name: 'track.gpx' })[0], 'utf-8'); }}"),
			Usage.javaScript("${{ $.importGpx('<?xml version=\"1.0\"?><gpx version=\"1.0\"><wpt lat=\"42.438878\" lon=\"-71.119277\"><name>5066</name></wpt></gpx>'); }}")
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("source", "GPX source to parse")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.javaScript("""
			${{
				let file      = $.find('File', { name: 'track1.gpx' })[0];
				let gpxString = $.getContent(file, 'utf-8');
				let gpxData   = $.importGpx(gpxString);

				$.print(Object.keys(gpxData));
			}}
			""", "Parse a GPX track from a file in the Structr filesystem")
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.InputOutput;
	}

	// ----- private methods -----
	private void readWaypoint(final Element waypoint, final List<GraphObjectMap> resultList) {

		final GraphObjectMap item = readPoint(waypoint);
		if (item != null) {

			resultList.add(item);
		}
	}

	private void readRoute(final Element route, final List<GraphObjectMap> resultList) {

		final List<GraphObjectMap> points = new LinkedList<>();
		final GraphObjectMap result       = new GraphObjectMap();

		for (final Element elem : getChildren(route)) {

			switch (elem.getTagName()) {

				case "rtept":
					final GraphObjectMap item = readPoint(elem);
					if (item != null) {

						points.add(item);
					}
					break;
			}

			readProperties(elem, result);
		}

		if (!points.isEmpty()) {
			result.put(pointsProperty, points);
		}

		resultList.add(result);
	}

	private void readTrack(final Element track, final List<GraphObjectMap> resultList) {

		final List<GraphObjectMap> segments = new LinkedList<>();
		final GraphObjectMap result         = new GraphObjectMap();

		for (final Element elem : getChildren(track)) {

			switch (elem.getTagName()) {

				case "trkseg":
					readTrackSegment(elem, segments);
					break;
			}

			readProperties(elem, result);
		}

		if (!segments.isEmpty()) {
			result.put(segmentsProperty, segments);
		}

		resultList.add(result);
	}

	private void readTrackSegment(final Element trackSegment, final List<GraphObjectMap> resultList) {

		final List<GraphObjectMap> points = new LinkedList<>();
		final GraphObjectMap result       = new GraphObjectMap();

		for (final Element elem : getChildren(trackSegment)) {

			switch (elem.getTagName()) {

				case "trkpt":
					final GraphObjectMap item = readPoint(elem);
					if (item != null) {

						points.add(item);
					}
					break;
			}

			readProperties(elem, result);
		}

		if (!points.isEmpty()) {
			result.put(pointsProperty, points);
		}

		resultList.add(result);
	}

	private GraphObjectMap readPoint(final Element point) {

		final GraphObjectMap item = new GraphObjectMap();

		// latitude and longitude are the only node attributes
		final Double latitude = getDoubleAttribute(point, "lat");
		if (latitude != null) {

			item.put(latitudeProperty, latitude);
		}

		// latitude and longitude are the only node attributes
		final Double longitude = getDoubleAttribute(point, "lon");
		if (longitude != null) {

			item.put(longitudeProperty, longitude);
		}

		// all other attributes are stored in child nodes
		for (final Element child : getChildren(point)) {

			readProperties(child, item);
		}

		return item;
	}

	private void readProperties(final Element child, final GraphObjectMap item) {

		final String tagName    = child.getTagName();
		final Property property = fieldMapping.get(tagName);

		if (property != null) {

			final Class valueType = property.valueType();
			if (valueType != null) {

				switch (valueType.getSimpleName()) {

					case "Double":
						storeDouble(child, item, property);
						break;

					case "String":
						storeString(child, item, property);
						break;

					case "Integer":
						storeInt(child, item, property);
						break;
				}
			}

		} else {

			if ("author".equals(tagName)) {

				final GraphObjectMap author = readPoint(child);
				if (!author.isEmpty()) {

					item.put(authorProperty, author);
				}
			}
		}
	}

	private List<Element> getChildren(final Element root) {

		final List<Element> elements = new LinkedList<>();
		final NodeList nodes         = root.getChildNodes();
		final int length             = nodes.getLength();

		for (int i=0; i<length; i++) {

			final Node node = nodes.item(i);
			if (node instanceof Element) {

				elements.add((Element)node);
			}
		}

		return elements;
	}

	private Double getDoubleAttribute(final Element source, final String name) {

		final String str = source.getAttribute(name);
		if (str != null && !str.isEmpty()) {

			return Double.valueOf(str);
		}

		return null;
	}

	private void storeDouble(final Element element, final GraphObjectMap result, final PropertyKey<Double> key) {

		final String source = element.getTextContent();
		if (source != null && !source.isEmpty()) {

			final Double value = Double.valueOf(source);
			if (value != null) {

				result.put(key, value);
			}
		}
	}

	private void storeInt(final Element element, final GraphObjectMap result, final PropertyKey<Integer> key) {

		final String source = element.getTextContent();
		if (source != null && !source.isEmpty()) {

			final Integer value = Integer.valueOf(source);
			if (value != null) {

				result.put(key, value);
			}
		}
	}

	private void storeString(final Element element, final GraphObjectMap result, final PropertyKey<String> key) {

		final String source = element.getTextContent();
		if (source != null && !source.isEmpty()) {

			result.put(key, source);
		}
	}
}
