package org.structr.autocomplete;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang.StringUtils;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.GraphObjectMap;
import org.structr.core.app.StructrApp;
import org.structr.core.parser.Functions;
import org.structr.core.property.GenericProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.SchemaHelper;
import org.structr.schema.action.Function;
import org.structr.schema.action.Hint;
import org.structr.web.entity.dom.DOMNode;



public abstract class AbstractHintProvider {

	private static final Set<String> startChars = new HashSet<>(Arrays.asList(new String[] { ".", ",", "(", "((", "(((", "((((", "(((((", "((((((", "${" } ));
	private static final Set<String> keywords   = new HashSet<>();

	static {

		keywords.add("current");
		keywords.add("request");
		keywords.add("this");
		keywords.add("element");
		keywords.add("page");
		keywords.add("link");
		keywords.add("template");
		keywords.add("parent");
		keywords.add("children");
		keywords.add("host");
		keywords.add("port");
		keywords.add("path_info");
		keywords.add("now");
		keywords.add("me");
		keywords.add("locale");
	}

	public static final Property<String> displayText = new StringProperty("displayText");
	public static final Property<String> text        = new StringProperty("text");
	public static final Property<GraphObject> from   = new GenericProperty("from");
	public static final Property<GraphObject> to     = new GenericProperty("to");
	public static final Property<Integer> line       = new IntProperty("line");
	public static final Property<Integer> ch         = new IntProperty("ch");

	/**
	 * Allowes the implementer to transform the given sourceName
	 * according to its own rules.
	 *
	 * @param sourceName
	 * @return the transformed sourceName
	 */
	protected abstract String getFunctionName(final String sourceName);
	protected abstract boolean isJavascript();

	private final Comparator comparator = new HintComparator();


	public List<GraphObject> getHints(final GraphObject currentEntity, final String type, final String currentToken, final String previousToken, final int cursorLine, final int cursorPosition) {

		final List<GraphObject> hints = new LinkedList<>();
		int maxNameLength             = 0;

		// System.out.println("currentToken: " + currentToken + ", previousToken: " + previousToken);

		if (StringUtils.isBlank(currentToken) || startChars.contains(currentToken)) {

			// display all possible hints
			for (final Hint hint : getAllHints(currentEntity, currentToken, previousToken)) {

				final GraphObjectMap item = new GraphObjectMap();
				String functionName       = getFunctionName(hint.getReplacement());

				if (hint.mayModify()) {

					item.put(text, visitReplacement(functionName));

				} else {

					item.put(text, functionName);
				}

				item.put(displayText, getFunctionName(hint.getName()) + " - " + textOrPlaceholder(hint.shortDescription()));
				addPosition(item, hint, cursorLine, cursorPosition, cursorPosition);

				if (functionName.length() > maxNameLength) {
					maxNameLength = functionName.length();
				}

				hints.add(item);
			}

		} else {

			final int currentTokenLength = currentToken.length();

			for (final Hint hint : getAllHints(currentEntity, currentToken, previousToken)) {

				final String functionName = getFunctionName(hint.getName());
				final String replacement  = hint.getReplacement();

				if (functionName.startsWith(currentToken)) {

					final GraphObjectMap item = new GraphObjectMap();

					if (hint.mayModify()) {

						item.put(text, visitReplacement(replacement));

					} else {

						item.put(text, replacement);
					}

					item.put(displayText, getFunctionName(hint.getName()) + " - " + textOrPlaceholder(hint.shortDescription()));

					addPosition(item, hint, cursorLine, cursorPosition - currentTokenLength, cursorPosition);

					if (functionName.length() > maxNameLength) {
						maxNameLength = functionName.length();
					}


					hints.add(item);
				}
			}
		}

		alignHintDescriptions(hints, maxNameLength);

		return hints;
	}

	protected String visitReplacement(final String replacement) {
		return replacement;
	}

	protected Hint createHint(final String name, final String signature, final String description) {
		return createHint(name, signature, description, null);
	}

	protected Hint createHint(final String name, final String signature, final String description, final String replacement) {

		final NonFunctionHint hint = new NonFunctionHint() {

			@Override
			public String shortDescription() {
				return description;
			}

			@Override
			public String getSignature() {
				return signature;
			}

			@Override
			public String getName() {
				return name;
			}
		};

		if (replacement != null) {
			hint.setReplacement(replacement);
		}

		return hint;
	}

	protected void alignHintDescriptions(final List<GraphObject> hints, final int maxNameLength) {

		// insert appropriate number of spaces into description to align function names
		for (final GraphObject item : hints) {

			final String text = item.getProperty(displayText);
			final int pos     = text.indexOf(" - ");

			if (pos < maxNameLength) {

				final StringBuilder buf = new StringBuilder(text);
				buf.insert(pos, StringUtils.leftPad("", maxNameLength - pos));

				// ignore exception, won't happen on a GraphObjectMap anyway
				try { item.setProperty(displayText, buf.toString()); } catch (FrameworkException fex) {}
			}
		}
	}

	protected String textOrPlaceholder(final String source) {

		if (StringUtils.isBlank(source)) {

			return "No description available yet.";
		}

		return source;
	}

	protected List<Hint> getAllHints(final GraphObject currentNode, final String currentToken, final String previousToken) {

		final Map<String, DataKey> dataKeys = new TreeMap<>();
		final List<Hint> hints              = new LinkedList<>();
		final List<Hint> local              = new LinkedList<>();

		// data key etc. hints
		if (currentNode != null) {
			recursivelyFindDataKeys(currentNode, dataKeys);
		}

		switch (previousToken) {

			case "current":
				local.add(createHint("id",    "", "ID of current entity"));
				break;

			case "this":
				local.add(createHint("id",    "", "ID of current node"));
				break;

			case "me":
				local.add(createHint("id",     "", "ID of current user"));
				local.add(createHint("name",   "", "Name of current user"));
				break;

			case "page":
				local.add(createHint("id",     "", "ID of current page"));
				local.add(createHint("name",   "", "Name of current page"));
				break;

			case "link":
				local.add(createHint("id",      "", "ID of current link"));
				local.add(createHint("name",    "", "Name of current link"));
				local.add(createHint("version", "", "Version of current link"));
				break;

			default:

				final ConfigurationProvider config = StructrApp.getConfiguration();
				final DataKey key                  = dataKeys.get(previousToken);

				if (key != null) {

					final Class type = key.identifyType(config);
					if (type != null) {

						final List<Hint> propertyHints = new LinkedList<>();

						// create hints based on schema type information
						for (final PropertyKey propertyKey : config.getPropertySet(type, PropertyView.All)) {

							final Hint propertyHint = createHint(propertyKey.jsonName(), "", type.getSimpleName() + " property");
							propertyHint.preventModification();

							propertyHints.add(propertyHint);
						}

						Collections.sort(propertyHints, comparator);
						hints.addAll(0, propertyHints);
					}
				}

				break;

		}

		if (!keywords.contains(previousToken) && !".".equals(currentToken) && !dataKeys.containsKey(previousToken)) {

			for (final Function<Object, Object> func : Functions.functions.values()) {
				hints.add(func);
			}

			Collections.sort(hints, comparator);

			// non-function hints
			local.add(createHint("current",   "", "Current data object",       !isJavascript() ? null : "get('current')"));
			local.add(createHint("request",   "", "Current request object",    !isJavascript() ? null : "get('request')"));
			local.add(createHint("this",      "", "Current object",            !isJavascript() ? null : "get('this')"));
			local.add(createHint("element",   "", "Current object",            !isJavascript() ? null : "get('element')"));
			local.add(createHint("page",      "", "Current page",              !isJavascript() ? null : "get('page')"));
			local.add(createHint("link",      "", "Current link",              !isJavascript() ? null : "get('link')"));
			local.add(createHint("template",  "", "Closest template node",     !isJavascript() ? null : "get('template')"));
			local.add(createHint("parent",    "", "Parent node",               !isJavascript() ? null : "get('parent')"));
			local.add(createHint("children",  "", "Collection of child nodes", !isJavascript() ? null : "get('children')"));
			local.add(createHint("host",      "", "Client's host name",        !isJavascript() ? null : "get('host')"));
			local.add(createHint("port",      "", "Client's port",             !isJavascript() ? null : "get('port')"));
			local.add(createHint("path_info", "", "URL path",                  !isJavascript() ? null : "get('path_info')"));
			local.add(createHint("now",       "", "Current date",              !isJavascript() ? null : "get('now')"));
			local.add(createHint("me",        "", "Current user",              !isJavascript() ? null : "get('me)"));
			local.add(createHint("locale",    "", "Current locale",            !isJavascript() ? null : "get('locale')"));
		}

		// add local hints to the beginning of the list
		Collections.sort(local, comparator);
		hints.addAll(0, local);

		// prepend data keys
		if (!dataKeys.containsKey(previousToken)) {

			for (final DataKey dataKey : dataKeys.values()) {

				hints.add(0, createHint(dataKey.getDataKey(), "", dataKey.getDescription(),  isJavascript() ? "get('" + dataKey.getDataKey() + "')" : null));
			}
		}

		return hints;
	}

	// ----- private methods -----
	private void addPosition(final GraphObjectMap item, final Hint hint, final int cursorLine, final int replaceFrom, final int replaceTo) {

		final GraphObjectMap fromObject = new GraphObjectMap();
		final GraphObjectMap toObject   = new GraphObjectMap();

		fromObject.put(line, cursorLine);
		fromObject.put(ch, replaceFrom);

		toObject.put(line, cursorLine);
		toObject.put(ch, replaceTo);

		item.put(from, fromObject);
		item.put(to, toObject);
	}

	private void recursivelyFindDataKeys(final GraphObject entity, final Map<String, DataKey> dataKeys) {

		if (entity != null) {

			final String dataKey = entity.getProperty(DOMNode.dataKey);
			if (dataKey != null) {

				final DataKey key = new DataKey(entity);
				dataKeys.put(key.getDataKey(), key);
			}

			recursivelyFindDataKeys(entity.getProperty(DOMNode.parent), dataKeys);
		}
	}

	// ----- nested classes -----
	protected static class HintComparator implements Comparator<Hint> {

		@Override
		public int compare(final Hint o1, final Hint o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	private static class DataKey implements Comparable<DataKey> {

		private String queryType = "REST";
		private String dataKey   = null;
		private String query     = null;

		public DataKey(final GraphObject entity) {

			dataKey = entity.getProperty(DOMNode.dataKey);
			query   = entity.getProperty(DOMNode.restQuery);

			if (query == null) {
				query = entity.getProperty(DOMNode.cypherQuery);
				queryType = "Cypher";
			}

			if (query == null) {
				query = entity.getProperty(DOMNode.xpathQuery);
				queryType = "XPath";
			}

			if (query == null) {
				query = entity.getProperty(DOMNode.functionQuery);
				queryType = "Function";
			}
		}

		public String getDataKey() {
			return dataKey;
		}

		public String getDescription() {

			final StringBuilder buf = new StringBuilder();

			buf.append("Data key for ");
			buf.append(queryType);
			buf.append(" query ");
			buf.append(StringUtils.abbreviate(query, 20));

			return buf.toString();
		}

		@Override
		public int compareTo(final DataKey other) {
			return dataKey.compareTo(other.getDataKey());
		}

		public Class identifyType(final ConfigurationProvider config) {

			// only for REST right now
			if ("REST".equals(queryType)) {

				// remove template expressions
				String cleanedQuery = query.replaceAll("\\$\\{.*\\}", "");

				// remove optional / for REST
				if (cleanedQuery.startsWith("/")) {
					cleanedQuery = cleanedQuery.substring(1);
				}

				final int queryStart = cleanedQuery.indexOf("?");
				if (queryStart >= 0 && queryStart < cleanedQuery.length()) {

					cleanedQuery = cleanedQuery.substring(0, queryStart);
				}

				return SchemaHelper.getEntityClassForRawType(cleanedQuery);
			}

			return null;
		}
	}
}
