/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.lucene.search.BooleanClause.Occur;
import org.jsoup.Jsoup;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Ownership;
import org.structr.core.Predicate;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.PropertyKey;
import org.structr.web.common.Function;
import org.structr.core.entity.LinkedTreeNode;
import org.structr.core.entity.Principal;
import org.structr.core.entity.relationship.PrincipalOwnsNode;
import org.structr.core.graph.search.PropertySearchAttribute;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.CollectionIdProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNode;
import org.structr.core.property.StringProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.common.ThreadLocalMatcher;
import org.structr.web.entity.PageData;
import org.structr.web.entity.Renderable;
import org.structr.web.entity.dom.relationship.DOMChildren;
import org.structr.web.entity.dom.relationship.DOMSiblings;
import org.structr.web.entity.html.relation.ResourceLink;
import org.structr.web.entity.relation.PageLink;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
//import jodd.http.HttpRequest;
//import jodd.http.HttpResponse;
//import jodd.jerry.Jerry;
//import static jodd.jerry.Jerry.jerry;

/**
 * Combines AbstractNode and org.w3c.dom.Node.
 *
 * @author Christian Morgner
 */
public abstract class DOMNode extends LinkedTreeNode<DOMChildren, DOMSiblings, DOMNode> implements Node, Renderable, DOMAdoptable, DOMImportable, PageData {

	private static final Logger logger = Logger.getLogger(DOMNode.class.getName());
	private static final ThreadLocalMatcher threadLocalTemplateMatcher = new ThreadLocalMatcher("\\$\\{[^}]*\\}");
	private static final ThreadLocalMatcher threadLocalFunctionMatcher = new ThreadLocalMatcher("([a-zA-Z0-9_]+)\\((.+)\\)");

	// ----- error messages for DOMExceptions -----
	protected static final String NO_MODIFICATION_ALLOWED_MESSAGE = "Permission denied.";
	protected static final String INVALID_ACCESS_ERR_MESSAGE = "Permission denied.";
	protected static final String INDEX_SIZE_ERR_MESSAGE = "Index out of range.";
	protected static final String CANNOT_SPLIT_TEXT_WITHOUT_PARENT = "Cannot split text element without parent and/or owner document.";
	protected static final String WRONG_DOCUMENT_ERR_MESSAGE = "Node does not belong to this document.";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE = "A node cannot accept itself as a child.";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_ANCESTOR = "A node cannot accept its own ancestor as child.";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT = "A document may only have one html element.";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT = "A document may only accept an html element as its document element.";
	protected static final String NOT_SUPPORTED_ERR_MESSAGE = "Node type not supported.";
	protected static final String NOT_FOUND_ERR_MESSAGE = "Node is not a child.";
	protected static final String NOT_SUPPORTED_ERR_MESSAGE_IMPORT_DOC = "Document nodes cannot be imported into another document.";
	protected static final String NOT_SUPPORTED_ERR_MESSAGE_ADOPT_DOC = "Document nodes cannot be adopted by another document.";
	protected static final String NOT_SUPPORTED_ERR_MESSAGE_RENAME = "Renaming of nodes is not supported by this implementation.";

	public static final Property<Boolean> hideOnIndex = new BooleanProperty("hideOnIndex");
	public static final Property<Boolean> hideOnDetail = new BooleanProperty("hideOnDetail");
	public static final Property<String> showForLocales = new StringProperty("showForLocales");
	public static final Property<String> hideForLocales = new StringProperty("hideForLocales");

	public static final Property<String> showConditions = new StringProperty("showConditions");
	public static final Property<String> hideConditions = new StringProperty("hideConditions");

	public static final Property<List<DOMNode>> children = new EndNodes<>("children", DOMChildren.class);
	public static final Property<DOMNode> parent = new StartNode<>("parent", DOMChildren.class);
	public static final Property<DOMNode> previousSibling = new StartNode<>("previousSibling", DOMSiblings.class);
	public static final Property<DOMNode> nextSibling = new EndNode<>("nextSibling", DOMSiblings.class);

	public static final Property<List<String>> childrenIds = new CollectionIdProperty("childrenIds", children);
	public static final Property<String> nextSiblingId = new EntityIdProperty("nextSiblingId", nextSibling);

	public static final Property<String> parentId = new EntityIdProperty("parentId", parent);

	public static final Property<Page> ownerDocument = new EndNode<>("ownerDocument", PageLink.class);
	public static final Property<String> pageId = new EntityIdProperty("pageId", ownerDocument);

	protected static final Map<String, Function<String, String>> functions = new LinkedHashMap<>();
	private static Set<Page> resultPages = new HashSet<>();

	static {

		functions.put("md5", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
					? DigestUtils.md5Hex(s[0])
					: "";

			}

		});
		functions.put("upper", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
					? s[0].toUpperCase()
					: "";

			}

		});
		functions.put("lower", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
					? s[0].toLowerCase()
					: "";

			}

		});
		functions.put("abbr", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s != null && s.length > 1 && s[0] != null && s[1] != null) {

					try {
						int maxLength = Integer.parseInt(s[1]);

						if (s[0].length() > maxLength) {

							return StringUtils.substringBeforeLast(StringUtils.substring(s[0], 0, maxLength), " ").concat("…");

						} else {

							return s[0];
						}

					} catch (NumberFormatException nfe) {

						return nfe.getMessage();

					}

				}

				return "";

			}

		});
		functions.put("capitalize", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
					? StringUtils.capitalize(s[0])
					: "";

			}
		});
		functions.put("titleize", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s == null || s.length < 2) {
					return null;
				}

				if (StringUtils.isBlank(s[0])) {
					return "";
				}

				if (s[1] == null) {
					s[1] = " ";
				}

				String[] in = StringUtils.split(s[0], s[1]);
				String[] out = new String[in.length];
				for (int i = 0; i < in.length; i++) {
					out[i] = StringUtils.capitalize(in[i]);
				};
				return StringUtils.join(out, " ");

			}

		});
		functions.put("clean", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				String result;

				if ((s != null) && (s.length > 0)) {

					if (StringUtils.isBlank(s[0])) {
						return "";
					}

					String normalized = Normalizer.normalize(s[0], Normalizer.Form.NFD)
						.replaceAll("\\<", "")
						.replaceAll("\\>", "")
						.replaceAll("\\.", "")
						.replaceAll("\\'", "-")
						.replaceAll("\\?", "")
						.replaceAll("\\(", "")
						.replaceAll("\\)", "")
						.replaceAll("\\{", "")
						.replaceAll("\\}", "")
						.replaceAll("\\[", "")
						.replaceAll("\\]", "")
						.replaceAll("\\+", "-")
						.replaceAll("/", "-")
						.replaceAll("–", "-")
						.replaceAll("\\\\", "-")
						.replaceAll("\\|", "-")
						.replaceAll("'", "-")
						.replaceAll("!", "")
						.replaceAll(",", "")
						.replaceAll("-", " ")
						.replaceAll("_", " ")
						.replaceAll("`", "-");

					result = normalized.replaceAll("-", " ");
					result = StringUtils.normalizeSpace(result.toLowerCase());
					result = result.replaceAll("[^\\p{ASCII}]", "").replaceAll("\\p{P}", "-").replaceAll("\\-(\\s+\\-)+", "-");
					result = result.replaceAll(" ", "-");

					return result;
				}

				return null;

			}

		});
		functions.put("urlencode", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
					? encodeURL(s[0])
					: "";

			}

		});
		functions.put("if", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s[0] == null || s.length < 3) {

					return "";
				}

				if (s[0].equals("true")) {

					return s[1];
				} else {

					return s[2];
				}

			}

		});
		functions.put("empty", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (StringUtils.isEmpty(s[0])) {

					return "true";
				} else {
					return "false";
				}

			}

		});
		functions.put("equal", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				logger.log(Level.FINE, "Length: {0}", s.length);

				if (s.length < 2) {

					return "true";
				}

				logger.log(Level.FINE, "Comparing {0} to {1}", new java.lang.Object[]{s[0], s[1]});

				if (s[0] == null || s[1] == null) {
					return "false";
				}

				return s[0].equals(s[1])
					? "true"
					: "false";

			}

		});
		functions.put("add", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				Double result = 0.0d;

				if (s != null) {

					for (String i : s) {

						try {

							result += Double.parseDouble(i);

						} catch (Throwable t) {

							return t.getMessage();

						}
					}

				}

				return new Double(result).toString();

			}

		});
		functions.put("subt", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s != null && s.length > 0) {

					try {

						Double result = Double.parseDouble(s[0]);

						for (int i = 1; i < s.length; i++) {

							result -= Double.parseDouble(s[i]);

						}

						return new Double(result).toString();

					} catch (Throwable t) {

						return t.getMessage();

					}
				}

				return "";

			}

		});
		functions.put("mult", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				Double result = 1.0d;

				if (s != null) {

					for (String i : s) {

						try {

							result *= Double.parseDouble(i);

						} catch (Throwable t) {

							return t.getMessage();

						}
					}

				}

				return new Double(result).toString();

			}

		});
		functions.put("quot", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				Double result = 0.0d;

				if (s != null && s.length == 2) {

					try {

						result = Double.parseDouble(s[0]) / Double.parseDouble(s[1]);

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return new Double(result).toString();

			}

		});
		functions.put("round", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				Double result = 0.0d;

				if (s != null && s.length == 2) {

					if (StringUtils.isBlank(s[0])) {
						return "";
					}

					try {

						Double f1 = Double.parseDouble(s[0]);
						double f2 = Math.pow(10, (Integer.parseInt(s[1])));
						long r = Math.round(f1 * f2);

						result = (double) r / f2;

					} catch (Throwable t) {

						return t.getMessage();

					}

				}

				return new Double(result).toString();

			}

		});
		functions.put("max", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				String result = "";
				String errorMsg = "ERROR! Usage: ${max(val1, val2)}. Example: ${max(5,10)}";

				if (s != null && s.length == 2) {

					try {
						result = Double.toString(Math.max(Double.parseDouble(s[0]), Double.parseDouble(s[1])));

					} catch (Throwable t) {
						logger.log(Level.WARNING, "Could not determine max() of {0} and {1}", new Object[]{s[0], s[1]});
						result = errorMsg;
					}

				}

				return result;

			}

		});
		functions.put("min", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				String result = "";
				String errorMsg = "ERROR! Usage: ${min(val1, val2)}. Example: ${min(5,10)}";

				if (s != null && s.length == 2) {

					try {
						result = Double.toString(Math.min(Double.parseDouble(s[0]), Double.parseDouble(s[1])));

					} catch (Throwable t) {
						logger.log(Level.WARNING, "Could not determine min() of {0} and {1}", new Object[]{s[0], s[1]});
						result = errorMsg;
					}

				}

				return result;

			}

		});
		functions.put("date_format", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				String result = "";
				String errorMsg = "ERROR! Usage: ${date_format(value, pattern)}. Example: ${date_format(Tue Feb 26 10:49:26 CET 2013, \"yyyy-MM-dd'T'HH:mm:ssZ\")}";

				if (s != null && s.length == 2) {

					String dateString = s[0];

					if (StringUtils.isBlank(dateString)) {
						return "";
					}

					String pattern = s[1];

					try {
						// parse with format from IS
						Date d = new SimpleDateFormat(ISO8601DateProperty.PATTERN).parse(dateString);

						// format with given pattern
						result = new SimpleDateFormat(pattern).format(d);

					} catch (ParseException ex) {
						logger.log(Level.WARNING, "Could not parse date " + dateString + " and format it to pattern " + pattern, ex);
						result = errorMsg;
					}

				}

				return result;
			}
		});
		functions.put("number_format", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				String result = "";
				String errorMsg = "ERROR! Usage: ${number_format(value, ISO639LangCode, pattern)}. Example: ${number_format(12345.6789, 'en', '#,##0.00')}";

				if (s != null && s.length == 3) {

					if (StringUtils.isBlank(s[0])) {
						return "";
					}

					try {

						Double val = Double.parseDouble(s[0]);
						String langCode = s[1];
						String pattern = s[2];

						NumberFormat formatter = DecimalFormat.getInstance(new Locale(langCode));
						((DecimalFormat) formatter).applyLocalizedPattern(pattern);
						result = formatter.format(val);

					} catch (Throwable t) {

						result = errorMsg;

					}

				} else {
					result = errorMsg;
				}

				return result;
			}

		});
		functions.put("not", new Function<String, String>() {

			@Override
			public String apply(String[] b) {

				if (b == null || b.length == 0) {
					return "";
				}
				
				return b[0].equals("true") ? "false" : "true";
			}

		});
		functions.put("and", new Function<String, String>() {

			@Override
			public String apply(String[] b) {

				boolean result = true;

				if (b != null) {

					for (String i : b) {

						try {

							result &= "true".equals(i);

						} catch (Throwable t) {

							return t.getMessage();

						}
					}

				}

				return Boolean.toString(result);
			}

		});
		functions.put("or", new Function<String, String>() {

			@Override
			public String apply(String[] b) {

				boolean result = false;

				if (b != null) {

					for (String i : b) {

						try {

							result |= "true".equals(i);

						} catch (Throwable t) {

							return t.getMessage();

						}
					}

				}

				return Boolean.toString(result);
			}

		});
		functions.put("GET", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				String result = "";
				String errorMsg = "ERROR! Usage: ${GET(URL[, contentType[, selector]])}. Example: ${GET('http://structr.org', 'text/html')}";

				if (s != null && s.length > 0) {

					try {

						String address = s[0];
						String contentType = null;

						if (s.length > 1) {
							contentType = s[1];
						}

						//long t0 = System.currentTimeMillis();
						if ("text/html".equals(contentType)) {

							String selector = null;

							if (s.length > 2) {

								selector = s[2];

//								String raw = getFromUrl2(address);
//								long t1 = System.currentTimeMillis();
//								Jerry doc = jerry(raw);
//								String html = doc.$(selector).html();
//								logger.log(Level.INFO, "Jerry took {0} ms to get and {1} ms to parse page.", new Object[]{t1 - t0, System.currentTimeMillis() - t1});
								String html = Jsoup.parse(new URL(address), 5000).select(selector).html();
								return html;

							} else {

								String html = Jsoup.parse(new URL(address), 5000).html();
								//logger.log(Level.INFO, "Jsoup took {0} ms to get and parse page.", (System.currentTimeMillis() - t0));

								return html;

							}

						} else {

							return getFromUrl(address);
						}

					} catch (Throwable t) {

						result = errorMsg + "\n" + t.getMessage();

					}

				} else {
					result = errorMsg;
				}

				return result;

			}

		});
	}

	/**
	 * This method will be called by the DOM logic when this node gets a new
	 * child. Override this method if you need to set properties on the
	 * child depending on its type etc.
	 *
	 * @param newChild
	 */
	protected void handleNewChild(Node newChild) {
		// override me
	}

	@Override
	public Class<DOMChildren> getChildLinkType() {
		return DOMChildren.class;
	}

	@Override
	public Class<DOMSiblings> getSiblingLinkType() {
		return DOMSiblings.class;
	}

	// ----- public methods -----
	@Override
	public String toString() {

		return getClass().getSimpleName() + " [" + getUuid() + "] (" + getTextContent() + ", " + treeGetChildPosition(this) + ")";
	}

	public List<DOMChildren> getChildRelationships() {
		return treeGetChildRelationships();
	}

	public String getPositionPath() {

		String path = "";

		DOMNode currentNode = this;
		while (currentNode.getParentNode() != null) {

			DOMNode parentNode = (DOMNode) currentNode.getParentNode();

			path = "/" + parentNode.treeGetChildPosition(currentNode) + path;

			currentNode = parentNode;

		}

		return path;

	}

	/**
	 * Get all ancestors of this node
	 *
	 * @return list of ancestors
	 */
	public List<Node> getAncestors() {

		List<Node> ancestors = new LinkedList();

		Node _parent = getParentNode();
		while (_parent != null) {

			ancestors.add(_parent);
			_parent = _parent.getParentNode();
		}

		return ancestors;

	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		try {

			increasePageVersion();

		} catch (FrameworkException ex) {

			logger.log(Level.WARNING, "Updating page version failed", ex);

		}

		return true;

	}

	// ----- private methods -----
	/**
	 * Do necessary updates on all containing pages
	 *
	 * @throws FrameworkException
	 */
	private void increasePageVersion() throws FrameworkException {

		Page page = (Page) getOwnerDocument();

		if (page != null) {

			page.unlockReadOnlyPropertiesOnce();
			page.increaseVersion();

		}

	}

	// ----- protected methods -----
	protected static String encodeURL(String source) {

		try {
			return URLEncoder.encode(source, "UTF-8");

		} catch (UnsupportedEncodingException ex) {

			logger.log(Level.WARNING, "Unsupported Encoding", ex);
		}

		// fallback, unencoded
		return source;
	}

	protected void checkIsChild(Node otherNode) throws DOMException {

		if (otherNode instanceof DOMNode) {

			Node _parent = otherNode.getParentNode();

			if (!isSameNode(_parent)) {

				throw new DOMException(DOMException.NOT_FOUND_ERR, NOT_FOUND_ERR_MESSAGE);
			}

			// validation successful
			return;
		}

		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE);
	}

	protected void checkHierarchy(Node otherNode) throws DOMException {

		// we can only check DOMNodes
		if (otherNode instanceof DOMNode) {

			// verify that the other node is not this node
			if (isSameNode(otherNode)) {
				throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE);
			}

			// verify that otherNode is not one of the
			// the ancestors of this node
			// (prevent circular relationships)
			Node _parent = getParentNode();
			while (_parent != null) {

				if (_parent.isSameNode(otherNode)) {
					throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_ANCESTOR);
				}

				_parent = _parent.getParentNode();
			}

			// TODO: check hierarchy constraints imposed by the schema
			// validation sucessful
			return;
		}

		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE);
	}

	protected void checkSameDocument(Node otherNode) throws DOMException {

		Document doc = getOwnerDocument();

		if (doc != null) {

			Document otherDoc = otherNode.getOwnerDocument();

			// Shadow doc is neutral
			if (otherDoc != null && !doc.equals(otherDoc) && !(doc instanceof ShadowDocument)) {

				throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, WRONG_DOCUMENT_ERR_MESSAGE);
			}

			if (otherDoc == null) {

				((DOMNode) otherNode).doAdopt((Page) doc);

			}
		}
	}

	protected void checkWriteAccess() throws DOMException {

		if (!securityContext.isAllowed(this, Permission.write)) {

			throw new DOMException(DOMException.NO_MODIFICATION_ALLOWED_ERR, NO_MODIFICATION_ALLOWED_MESSAGE);
		}
	}

	protected void checkReadAccess() throws DOMException {

		if (!securityContext.isAllowed(this, Permission.read)) {

			throw new DOMException(DOMException.INVALID_ACCESS_ERR, INVALID_ACCESS_ERR_MESSAGE);
		}
	}

	protected String indent(final int depth) {

		StringBuilder indent = new StringBuilder("\n");

		for (int d = 0; d < depth; d++) {

			indent.append("  ");

		}

		return indent.toString();
	}

	protected java.lang.Object getReferencedProperty(SecurityContext securityContext, RenderContext renderContext, String refKey)
		throws FrameworkException {

		final String DEFAULT_VALUE_SEP = "!";
		String pageId = renderContext.getPageId();
		String[] parts = refKey.split("[\\.]+");
		String referenceKey = parts[parts.length - 1];
		String defaultValue = null;

		if (StringUtils.contains(referenceKey, DEFAULT_VALUE_SEP)) {
			String[] ref = StringUtils.split(referenceKey, DEFAULT_VALUE_SEP);
			referenceKey = ref[0];
			if (ref.length > 1) {
				defaultValue = ref[1];
			} else {
				defaultValue = "";
			}
		}

		Page _page = renderContext.getPage();
		GraphObject _data = null;

		// walk through template parts
		for (int i = 0; (i < parts.length); i++) {

			String part = parts[i];
			String lowerCasePart = part.toLowerCase();

			if (_data != null) {

				Object value = _data.getProperty(StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), part));

				if (value instanceof GraphObject) {
					_data = (GraphObject) value;

					continue;

				}

				// special keyword "size"
				if (i > 0 && "size".equals(lowerCasePart)) {

					Object val = _data.getProperty(StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), parts[i - 1]));

					if (val instanceof List) {

						return ((List) val).size();

					}

				}

				// special keyword "link", works on deeper levels, too
				if ("link".equals(lowerCasePart) && _data instanceof AbstractNode) {

					ResourceLink rel = ((AbstractNode) _data).getOutgoingRelationship(ResourceLink.class);
					if (rel != null) {

						_data = rel.getTargetNode();

						break;

					}

					/*
					 for (AbstractRelationship rel : ((AbstractNode) _data).getRelationships(org.structr.web.common.RelType.LINK, Direction.OUTGOING)) {

					 _data = rel.getTargetNode();

					 break;

					 }
					 */
					continue;

				}

				if (value == null) {

					// Need to return null here to avoid _data sticking to the (wrong) parent object
					return null;

				}

			}

			// data objects from parent elements
			if (renderContext.hasDataForKey(lowerCasePart)) {

				_data = renderContext.getDataNode(lowerCasePart);

				continue;

			}

			// special keyword "request"
			if ("request".equals(lowerCasePart)) {

				HttpServletRequest request = renderContext.getRequest(); //securityContext.getRequest();

				if (request != null) {

					if (StringUtils.contains(refKey, "!")) {

						return StringUtils.defaultIfBlank(request.getParameter(referenceKey), defaultValue);

					} else {

						return StringUtils.defaultString(request.getParameter(referenceKey));
					}
				}

			}

			// special keyword "now":
			if ("now".equals(lowerCasePart)) {

				// Return current date converted in format
				// Note: We use "createdDate" here only as an arbitrary property key to get the database converter
				return AbstractNode.createdDate.inputConverter(securityContext).revert(new Date());

			}

			// special keyword "me"
			if ("me".equals(lowerCasePart)) {

				Principal me = (Principal) securityContext.getUser(false);

				if (me != null) {

					_data = me;

					continue;
				}

			}

			// the following keywords work only on root level
			// so that they can be used as property keys for data objects
			if (_data == null) {

				// details data object id
				if ("id".equals(lowerCasePart)) {
					return renderContext.getDetailsDataObject().getUuid();
				}

				// special keyword "this"
				if ("this".equals(lowerCasePart)) {

					_data = renderContext.getDataObject();

					continue;

				}

				// special keyword "ownerDocument", works only on root level
				if ("page".equals(lowerCasePart)) {

					_data = _page;

					continue;

				}

				// special keyword "link", works only on root level
				if ("link".equals(lowerCasePart)) {

					ResourceLink rel = getOutgoingRelationship(ResourceLink.class);

					if (rel != null) {
						_data = rel.getTargetNode();
						break;
					}

					/*
					 for (AbstractRelationship rel : getRelationships(org.structr.web.common.RelType.LINK, Direction.OUTGOING)) {

					 _data = rel.getTargetNode();

					 break;

					 }
					
					 continue;
					 */
				}

				// special keyword "parent"
				if ("parent".equals(lowerCasePart)) {

					_data = (DOMNode) getParentNode();

					continue;

				}

				// special keyword "owner"
				if ("owner".equals(lowerCasePart)) {

					Ownership rel = getIncomingRelationship(PrincipalOwnsNode.class);
					if (rel != null) {

						_data = rel.getSourceNode();
					}

					continue;

				}

				// special keyword "search_result_size"
				if ("search_result_size".equals(lowerCasePart)) {

					Set<Page> pages = getResultPages(securityContext, (Page) _page);

					if (!pages.isEmpty()) {

						return pages.size();
					}

					return 0;

				}

				// special keyword "result_size"
				if ("result_count".equals(lowerCasePart) || "result_size".equals(lowerCasePart)) {

					Result result = renderContext.getResult();

					if (result != null) {

						return result.getRawResultCount();

					}

				}

				// special keyword "page_size"
				if ("page_size".equals(lowerCasePart)) {

					Result result = renderContext.getResult();

					if (result != null) {

						return result.getPageSize();

					}

				}

				// special keyword "page_count"
				if ("page_count".equals(lowerCasePart)) {

					Result result = renderContext.getResult();

					if (result != null) {

						return result.getPageCount();

					}

				}

				// special keyword "page_no"
				if ("page_no".equals(lowerCasePart)) {

					Result result = renderContext.getResult();

					if (result != null) {

						return result.getPage();

					}

				}

				//				// special keyword "rest_result"
//				if ("rest_result".equals(lowerCasePart)) {
//
//					HttpServletRequest request = securityContext.getRequest();
//
//					if (request != null) {
//
//						return StringEscapeUtils.escapeJavaScript(StringUtils.replace(StringUtils.defaultString((String) request.getAttribute(HtmlServlet.REST_RESPONSE)), "\n", ""));
//					}
//
//					return 0;
//
//				}
//				
			}

		}

		if (_data != null) {

			PropertyKey referenceKeyProperty = StructrApp.getConfiguration().getPropertyKeyForJSONName(_data.getClass(), referenceKey);
			//return getEditModeValue(securityContext, renderContext, _data, referenceKeyProperty, defaultValue);
			Object value = _data.getProperty(referenceKeyProperty);

			PropertyConverter converter = referenceKeyProperty.inputConverter(securityContext);

			if (value != null && converter != null) {
				value = converter.revert(value);
			}

			return value != null ? value : defaultValue;

		}

		return null;

	}

	protected String getPropertyWithVariableReplacement(SecurityContext securityContext, RenderContext renderContext, PropertyKey<String> key) throws FrameworkException {

		return replaceVariables(securityContext, renderContext, getProperty(key));

	}

	/**
	 * @deprecated This method uses the security context of instantiation
	 * which is a bad idea. Use
	 * {@link DOMNode#getPropertyWithVariableReplacement} instead
	 *
	 * @param renderContext
	 * @param key
	 * @return
	 * @throws FrameworkException
	 */
	protected String getPropertyWithVariableReplacement(RenderContext renderContext, PropertyKey<String> key) throws FrameworkException {

		HttpServletRequest request = renderContext.getRequest();

		if (securityContext.getRequest() == null) {

			securityContext.setRequest(request);
		}

		return replaceVariables(securityContext, renderContext, super.getProperty(key));

	}

	protected String replaceVariables(SecurityContext securityContext, RenderContext renderContext, Object rawValue)
		throws FrameworkException {

		String value = null;

		if (rawValue == null) {

			return null;

		}

		if (rawValue instanceof String) {

			value = (String) rawValue;

			if (!(EditMode.RAW.equals(renderContext.getEditMode(securityContext.getUser(false))))) {

				// re-use matcher from previous calls
				Matcher matcher = threadLocalTemplateMatcher.get();

				matcher.reset(value);

				while (matcher.find()) {

					String group = matcher.group();
					String source = group.substring(2, group.length() - 1);

					// fetch referenced property
					String partValue = extractFunctions(securityContext, renderContext, source);

					if (partValue != null) {

						value = value.replace(group, partValue);
					} else {

						// If the whole expression should be replaced, and partValue is null
						// replace it by null to make it possible for HTML attributes to not be rendered
						// and avoid something like ... selected="" ... which is interpreted as selected==true by
						// all browsers
						value = value.equals(group) ? null : value.replace(group, "");
					}

				}

			}

		} else if (rawValue instanceof Boolean) {

			value = Boolean.toString((Boolean) rawValue);

		} else {

			value = rawValue.toString();

		}

		return value;

	}

	protected String extractFunctions(SecurityContext securityContext, RenderContext renderContext, String source)
		throws FrameworkException {

		String pageId = renderContext.getPageId();

		// re-use matcher from previous calls
		Matcher functionMatcher = threadLocalFunctionMatcher.get();

		functionMatcher.reset(source);

		if (functionMatcher.matches()) {

			String functionGroup = functionMatcher.group(1);
			String parameter = functionMatcher.group(2);
			String functionName = functionGroup.substring(0, functionGroup.length());
			Function<String, String> function = functions.get(functionName);

			if (function != null) {

				if (parameter.contains(",")) {

					String[] parameters = split(parameter);
					String[] results = new String[parameters.length];

					// collect results from comma-separated function parameter
					for (int i = 0; i < parameters.length; i++) {

						results[i] = extractFunctions(securityContext, renderContext, StringUtils.strip(parameters[i]));
					}

					return function.apply(results);

				} else {

					String result = extractFunctions(securityContext, renderContext, StringUtils.strip(parameter));

					return function.apply(new String[]{result});

				}
			}

		}

		// if any of the following conditions match, the literal source value is returned
		if (StringUtils.isNotBlank(source) && StringUtils.isNumeric(source)) {

			// return numeric value
			return source;
		} else if (source.startsWith("\"") && source.endsWith("\"")) {

			return source.substring(1, source.length() - 1);
		} else if (source.startsWith("'") && source.endsWith("'")) {

			return source.substring(1, source.length() - 1);
		} else {

			// return property key
			return convertValueForHtml(getReferencedProperty(securityContext, renderContext, source));
		}
	}

	/**
	 * Return (cached) result pages
	 *
	 * Search string is taken from SecurityContext's http request Given
	 * displayPage is substracted from search result (we don't want to
	 * return search result ownerDocument in search results)
	 *
	 * @param securityContext
	 * @param displayPage
	 * @return
	 */
	protected Set<Page> getResultPages(final SecurityContext securityContext, final Page displayPage) {

		HttpServletRequest request = securityContext.getRequest();
		String search = request.getParameter("search");

		if (StringUtils.isEmpty(search)) {

			return Collections.EMPTY_SET;
		}

		resultPages = (Set<Page>) request.getAttribute("searchResults");

		if ((resultPages != null) && !resultPages.isEmpty()) {

			return resultPages;
		}

		if (resultPages == null) {

			resultPages = new HashSet<>();
		}

		// fetch search results
		// List<GraphObject> results              = ((SearchResultView) startNode).getGraphObjects(request);
		List<SearchAttribute> searchAttributes = new LinkedList<>();

		searchAttributes.add(new PropertySearchAttribute(Content.content, search, Occur.MUST, false));
		searchAttributes.add(Search.andExactType(Content.class));

		try {

			Result<Content> result = StructrApp.getInstance().command(SearchNodeCommand.class).execute(searchAttributes);
			for (Content content : result.getResults()) {

				resultPages.add((Page) content.getOwnerDocument());
			}

			// Remove result ownerDocument itself
			resultPages.remove((Page) displayPage);

		} catch (FrameworkException fe) {

			logger.log(Level.WARNING, "Error while searching in content", fe);

		}

		return resultPages;

	}

	/**
	 * Decide whether this node should be displayed for the given conditions
	 * string.
	 *
	 * @param securityContext
	 * @param renderContext
	 * @return
	 */
	protected boolean displayForConditions(final SecurityContext securityContext, final RenderContext renderContext) {

		// In raw mode, render everything
		if (EditMode.RAW.equals(renderContext.getEditMode(securityContext.getUser(false)))) {
			return true;
		}

		String _showConditions = getProperty(DOMNode.showConditions);
		String _hideConditions = getProperty(DOMNode.hideConditions);

		// If both fields are empty, render node
		if (StringUtils.isBlank(_hideConditions) && StringUtils.isBlank(_showConditions)) {
			return true;
		}
		try {
			// If hide conditions evaluate to "true", don't render
			if (StringUtils.isNotBlank(_hideConditions) && "true".equals(extractFunctions(securityContext, renderContext, _hideConditions))) {
				return false;
			}

		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, "Hide conditions " + _hideConditions + " could not be evaluated.", ex);
		}
		try {
			// If show conditions don't evaluate to "true", don't render
			if (StringUtils.isNotBlank(_showConditions) && !("true".equals(extractFunctions(securityContext, renderContext, _showConditions)))) {
				return false;
			}

		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, "Show conditions " + _showConditions + " could not be evaluated.", ex);
		}

		return true;

	}

	/**
	 * Decide whether this node should be displayed for the given locale
	 * settings.
	 *
	 * @param renderContext
	 * @return
	 */
	protected boolean displayForLocale(final RenderContext renderContext) {

		// In raw mode, render everything
		if (EditMode.RAW.equals(renderContext.getEditMode(securityContext.getUser(false)))) {
			return true;
		}

		String localeString = renderContext.getLocale().toString();

		String show = getProperty(DOMNode.showForLocales);
		String hide = getProperty(DOMNode.hideForLocales);

		// If both fields are empty, render node
		if (StringUtils.isBlank(hide) && StringUtils.isBlank(show)) {
			return true;
		}

		// If locale string is found in hide, don't render
		if (StringUtils.contains(hide, localeString)) {
			return false;
		}

		// If locale string is found in hide, don't render
		if (StringUtils.isNotBlank(show) && !StringUtils.contains(show, localeString)) {
			return false;
		}

		return true;

	}

	protected String convertValueForHtml(java.lang.Object value) {

		if (value != null) {

			// TODO: do more intelligent conversion here
			return value.toString();
		}

		return null;

	}

	protected String escapeForHtml(final String raw) {

		return StringUtils.replaceEach(raw, new String[]{"&", "<", ">"}, new String[]{"&amp;", "&lt;", "&gt;"});

	}

	protected String escapeForHtmlAttributes(final String raw) {

		return StringUtils.replaceEach(raw, new String[]{"&", "<", ">", "\"", "'"}, new String[]{"&amp;", "&lt;", "&gt;", "&quot;", "&#39;"});

	}

	protected String[] split(String source) {

		ArrayList<String> tokens = new ArrayList<>(20);
		boolean inDoubleQuotes = false;
		boolean inSingleQuotes = false;
		int len = source.length();
		int level = 0;
		StringBuilder currentToken = new StringBuilder(len);

		for (int i = 0; i < len; i++) {

			char c = source.charAt(i);

			// do not strip away separators in nested functions!
			if ((level != 0) || (c != ',')) {

				currentToken.append(c);
			}

			switch (c) {

				case '(':
					level++;

					break;

				case ')':
					level--;

					break;

				case '"':
					if (inDoubleQuotes) {

						inDoubleQuotes = false;

						level--;

					} else {

						inDoubleQuotes = true;

						level++;

					}

					break;

				case '\'':
					if (inSingleQuotes) {

						inSingleQuotes = false;

						level--;

					} else {

						inSingleQuotes = true;

						level++;

					}

					break;

				case ',':
					if (level == 0) {

						tokens.add(currentToken.toString().trim());
						currentToken.setLength(0);

					}

					break;

			}

		}

		if (currentToken.length() > 0) {

			tokens.add(currentToken.toString().trim());
		}

		return tokens.toArray(new String[0]);

	}

	protected void collectNodesByPredicate(Node startNode, DOMNodeList results, Predicate<Node> predicate, int depth, boolean stopOnFirstHit) {

		if (predicate.evaluate(securityContext, startNode)) {

			results.add(startNode);

			if (stopOnFirstHit) {

				return;
			}
		}

		NodeList _children = startNode.getChildNodes();
		if (_children != null) {

			int len = _children.getLength();
			for (int i = 0; i < len; i++) {

				Node child = _children.item(i);

				collectNodesByPredicate(child, results, predicate, depth + 1, stopOnFirstHit);
			}
		}
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getTextContent() throws DOMException {

		final DOMNodeList results = new DOMNodeList();
		final TextCollector textCollector = new TextCollector();

		collectNodesByPredicate(this, results, textCollector, 0, false);

		return textCollector.getText();
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		// TODO: implement?
	}

	@Override
	public Node getParentNode() {
		// FIXME: type cast correct here?
		return (Node) getProperty(parent);
	}

	@Override
	public NodeList getChildNodes() {

		checkReadAccess();

		return new DOMNodeList(treeGetChildren());
	}

	@Override
	public Node getFirstChild() {
		return treeGetFirstChild();
	}

	@Override
	public Node getLastChild() {
		return treeGetLastChild();
	}

	@Override
	public Node getPreviousSibling() {
		return listGetPrevious(this);
	}

	@Override
	public Node getNextSibling() {
		return listGetNext(this);
	}

	@Override
	public Document getOwnerDocument() {
		return getProperty(ownerDocument);
	}

	@Override
	public Node insertBefore(final Node newChild, final Node refChild) throws DOMException {

		// according to DOM spec, insertBefore with null refChild equals appendChild
		if (refChild == null) {

			return appendChild(newChild);
		}

		checkWriteAccess();

		checkSameDocument(newChild);
		checkSameDocument(refChild);

		checkHierarchy(newChild);
		checkHierarchy(refChild);

		try {

			if (newChild instanceof DocumentFragment) {

				// When inserting document fragments, we must take
				// care of the special case that the nodes already
				// have a NEXT_LIST_ENTRY relationship coming from
				// the document fragment, so we must first remove
				// the node from the document fragment and then
				// add it to the new parent.
				DocumentFragment fragment = (DocumentFragment) newChild;
				Node currentChild = fragment.getFirstChild();

				while (currentChild != null) {

					// save next child in fragment list for later use
					Node savedNextChild = currentChild.getNextSibling();

					// remove child from document fragment
					fragment.removeChild(currentChild);

					// insert child into new parent
					insertBefore(currentChild, refChild);

					// next
					currentChild = savedNextChild;
				}

			} else {

				Node _parent = newChild.getParentNode();
				if (_parent != null) {

					_parent.removeChild(newChild);
				}

				treeInsertBefore((DOMNode) newChild, (DOMNode) refChild);

				// allow parent to set properties in new child
				handleNewChild(newChild);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return refChild;
	}

	@Override
	public Node replaceChild(final Node newChild, final Node oldChild) throws DOMException {

		checkWriteAccess();

		checkSameDocument(newChild);
		checkSameDocument(oldChild);

		checkHierarchy(newChild);
		checkHierarchy(oldChild);

		try {

			if (newChild instanceof DocumentFragment) {

				// When inserting document fragments, we must take
				// care of the special case that the nodes already
				// have a NEXT_LIST_ENTRY relationship coming from
				// the document fragment, so we must first remove
				// the node from the document fragment and then
				// add it to the new parent.
				// replace indirectly using insertBefore and remove
				DocumentFragment fragment = (DocumentFragment) newChild;
				Node currentChild = fragment.getFirstChild();

				while (currentChild != null) {

					// save next child in fragment list for later use
					Node savedNextChild = currentChild.getNextSibling();

					// remove child from document fragment
					fragment.removeChild(currentChild);

					// add child to new parent
					insertBefore(currentChild, oldChild);

					// next
					currentChild = savedNextChild;
				}

				// finally, remove reference element
				removeChild(oldChild);

			} else {

				Node _parent = newChild.getParentNode();
				if (_parent != null && _parent instanceof DOMNode) {

					_parent.removeChild(newChild);
				}

				// replace directly
				treeReplaceChild((DOMNode) newChild, (DOMNode) oldChild);

				// allow parent to set properties in new child
				handleNewChild(newChild);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return oldChild;
	}

	@Override
	public Node removeChild(final Node node) throws DOMException {

		checkWriteAccess();
		checkSameDocument(node);
		checkIsChild(node);

		try {

			treeRemoveChild((DOMNode) node);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return node;
	}

	@Override
	public Node appendChild(final Node newChild) throws DOMException {

		checkWriteAccess();
		checkSameDocument(newChild);
		checkHierarchy(newChild);

		try {

			if (newChild instanceof DocumentFragment) {

				// When inserting document fragments, we must take
				// care of the special case that the nodes already
				// have a NEXT_LIST_ENTRY relationship coming from
				// the document fragment, so we must first remove
				// the node from the document fragment and then
				// add it to the new parent.
				// replace indirectly using insertBefore and remove
				DocumentFragment fragment = (DocumentFragment) newChild;
				Node currentChild = fragment.getFirstChild();

				while (currentChild != null) {

					// save next child in fragment list for later use
					Node savedNextChild = currentChild.getNextSibling();

					// remove child from document fragment
					fragment.removeChild(currentChild);

					// append child to new parent
					appendChild(currentChild);

					// next
					currentChild = savedNextChild;
				}

			} else {

				Node _parent = newChild.getParentNode();

				if (_parent != null && _parent instanceof DOMNode) {
					_parent.removeChild(newChild);
				}

				treeAppendChild((DOMNode) newChild);

				// allow parent to set properties in new child
				handleNewChild(newChild);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}

		return newChild;
	}

	@Override
	public boolean hasChildNodes() {
		return !getProperty(children).isEmpty();
	}

	@Override
	public Node cloneNode(boolean deep) {

		if (deep) {

			throw new UnsupportedOperationException("cloneNode with deep=true is not supported yet.");

		} else {

			final PropertyMap properties = new PropertyMap();

			for (Iterator<PropertyKey> it = getPropertyKeys(uiView.name()).iterator(); it.hasNext();) {

				PropertyKey key = it.next();

				// omit system properties (except type), parent/children and page relationships
				if (key.equals(GraphObject.type) || (!key.isUnvalidated()
					&& !key.equals(GraphObject.id)
					&& !key.equals(DOMNode.ownerDocument) && !key.equals(DOMNode.pageId)
					&& !key.equals(DOMNode.parent) && !key.equals(DOMNode.parentId)
					&& !key.equals(DOMNode.children) && !key.equals(DOMNode.childrenIds))) {

					properties.put(key, getProperty(key));
				}
			}

			final App app = StructrApp.getInstance(securityContext);

			try {
				app.beginTx();
				DOMNode node = app.create(getClass(), properties);
				app.commitTx();

				return node;

			} catch (FrameworkException ex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, ex.toString());

			} finally {

				app.finishTx();
			}

		}
	}

	@Override
	public boolean isSupported(String string, String string1) {
		return false;
	}

	@Override
	public String getNamespaceURI() {
		return null; //return "http://www.w3.org/1999/xhtml";
	}

	@Override
	public String getPrefix() {
		return null;
	}

	@Override
	public void setPrefix(String prefix) throws DOMException {
	}

	@Override
	public String getBaseURI() {
		return null;
	}

	@Override
	public short compareDocumentPosition(Node node) throws DOMException {
		return 0;
	}

	@Override
	public boolean isSameNode(Node node) {

		if (node != null && node instanceof DOMNode) {

			String otherId = ((DOMNode) node).getProperty(GraphObject.id);
			String ourId = getProperty(GraphObject.id);

			if (ourId != null && otherId != null && ourId.equals(otherId)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String lookupPrefix(String string) {
		return null;
	}

	@Override
	public boolean isDefaultNamespace(String string) {
		return true;
	}

	@Override
	public String lookupNamespaceURI(String string) {
		return null;
	}

	@Override
	public boolean isEqualNode(Node node) {
		return equals(node);
	}

	@Override
	public Object getFeature(String string, String string1) {
		return null;
	}

	@Override
	public Object setUserData(String string, Object o, UserDataHandler udh) {
		return null;
	}

	@Override
	public Object getUserData(String string) {
		return null;
	}

	@Override
	public final void normalize() {

		final App app = StructrApp.getInstance(securityContext);

		try {

			Document document = getOwnerDocument();
			if (document != null) {

				// merge adjacent text nodes until there is only one left
				Node child = getFirstChild();
				while (child != null) {

					if (child instanceof Text) {

						Node next = child.getNextSibling();
						if (next != null && next instanceof Text) {

							String text1 = child.getNodeValue();
							String text2 = next.getNodeValue();

							// create new text node
							Text newText = document.createTextNode(text1.concat(text2));

							removeChild(child);
							insertBefore(newText, next);
							removeChild(next);

							child = newText;

						} else {

							// advance to next node
							child = next;
						}

					} else {

						// advance to next node
						child = child.getNextSibling();

					}
				}

				// recursively normalize child nodes
				if (hasChildNodes()) {

					Node currentChild = getFirstChild();
					while (currentChild != null) {

						currentChild.normalize();
						currentChild = currentChild.getNextSibling();
					}
				}
			}

			app.commitTx();

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		} finally {

			app.finishTx();
		}
	}

	// ----- interface DOMAdoptable -----
	@Override
	public Node doAdopt(final Page _page) throws DOMException {

		if (_page != null) {

			final App app = StructrApp.getInstance(securityContext);

			try {

				app.beginTx();
				setProperty(ownerDocument, _page);
				app.commitTx();

			} catch (FrameworkException fex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

			} finally {

				app.finishTx();
			}
		}

		return this;
	}

	// ----- nested classes -----
	protected static class TextCollector implements Predicate<Node> {

		private StringBuilder textBuffer = new StringBuilder(200);

		@Override
		public boolean evaluate(SecurityContext securityContext, Node... obj) {

			if (obj[0] instanceof Text) {
				textBuffer.append(((Text) obj[0]).getTextContent());
			}

			return false;
		}

		public String getText() {
			return textBuffer.toString();
		}
	}

	protected static class TagPredicate implements Predicate<Node> {

		private String tagName = null;

		public TagPredicate(String tagName) {
			this.tagName = tagName;
		}

		@Override
		public boolean evaluate(SecurityContext securityContext, Node... obj) {

			if (obj[0] instanceof DOMElement) {

				DOMElement elem = (DOMElement) obj[0];

				if (tagName.equals(elem.getProperty(DOMElement.tag))) {
					return true;
				}
			}

			return false;
		}
	}

	private static String getFromUrl(final String requestUrl) throws IOException {

		DefaultHttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(requestUrl);

		get.setHeader("Connection", "close");

		return IOUtils.toString(client.execute(get).getEntity().getContent(), "UTF-8");

	}

//	private static String getFromUrl2(final String requestUrl) throws IOException {
//		
//		HttpRequest httpRequest = HttpRequest.get(requestUrl);
//		HttpResponse response = httpRequest.send();
//		return response.body();
//		
//	}
	public static Set<DOMNode> getAllChildNodes(final DOMNode node) {

		Set<DOMNode> allChildNodes = new HashSet();

		DOMNode n = (DOMNode) node.getFirstChild();

		while (n != null) {

			allChildNodes.add(n);
			allChildNodes.addAll(getAllChildNodes(n));
			n = (DOMNode) n.getNextSibling();

		}

		return allChildNodes;
	}

}
