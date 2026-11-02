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

import org.apache.commons.lang3.StringUtils;
import org.structr.appmap.ApplicationMap.MapEdge;
import org.structr.appmap.ApplicationMap.MapNode;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The CRUD matrix of the application: what every operation (method, repeater, event action
 * mapping) creates, reads, updates and deletes, per type.
 *
 * Method scripts are read with variable tracking: $.create and $.getOrCreate create, $.find
 * reads, assignments and $.set on a tracked variable or on $.this update, $.delete deletes,
 * and relationship names lead from one type to the next. A lifecycle method updates its
 * type. Repeaters read the type they show, action mappings do what their action says to
 * their type. An operation also does what the methods it calls do, weaker with every step;
 * helper methods (that write nothing and are called from at least four places) neither pass
 * on nor take part.
 *
 * Every access becomes a business edge from the operation to the type (creates, reads,
 * updates, deletes), every call an edge to the called method (calls, invokes for an action
 * mapping, helper for a helper method). The accesses including those passed on along calls
 * are the crud entries of a node.
 */
class CrudAnalysis {

	private static final Map<String, Double> WEIGHT = Map.of("C", 3.0, "U", 2.5, "D", 2.5, "R", 1.0);
	private static final Map<String, String> KIND   = Map.of("C", "creates", "R", "reads", "U", "updates", "D", "deletes");
	private static final Map<String, String> VERB   = Map.of("create", "C", "update", "U", "delete", "D");
	private static final Set<String> CREATING       = Set.of("create", "getOrCreate", "get_or_create");
	private static final Set<String> OPERATIONS     = Set.of(ApplicationMap.METHOD, ApplicationMap.ACTION, ApplicationMap.VIEW);

	private static final String FINDER         = "\\$\\.(find|findPrivileged|find_privileged|search|searchFulltext|create|getOrCreate|get_or_create|getOrNull|get_or_null)\\(\\s*['\"](\\w+)['\"]";
	private static final Pattern FINDER_ANY    = Pattern.compile(FINDER);
	private static final Pattern ASSIGN_FINDER = Pattern.compile("(?:const|let|var)\\s+(\\w+)\\s*=\\s*(?:await\\s+)?(?:\\$\\.first\\(\\s*)?" + FINDER);
	private static final Pattern LOOP_FINDER   = Pattern.compile("for\\s*\\(\\s*(?:const|let|var)\\s+(\\w+)\\s+of\\s+(?:\\$\\.first\\(\\s*)?" + FINDER);
	private static final Pattern ASSIGN_PATH   = Pattern.compile("(?:const|let|var)\\s+(\\w+)\\s*=\\s*(\\$\\.this|\\w+)\\.(\\w+)\\b(?!\\s*\\()");
	private static final Pattern LOOP_PATH     = Pattern.compile("for\\s*\\(\\s*(?:const|let|var)\\s+(\\w+)\\s+of\\s+(\\$\\.this|\\w+)\\.(\\w+)\\b");
	private static final Pattern LOOP_VAR      = Pattern.compile("for\\s*\\(\\s*(?:const|let|var)\\s+(\\w+)\\s+of\\s+(\\w+)\\s*\\)");
	private static final Pattern PROP_WRITE    = Pattern.compile("(\\$\\.this|\\b\\w+)\\.(\\w+)\\s*=(?!=)");
	private static final Pattern SET           = Pattern.compile("\\$\\.set\\(\\s*(\\$\\.this|\\w+)\\s*,");
	private static final Pattern DELETE        = Pattern.compile("\\$\\.delete\\(\\s*(\\$\\.this|\\w+)\\b");
	private static final Pattern THIS_READ     = Pattern.compile("\\$\\.this\\.\\w+");
	private static final Pattern CALL_TYPE     = Pattern.compile("\\$\\.(\\w+)\\.(\\w+)\\s*\\(");
	private static final Pattern CALL_GLOBAL   = Pattern.compile("\\$\\.(\\w+)\\s*\\(");

	private final Map<String, Map<String, Map<String, String>>> access = new LinkedHashMap<>();
	private final Map<String, Set<String>> calls                       = new LinkedHashMap<>();
	private final Map<String, Map<String, Double>> matrix              = new LinkedHashMap<>();
	private final Map<String, String> typeIds                          = new LinkedHashMap<>();
	private final Map<String, String> globalMethods                    = new LinkedHashMap<>();
	private final Map<String, String> typeMethods                      = new LinkedHashMap<>();
	private final Map<String, MapNode> nodes                           = new LinkedHashMap<>();
	private final Set<String> helpers                                  = new TreeSet<>();
	private final List<MapEdge> edges                                  = new LinkedList<>();
	private final ApplicationMap map;

	CrudAnalysis(final ApplicationMap map, final Set<String> kept) {

		this.map = map;

		for (final MapNode node : map.getNodes()) {

			if (kept.contains(node.getId())) {

				nodes.put(node.getId(), node);

				if (ApplicationMap.TYPE.equals(node.getKind())) {
					typeIds.put(node.getName(), node.getId());
				}
			}
		}

		for (final SchemaIndex.Method method : map.getSchema().getMethods()) {

			if (nodes.containsKey(method.id)) {

				if (method.owner == null) {
					globalMethods.put(method.name, method.id);
				} else {
					typeMethods.put(method.owner + "." + method.name, method.id);
				}
			}
		}
	}

	void run() {

		for (final SchemaIndex.Method method : map.getSchema().getMethods()) {

			if (nodes.containsKey(method.id)) {
				analyzeMethod(method);
			}
		}

		for (final MapNode node : nodes.values()) {

			if (ApplicationMap.VIEW.equals(node.getKind()) && node.getType() != null) {
				note(node.getId(), node.getType(), "R", "repeater " + node.getName());
			}

			if (ApplicationMap.ACTION.equals(node.getKind())) {

				final String action = node.getAction();

				// an action mapping whose action was never chosen has none; Map.of does not take a null key
				if (action != null && VERB.containsKey(action) && node.getType() != null) {
					note(node.getId(), node.getType(), VERB.get(action), "mapping " + action + " on " + node.getType());
				}

				if (node.getMethod() != null && nodes.containsKey(node.getMethod())) {

					call(node.getId(), node.getMethod());

				} else if (node.getType() != null && "method".equals(action)) {

					note(node.getId(), node.getType(), "U", "mapping calls a method on " + node.getType());
				}
			}
		}

		findHelpers();
		propagate();
		collectEdges();
	}

	Set<String> crudOf(final String id) {

		final Map<String, Double> row = matrix.get(id);

		if (row == null) {
			return null;
		}

		final Set<String> crud = new TreeSet<>();

		row.forEach((key, value) -> {

			if (value >= 1) {

				final String[] parts = key.split("\\|", 2);

				crud.add(parts[1] + ":" + parts[0]);
			}
		});

		return crud;
	}

	Set<String> getHelpers() {

		return helpers;
	}

	List<MapEdge> getEdges() {

		return edges;
	}

	// ----- private methods -----
	private void analyzeMethod(final SchemaIndex.Method method) {

		final SchemaIndex schema       = map.getSchema();
		final String source            = method.source;
		final String id                = method.id;
		final String name              = nodes.get(id).getName();
		final Map<String, String> vars = new HashMap<>();

		if (method.owner != null) {

			vars.put("$.this", method.owner);

			if (method.isLifecycle) {
				note(id, method.owner, "U", method.name + " is a lifecycle method of " + method.owner);
			}
		}

		Matcher matcher = FINDER_ANY.matcher(source);

		while (matcher.find()) {
			note(id, matcher.group(2), CREATING.contains(matcher.group(1)) ? "C" : "R", lineOf(name, source, matcher.start()));
		}

		for (final Pattern pattern : List.of(ASSIGN_FINDER, LOOP_FINDER)) {

			matcher = pattern.matcher(source);

			while (matcher.find()) {
				vars.put(matcher.group(1), matcher.group(3));
			}
		}

		// relationship paths, twice so that chains resolve
		for (int pass = 0; pass < 2; pass++) {

			for (final Pattern pattern : List.of(ASSIGN_PATH, LOOP_PATH)) {

				matcher = pattern.matcher(source);

				while (matcher.find()) {

					final String type   = vars.get(matcher.group(2));
					final String target = type != null ? schema.getRelationshipTarget(type, matcher.group(3)) : null;

					if (target != null) {

						vars.put(matcher.group(1), target);
						note(id, target, "R", lineOf(name, source, matcher.start()));
					}
				}
			}

			matcher = LOOP_VAR.matcher(source);

			while (matcher.find()) {

				if (vars.containsKey(matcher.group(2))) {
					vars.put(matcher.group(1), vars.get(matcher.group(2)));
				}
			}
		}

		matcher = PROP_WRITE.matcher(source);

		while (matcher.find()) {

			final String type = vars.get(matcher.group(1));

			if (type != null) {

				final String target = schema.getRelationshipTarget(type, matcher.group(2));

				note(id, type, "U", lineOf(name, source, matcher.start()));

				if (target != null) {
					note(id, target, "R", lineOf(name, source, matcher.start()));
				}
			}
		}

		matcher = SET.matcher(source);

		while (matcher.find()) {

			if (vars.containsKey(matcher.group(1))) {
				note(id, vars.get(matcher.group(1)), "U", lineOf(name, source, matcher.start()));
			}
		}

		matcher = DELETE.matcher(source);

		while (matcher.find()) {

			if (vars.containsKey(matcher.group(1))) {
				note(id, vars.get(matcher.group(1)), "D", lineOf(name, source, matcher.start()));
			}
		}

		if (method.owner != null && THIS_READ.matcher(source).find()) {
			note(id, method.owner, "R", name + ": reads $.this");
		}

		matcher = CALL_TYPE.matcher(source);

		while (matcher.find()) {

			final String callee = typeMethods.get(matcher.group(1) + "." + matcher.group(2));

			if (callee != null && !callee.equals(id)) {
				call(id, callee);
			}
		}

		matcher = CALL_GLOBAL.matcher(source);

		while (matcher.find()) {

			final String callee = globalMethods.get(matcher.group(1));

			if (callee != null && !callee.equals(id)) {
				call(id, callee);
			}
		}
	}

	/**
	 * The evidence of what a method does: its name, and the line of its code where it does it,
	 * with its number, so that a reader finds it.
	 */
	private static String lineOf(final String name, final String source, final int index) {

		final int start   = source.lastIndexOf('\n', index - 1) + 1;
		final int end     = source.indexOf('\n', index);
		final int number  = (int) source.substring(0, start).chars().filter(c -> c == '\n').count() + 1;
		final String line = source.substring(start, end < 0 ? source.length() : end).trim();

		return name + ", line " + number + ": " + StringUtils.abbreviate(line, 160);
	}

	/**
	 * Records the first evidence of an operation on a type; a type that takes no part in the map
	 * is ignored.
	 */
	private void note(final String operation, final String type, final String op, final String evidence) {

		if (typeIds.containsKey(type)) {
			access.computeIfAbsent(operation, k -> new LinkedHashMap<>()).computeIfAbsent(type, k -> new LinkedHashMap<>()).putIfAbsent(op, evidence);
		}
	}

	private void call(final String caller, final String callee) {

		calls.computeIfAbsent(caller, k -> new LinkedHashSet<>()).add(callee);
	}

	private boolean writes(final String operation) {

		for (final Map<String, String> ops : access.getOrDefault(operation, Map.of()).values()) {

			if (ops.containsKey("C") || ops.containsKey("U") || ops.containsKey("D")) {
				return true;
			}
		}

		return false;
	}

	private void findHelpers() {

		final Map<String, Set<String>> callers = new HashMap<>();

		calls.forEach((caller, callees) -> callees.forEach(callee -> callers.computeIfAbsent(callee, k -> new HashSet<>()).add(caller)));

		for (final SchemaIndex.Method method : map.getSchema().getMethods()) {

			if (nodes.containsKey(method.id) && callers.getOrDefault(method.id, Set.of()).size() >= 4 && !writes(method.id)) {
				helpers.add(method.id);
			}
		}
	}

	/**
	 * An operation does what it does itself, and what the methods it calls do, up to three
	 * calls deep and weaker by 0.7 with every step. Only what keeps a weight of at least 1
	 * counts (see crudOf): reading reaches no further than the operation itself, updating and
	 * deleting two calls deep, creating three.
	 */
	private void propagate() {

		access.forEach((operation, types) -> types.forEach((type, ops) -> ops.keySet().forEach(op -> raise(operation, type, op, WEIGHT.get(op)))));

		for (final MapNode node : nodes.values()) {

			if (!OPERATIONS.contains(node.getKind())) {
				continue;
			}

			final String operation = node.getId();
			final Set<String> seen = new HashSet<>(Set.of(operation));
			List<String> frontier  = List.of(operation);
			double factor          = 1.0;

			for (int step = 0; step < 3; step++) {

				final List<String> next = new LinkedList<>();

				factor *= 0.7;

				for (final String current : frontier) {

					for (final String callee : calls.getOrDefault(current, Set.of())) {

						if (seen.contains(callee) || helpers.contains(callee)) {
							continue;
						}

						final double weight = factor;

						seen.add(callee);
						next.add(callee);

						access.getOrDefault(callee, Map.of()).forEach((type, ops) -> ops.keySet().forEach(op -> raise(operation, type, op, weight * WEIGHT.get(op))));
					}
				}

				frontier = next;
			}
		}
	}

	private void raise(final String operation, final String type, final String op, final double value) {

		matrix.computeIfAbsent(operation, k -> new LinkedHashMap<>()).merge(type + "|" + op, value, Math::max);
	}

	private void collectEdges() {

		access.forEach((operation, types) -> types.forEach((type, ops) -> ops.forEach((op, evidence) -> {
			edges.add(new MapEdge(operation, typeIds.get(type), KIND.get(op), WEIGHT.get(op), evidence));
		})));

		calls.forEach((caller, callees) -> callees.forEach(callee -> {

			final String kind = helpers.contains(callee) ? "helper" : (ApplicationMap.ACTION.equals(nodes.get(caller).getKind()) ? "invokes" : "calls");

			edges.add(new MapEdge(caller, callee, kind, 1.5, nodes.get(caller).getName() + " calls " + nodes.get(callee).getName()));
		}));
	}
}
