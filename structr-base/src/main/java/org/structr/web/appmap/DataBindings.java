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
package org.structr.web.appmap;

import org.apache.commons.lang3.StringUtils;
import org.structr.appmap.ApplicationMap;
import org.structr.appmap.SchemaIndex;
import org.structr.core.traits.StructrTraits;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The repeaters and event action mappings of the pages and shared components, with the type
 * each of them works on, resolved the way Structr resolves it when it renders the page.
 *
 * A repeater shows the type its query names (find('T'), const type = 'T', a method of T,
 * a global method by the types it deals with, MATCH (n:T)); failing that, a nested key like
 * task.subtasks is followed through the relationship names of its parent keys; failing that,
 * the properties read on the key below the repeater pick the type that has them. Data keys
 * are scoped as in rendering: an element sees the keys of the repeaters above it, me (the
 * user) and current (the object of a detail page).
 *
 * An action mapping acts on its data type, else on the type of the key in its id expression,
 * else on the owner of its method, else on the type of a key in its parameter mappings, else
 * on the type that has the properties its user input names. A success URL that navigates
 * leads to a page.
 */
class DataBindings {

	private static final Pattern FIND            = Pattern.compile("(?:\\$\\.)?(?:find|search|find_privileged|findPrivileged|getOrCreate|get_or_create|create|search_fulltext)\\s*\\(\\s*['\"](\\w+)['\"]");
	private static final Pattern CONST_TYPE      = Pattern.compile("(?:type|typeName)\\s*[:=]\\s*['\"](\\w+)['\"]");
	private static final Pattern TYPE_METHOD     = Pattern.compile("\\$\\.(\\w+)\\.(\\w+)\\s*\\(");
	private static final Pattern GLOBAL_METHOD   = Pattern.compile("\\$\\.(\\w+)\\s*\\(");
	private static final Pattern CYPHER          = Pattern.compile("\\(\\s*\\w*\\s*:\\s*(\\w+)");
	private static final Pattern KEY_PROPERTY    = Pattern.compile("\\$\\{\\s*(\\w+)\\.(\\w+)");
	private static final Pattern KEY_PROPERTY_JS = Pattern.compile("\\$\\.(\\w+)\\.(\\w+)");
	private static final Pattern STATIC_LIST     = Pattern.compile("\\[\\s*\\{\\s*\\w+\\s*:");
	private static final Pattern PATH            = Pattern.compile("(?<![\\w.])(?:\\$\\.)?([A-Za-z_]\\w*)((?:\\.[A-Za-z_]\\w*)+)");
	private static final Pattern KEY_IN_EXPR     = Pattern.compile("\\$\\{\\s*(\\w+)(?:\\.id)?\\s*\\}");
	private static final Pattern LOCALIZE        = Pattern.compile("localize\\(\\s*['\"]([^'\"]+)['\"]");

	// a general property name like name or value proves nothing about the type
	private static final Set<String> GENERIC_PROPERTIES = Set.of("id", "name", "type", "value", "label", "title", "text", "description", "status", "lang", "language", "createdDate", "lastModifiedDate", "owner", "position", "key", "icon", "url", "link", "isActive", "active");

	private static final Map<String, String> VERBS = Map.of("create", "create", "update", "update", "delete", "delete", "method", "run");

	private final Map<String, String> currentTypes = new HashMap<>();
	private final Map<String, String> viewTypes    = new HashMap<>();

	// the input elements that action mappings send as user input: the property each one sets, as Type.property where
	// the type of the action mapping is known
	private final Map<String, String> inputTargets = new HashMap<>();
	private final List<HtmlNode> views             = new ArrayList<>();
	private final Function<String, String> pageOf;
	private final List<HtmlNode> all;
	private final ApplicationMap map;
	private final SchemaIndex schema;

	/**
	 * The HTML elements of all pages, each with the action mappings it triggers, and the page
	 * a URL leads to, as the pages resolve their links.
	 */
	DataBindings(final ApplicationMap map, final List<HtmlNode> all, final Function<String, String> pageOf) {

		this.map    = map;
		this.all    = all;
		this.pageOf = pageOf;
		this.schema = map.getSchema();
	}

	void contribute() {

		resolveCurrentTypes();
		resolveViews();

		for (final HtmlNode html : all) {

			for (final ActionMapping action : html.actions) {
				addAction(action, html);
			}
		}

		describeViews();
	}

	// ----- private methods -----
	/**
	 * The object of a detail page (/page/uuid): the data type of an action mapping on current,
	 * else the type whose properties the page reads on current. A page that renders any type
	 * through current.type has no single current type.
	 */
	private void resolveCurrentTypes() {

		final Map<String, Map<String, Integer>> readOnCurrent = new LinkedHashMap<>();

		for (final HtmlNode html : all) {

			for (final String[] read : keyProperties(html.text())) {

				if ("current".equals(read[0])) {
					readOnCurrent.computeIfAbsent(html.home, k -> new LinkedHashMap<>()).merge(read[1], 1, Integer::sum);
				}
			}
		}

		for (final HtmlNode html : all) {

			for (final ActionMapping action : html.actions) {

				final String idExpression = action.getIdExpression();

				if (idExpression != null && idExpression.contains("current") && StringUtils.isNotBlank(action.getDataType())) {
					readOnCurrent.computeIfAbsent(html.home, k -> new LinkedHashMap<>()).put("__type:" + action.getDataType(), 1);
				}
			}
		}

		readOnCurrent.forEach((home, read) -> {

			final Optional<String> declared = read.keySet().stream().filter(k -> k.startsWith("__type:")).findFirst();

			if (declared.isPresent()) {

				currentTypes.put(home, declared.get().substring("__type:".length()));

			} else if (read.getOrDefault("type", 0) < 2) {

				currentTypes.put(home, bestByProperties(read.keySet()));
			}
		});
	}

	/**
	 * Resolves the repeaters from the outside in, so that a nested key finds the type of its
	 * parent's key.
	 */
	private void resolveViews() {

		final List<HtmlNode> repeaters = new ArrayList<>();

		for (final HtmlNode html : all) {

			if (StringUtils.isNotEmpty(html.dataKey)) {
				repeaters.add(html);
			}
		}

		repeaters.sort(Comparator.comparingInt(html -> html.ancestors().size()));

		for (final HtmlNode html : repeaters) {

			final String script             = StringUtils.isNotEmpty(html.functionQuery) ? html.functionQuery : (html.cypherQuery != null ? html.cypherQuery : "");
			final Map<String, Double> found = typesIn(script);
			String type                     = null;
			String how                      = null;

			// a list written out in the script (options with name and value) shows no type
			if (found.isEmpty() && STATIC_LIST.matcher(script).find()) {
				continue;
			}

			if (!found.isEmpty()) {

				type = best(found);
				how  = "query";
			}

			// a nested key like task.subtasks or me.currentBasket.images follows the relationships of the parent keys
			if (type == null && !isComponentRoot(html)) {

				type = resolvePath(html.parent != null ? scopeOf(html.parent) : baseScope(html.home), script);
				how  = type != null ? "relationship" : null;
			}

			if (type == null) {

				type = bestByProperties(propertiesRead(html, html.dataKey).keySet());
				how  = type != null ? "properties" : null;
			}

			if (type != null) {

				viewTypes.put(html.id, type);

				// a repeater that is the root of a shared component must not replace the component
				final String id = map.hasNode(html.id) ? "view:" + html.id : html.id;

				map.addNode(id, ApplicationMap.VIEW, html.dataKey + ": " + type).setHome(html.home).setType(type, how);
				map.addEdge(html.home, id, "contains", 1.5, null);

				// the type a repeater shows takes part with its page, also when no script of the page names it
				map.connect(html.home, schema.getTypeIds().get(type));

				html.mapIds.add(id);
				views.add(html);
			}
		}
	}

	/**
	 * How each repeater uses its type, in the terms of the page: its data expression and what its
	 * copies are. The options of a select are a choice, and the select is mostly the input of an
	 * action mapping elsewhere, a button that saves the form; the choice sets the property that
	 * action mapping sends it as, else the property the select is named after.
	 */
	private void describeViews() {

		for (final HtmlNode html : views) {

			final Map<String, Object> use = new LinkedHashMap<>();
			final String script           = StringUtils.isNotEmpty(html.functionQuery) ? html.functionQuery : html.cypherQuery;
			final HtmlNode select         = choiceOf(html);

			if (StringUtils.isNotBlank(script)) {
				use.put("q", StringUtils.abbreviate(script.trim().replaceAll("\\s+", " "), 200));
			}

			if (select != null) {

				final String name  = select.attribute("_html_name");
				final String field = inputTargets.getOrDefault(select.id, StringUtils.isNotEmpty(name) && !name.contains("${") ? name : null);

				use.put("role", "choice");

				if (field != null) {
					use.put("f", field);
				}

			} else {

				use.put("role", "tr".equals(html.tag) ? "row" : "item");
			}

			for (final String id : html.mapIds) {
				map.getNode(id).setUse(use);
			}
		}
	}

	/**
	 * The select or datalist whose options a repeater makes, or null.
	 */
	private HtmlNode choiceOf(final HtmlNode html) {

		if (!"option".equals(html.tag) && !"optgroup".equals(html.tag)) {
			return null;
		}

		for (final HtmlNode ancestor : html.ancestors()) {

			if ("select".equals(ancestor.tag) || "datalist".equals(ancestor.tag)) {
				return ancestor;
			}
		}

		return null;
	}

	private void addAction(final ActionMapping action, final HtmlNode trigger) {

		final Map<String, String> scope = scopeOf(trigger);
		final String idExpression       = action.getIdExpression() != null ? action.getIdExpression() : "";
		final String methodName         = action.getMethod();
		String method                   = action.getMethodNode() != null ? action.getMethodNode().getUuid() : null;
		String type                     = null;
		String how                      = null;

		// an empty data type is how the editor leaves the field when nothing was chosen
		if (StringUtils.isNotBlank(action.getDataType())) {

			type = action.getDataType();
			how  = "dataType";

		} else {

			final Matcher matcher = KEY_IN_EXPR.matcher(idExpression);
			final String key      = matcher.find() ? matcher.group(1) : null;

			if (key != null && scope.containsKey(key)) {

				type = scope.get(key);
				how  = "scope:" + key;

			} else if (schema.isType(idExpression)) {

				type = idExpression;
				how  = "idExpression";

			} else if (key != null && schema.getTypeNamedLike(key) != null) {

				// a key that is not in scope here, mostly inside a shared component whose repeater lies in the page
				// around it, is nearly always named after its type
				type = schema.getTypeNamedLike(key);
				how  = "key name:" + key;
			}
		}

		if (method == null && StringUtils.isNotBlank(methodName) && type != null) {
			method = schema.getTypeMethod(type, methodName);
		}

		if (method == null && StringUtils.isNotBlank(methodName)) {
			method = schema.getGlobalMethod(methodName);
		}

		// a method called without a type to look it up on is the one of that name, if only one type has it
		if (method == null && StringUtils.isNotBlank(methodName)) {
			method = schema.getOnlyTypeMethod(methodName);
		}

		if (type == null && method != null && schema.getMethod(method) != null && schema.getMethod(method).owner != null) {

			type = schema.getMethod(method).owner;
			how  = "method owner";
		}

		// user input names properties; the type that has them supplies the type
		final Set<String> inputs = new LinkedHashSet<>();

		for (final ParameterMapping parameter : action.getParameterMappings()) {

			if ("user-input".equals(parameter.getParameterType()) && parameter.getParameterName() != null) {
				inputs.add(parameter.getParameterName());
			}

			final Matcher matcher = KEY_IN_EXPR.matcher(parameter.getScriptExpression() != null ? parameter.getScriptExpression() : "");

			while (matcher.find()) {

				if (type == null && scope.containsKey(matcher.group(1))) {

					type = scope.get(matcher.group(1));
					how  = "parameter:" + matcher.group(1);
				}
			}
		}

		if (type == null) {

			type = bestByProperties(inputs);
			how  = type != null ? "input names" : null;
		}

		final String verb     = action.getAction() != null ? VERBS.getOrDefault(action.getAction(), action.getAction()) : "no action";
		final String resolved = method != null && schema.getMethod(method) != null ? schema.getMethod(method).name : methodName;
		final String target   = "method".equals(action.getAction()) && resolved != null ? resolved + "()" : verb + " " + (type != null ? type : "object");
		final String where    = map.getNode(trigger.home) != null ? map.getNode(trigger.home).getName() : "";
		final String id       = action.getUuid();

		// the input elements it sends set the properties of its type; a choice among them names them (see describeViews)
		for (final ParameterMapping parameter : action.getParameterMappings()) {

			if ("user-input".equals(parameter.getParameterType()) && parameter.getParameterName() != null && parameter.getInputElement() != null) {
				inputTargets.putIfAbsent(parameter.getInputElement().getUuid(), type != null ? type + "." + parameter.getParameterName() : parameter.getParameterName());
			}
		}

		final Map<String, Object> use = new LinkedHashMap<>();

		use.put("el", labelOf(trigger));

		if (!inputs.isEmpty()) {
			use.put("in", new LinkedList<>(inputs));
		}

		map.addNode(id, ApplicationMap.ACTION, where + ": " + labelOf(trigger) + " → " + target).setHome(trigger.home).setType(type, how).setAction(action.getAction()).setEvent(action.getEvent()).setMethod(method).setUse(use);
		map.addEdge(trigger.home, id, "contains", 1.5, null);

		// as with repeaters, the type an action mapping works on takes part with its page
		map.connect(trigger.home, type != null ? schema.getTypeIds().get(type) : null);

		trigger.mapIds.add(id);

		final String url = action.getSuccessURL();

		if ("navigate-to-url".equals(action.getSuccessBehaviour()) && StringUtils.isNotEmpty(url)) {
			map.addEdge(id, pageOf.apply(url), "navigates", 1, null);
		}
	}

	private boolean isComponentRoot(final HtmlNode html) {

		return html.parent == null && html.id.equals(html.home);
	}

	/**
	 * The types a script names, with their weight: twice for a find(), a type name or a
	 * Cypher label, once for a method of the type and for each of the two types a global
	 * method it calls deals with most.
	 */
	private Map<String, Double> typesIn(final String script) {

		final Map<String, Double> found = new LinkedHashMap<>();

		for (final Pattern pattern : List.of(FIND, CONST_TYPE, CYPHER)) {

			final Matcher matcher = pattern.matcher(script);

			while (matcher.find()) {

				if (schema.isType(matcher.group(1))) {
					found.merge(matcher.group(1), 2.0, Double::sum);
				}
			}
		}

		Matcher matcher = TYPE_METHOD.matcher(script);

		while (matcher.find()) {

			if (schema.isType(matcher.group(1))) {
				found.merge(matcher.group(1), 1.0, Double::sum);
			}
		}

		matcher = GLOBAL_METHOD.matcher(script);

		while (matcher.find()) {

			final String method = schema.getGlobalMethod(matcher.group(1));

			if (method != null) {

				for (final String type : schema.getMethodTypes(method).stream().limit(2).toList()) {
					found.merge(type, 1.0, Double::sum);
				}
			}
		}

		return found;
	}

	/**
	 * The data keys visible at a node: those of the repeaters around it, the innermost one
	 * winning, plus me and current.
	 */
	private Map<String, String> scopeOf(final HtmlNode html) {

		final Map<String, String> scope = baseScope(html.home);
		final List<HtmlNode> chain      = new LinkedList<>(html.ancestors());

		Collections.reverse(chain);
		chain.add(html);

		for (final HtmlNode current : chain) {

			if (viewTypes.containsKey(current.id) && StringUtils.isNotEmpty(current.dataKey)) {
				scope.put(current.dataKey, viewTypes.get(current.id));
			}
		}

		return scope;
	}

	private Map<String, String> baseScope(final String home) {

		final Map<String, String> scope = new LinkedHashMap<>();

		scope.put("me", StructrTraits.USER);

		if (home != null && currentTypes.get(home) != null) {
			scope.put("current", currentTypes.get(home));
		}

		return scope;
	}

	/**
	 * The type a property path ends in, e.g. me.currentBasket.images, following relationship
	 * names hop by hop.
	 */
	private String resolvePath(final Map<String, String> scope, final String script) {

		final Matcher matcher = PATH.matcher(script);

		while (matcher.find()) {

			String type = scope.get(matcher.group(1));

			if (type == null) {
				continue;
			}

			for (final String name : StringUtils.strip(matcher.group(2), ".").split("\\.")) {

				type = relationshipTarget(type, name);

				if (type == null) {
					break;
				}
			}

			if (type != null) {
				return type;
			}
		}

		return null;
	}

	private String relationshipTarget(final String type, final String name) {

		final String target = schema.getRelationshipTarget(type, name);

		if (target != null) {
			return target;
		}

		for (final String parent : schema.getParents(type)) {

			final String inherited = schema.getRelationshipTarget(parent, name);

			if (inherited != null) {
				return inherited;
			}
		}

		return null;
	}

	/**
	 * The properties read on a key below a node: ${key.prop} and $.key.prop.
	 */
	private Map<String, Integer> propertiesRead(final HtmlNode html, final String key) {

		final Map<String, Integer> found = new LinkedHashMap<>();

		for (final HtmlNode current : html.subtree()) {

			for (final String[] read : keyProperties(current.text())) {

				if (key.equals(read[0])) {
					found.merge(read[1], 1, Integer::sum);
				}
			}
		}

		return found;
	}

	private List<String[]> keyProperties(final String text) {

		final List<String[]> result = new LinkedList<>();

		for (final Pattern pattern : List.of(KEY_PROPERTY, KEY_PROPERTY_JS)) {

			final Matcher matcher = pattern.matcher(text);

			while (matcher.find()) {
				result.add(new String[] { matcher.group(1), matcher.group(2) });
			}
		}

		return result;
	}

	/**
	 * The type that has the properties read, if at least half a vote points to it; a
	 * property that four or more types have counts for none.
	 */
	private String bestByProperties(final Collection<String> properties) {

		final Map<String, Double> score = new LinkedHashMap<>();

		for (final String property : properties) {

			final Set<String> candidates = schema.getTypesWithProperty(property);

			if (!GENERIC_PROPERTIES.contains(property) && !candidates.isEmpty() && candidates.size() <= 4) {

				for (final String type : candidates) {
					score.merge(type, 1.0 / candidates.size(), Double::sum);
				}
			}
		}

		final String best = best(score);

		return best != null && score.get(best) >= 0.5 ? best : null;
	}

	/**
	 * The key with the highest value; of several, the first.
	 */
	private String best(final Map<String, Double> values) {

		String best = null;

		for (final Map.Entry<String, Double> entry : values.entrySet()) {

			if (best == null || entry.getValue() > values.get(best)) {
				best = entry.getKey();
			}
		}

		return best;
	}

	/**
	 * What a user sees on a trigger element: its title, aria label or placeholder, else its
	 * static text, else its name.
	 */
	private String labelOf(final HtmlNode trigger) {

		for (final String key : List.of("_html_title", "_html_aria-label", "_html_placeholder")) {

			final String value = trigger.attribute(key);

			if (StringUtils.isNotEmpty(value) && !value.contains("${")) {
				return StringUtils.left(value, 40);
			}
		}

		final String text = visibleText(trigger);

		if (!text.isEmpty()) {
			return text;
		}

		if (trigger.name != null && !trigger.name.isEmpty() && !trigger.name.startsWith("New ") && !trigger.name.contains("${")) {
			return StringUtils.left(trigger.name, 40);
		}

		final String htmlName = trigger.attribute("_html_name");

		return StringUtils.isNotEmpty(htmlName) ? htmlName : (trigger.tag != null ? trigger.tag : "Element");
	}

	/**
	 * The static text nodes below an element without script expressions; a localized label
	 * names the element in the language of the code.
	 */
	private String visibleText(final HtmlNode html) {

		final List<String> parts = new LinkedList<>();

		for (final HtmlNode current : html.subtree()) {

			if (!StructrTraits.CONTENT.equals(current.type) || current.content == null) {
				continue;
			}

			final Matcher matcher = LOCALIZE.matcher(current.content);

			while (matcher.find()) {
				parts.add(matcher.group(1));
			}

			final String text = current.content.replaceAll("\\$\\{[^}]*\\}", "");

			if (text.matches("(?s).*[{};()].*")) {
				continue;
			}

			final String plain = StringUtils.normalizeSpace(text.replaceAll("<[^>]+>", " "));

			if (!plain.isEmpty()) {
				parts.add(plain);
			}
		}

		return StringUtils.left(String.join(" ", parts), 40);
	}
}
