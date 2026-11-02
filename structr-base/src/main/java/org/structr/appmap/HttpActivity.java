/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.appmap;

import org.structr.core.Services;
import org.structr.rest.common.Stats;
import org.structr.rest.service.HttpService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * How much the entry points of an application are used and how fast they answer, from the
 * request statistics of the HTTP service: a page by the name it is rendered under, a REST
 * endpoint by the resource signature of the request, which is the signature of its grant;
 * a request to a type that no endpoint stands for counts for the type. The statistics only
 * know entry points: what a method or page calls on its way is not measured and gets nothing.
 */
public class HttpActivity {

	public static void apply(final ApplicationMap map) {

		final HttpService http = Services.getInstance().getServiceImplementation(HttpService.class);

		if (http == null) {
			return;
		}

		applyToPages(map, http.getRequestStats("html"));
		applyToEndpoints(map, http.getRequestStats("json"));
	}

	// ----- private methods -----
	private static void applyToPages(final ApplicationMap map, final Map<String, Stats> stats) {

		// a name that several pages share cannot be told apart, so it counts for none of them
		final Map<String, ApplicationMap.MapNode> pages = new LinkedHashMap<>();
		final Map<String, Long> names                   = map.getNodes().stream().filter(n -> ApplicationMap.PAGE.equals(n.getKind())).collect(Collectors.groupingBy(ApplicationMap.MapNode::getName, Collectors.counting()));

		for (final ApplicationMap.MapNode node : map.getNodes()) {

			if (ApplicationMap.PAGE.equals(node.getKind()) && names.get(node.getName()) == 1) {
				pages.put(node.getName(), node);
			}
		}

		for (final Map.Entry<String, Stats> entry : stats.entrySet()) {

			final ApplicationMap.MapNode page = pages.get(entry.getKey());

			if (page != null) {
				add(page, entry.getValue());
			}
		}
	}

	private static void applyToEndpoints(final ApplicationMap map, final Map<String, Stats> stats) {

		final SchemaIndex schema = map.getSchema();

		for (final Map.Entry<String, Stats> entry : stats.entrySet()) {

			final ApplicationMap.MapNode node = endpointOf(map, schema, entry.getKey());

			if (node != null) {
				add(node, entry.getValue());
			}
		}
	}

	/**
	 * The map element a REST request went to: the endpoint of its signature, also without the view the signature ends with
	 * (Project/_Ui) and without the placeholder of an object id (Project/_id/archive); otherwise the type it names.
	 */
	private static ApplicationMap.MapNode endpointOf(final ApplicationMap map, final SchemaIndex schema, final String signature) {

		final String[] parts   = signature.split("/");
		final String withoutId = Arrays.stream(parts).filter(part -> !"_id".equals(part)).collect(Collectors.joining("/"));
		final String plain     = parts.length > 1 && parts[parts.length - 1].startsWith("_") ? signature.substring(0, signature.lastIndexOf('/')) : signature;

		for (final String candidate : new String[] { signature, plain, withoutId }) {

			final ApplicationMap.MapNode endpoint = map.getNode("api:" + candidate);

			if (endpoint != null) {
				return endpoint;
			}
		}

		final String type = schema.isType(parts[0]) ? schema.getTypeIds().get(parts[0]) : null;

		return type != null ? map.getNode(type) : null;
	}

	private static void add(final ApplicationMap.MapNode node, final Stats stats) {

		if (stats.getCount() > 0) {
			node.addActivity(stats.getCount(), stats.getAverageValue(), stats.getMaxValue());
		}
	}
}
