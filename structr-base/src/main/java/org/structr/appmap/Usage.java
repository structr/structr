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

import org.structr.common.event.RuntimeEventLog;
import org.structr.core.Services;
import org.structr.rest.common.Stats;
import org.structr.rest.service.HttpService;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * How much the entry points of an application are used since the start: a page by the
 * renderings the HTTP service counts under its name, a REST endpoint by the requests of every
 * method the event log counts under the resource signature of the request, which is the
 * signature of its grant, a request to a type by the endpoint its grants make (a request to a
 * type without grants, which only an administrator may send, counts for the type), and a
 * method CronService runs by its runs. Only entry points are counted: what a method or page
 * calls on its way gets nothing.
 */
public class Usage {

	public static void apply(final ApplicationMap map) {

		final HttpService http = Services.getInstance().getServiceImplementation(HttpService.class);

		if (http != null) {
			applyToPages(map, http.getRequestStats("html"));
		}

		applyToEndpoints(map, RuntimeEventLog.getRestCounts());
		applyToSchedules(map, RuntimeEventLog.getCronCounts());
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
				page.addUse(entry.getValue().getCount());
			}
		}
	}

	private static void applyToEndpoints(final ApplicationMap map, final Map<String, Long> requests) {

		final SchemaIndex schema = map.getSchema();

		for (final Map.Entry<String, Long> entry : requests.entrySet()) {

			final ApplicationMap.MapNode node = endpointOf(map, schema, entry.getKey());

			if (node != null) {
				node.addUse(entry.getValue());
			}
		}
	}

	// a task is the name of a user-defined function or Type.method
	private static void applyToSchedules(final ApplicationMap map, final Map<String, Long> runs) {

		final SchemaIndex schema = map.getSchema();

		for (final Map.Entry<String, Long> entry : runs.entrySet()) {

			final String[] parts = entry.getKey().split("\\.");
			final String method  = parts.length == 1 ? schema.getGlobalMethod(parts[0]) : (parts.length == 2 ? schema.getTypeMethod(parts[0], parts[1]) : null);
			final ApplicationMap.MapNode node = method != null ? map.getNode(method) : null;

			if (node != null) {
				node.addUse(entry.getValue());
			}
		}
	}

	/**
	 * The map element a REST request went to: the endpoint of its signature, also without the view the signature ends with
	 * (Project/_Ui) and without the placeholder of an object id (Project/_id/archive); otherwise the endpoint of the
	 * type it names (Project/_id/tasks), or else the type.
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

		if (!schema.isType(parts[0])) {
			return null;
		}

		final ApplicationMap.MapNode endpoint = map.getNode("api:" + parts[0]);

		return endpoint != null ? endpoint : map.getNode(schema.getTypeIds().get(parts[0]));
	}
}
