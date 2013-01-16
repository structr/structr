package org.structr.web.entity.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.neo4j.graphdb.Direction;
import org.structr.common.Permission;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.ThreadLocalCommand;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchAttribute;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.EntityProperty;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.web.common.Function;
import org.structr.web.common.PageHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.common.ThreadLocalMatcher;
import org.structr.web.entity.Renderable;
import org.structr.web.entity.relation.ChildrenRelationship;
import org.structr.web.servlet.HtmlServlet;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;

/**
 * Combines AbstractNode and org.w3c.dom.Node.
 * 
 * @author Christian Morgner
 */

public abstract class DOMNode extends AbstractNode implements Node, Renderable {

	private static final Logger logger                                      = Logger.getLogger(DOMNode.class.getName());
	private static final ThreadLocalMatcher threadLocalTemplateMatcher      = new ThreadLocalMatcher("\\$\\{[^}]*\\}");
	private static final ThreadLocalMatcher threadLocalFunctionMatcher      = new ThreadLocalMatcher("([a-zA-Z0-9_]+)\\((.+)\\)");
	
	// ----- error messages for DOMExceptions -----
	protected static final String NO_MODIFICATION_ALLOWED_MESSAGE           = "Permission denied";
	protected static final String INVALID_ACCESS_ERR_MESSAGE                = "Permission denied";
	protected static final String INDEX_SIZE_ERR_MESSAGE                    = "Index out of range";
	protected static final String CANNOT_SPLIT_TEXT_WITHOUT_PARENT          = "Cannot split text element without parent and/or owner document";
	protected static final String WRONG_DOCUMENT_ERR_MESSAGE                = "Nodes are not created by the same document";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_SAME_NODE   = "A node cannot accept itself as a child";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_ANCESTOR    = "A node cannot accept its own ancestor as child";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT    = "A document may only have one document element";
	protected static final String HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT     = "A document may only accept an html element as its document element";
	protected static final String NOT_SUPPORTED_ERR_MESSAGE                 = "Node type not supported";
	protected static final String NOT_FOUND_ERR_MESSAGE                     = "Node is not a child";
	
	protected static final Map<String, Function<String, String>> functions  = new LinkedHashMap<String, Function<String, String>>();
	
	public static final CollectionProperty<DOMNode> children                = new CollectionProperty<DOMNode>("children", DOMNode.class, RelType.CONTAINS, Direction.OUTGOING, true);
	public static final EntityProperty<DOMNode> parent                      = new EntityProperty<DOMNode>("parent", DOMNode.class, RelType.CONTAINS, Direction.INCOMING, false);
	public static final EntityProperty<Page> page                           = new EntityProperty<Page>("page", Page.class, RelType.PAGE, Direction.OUTGOING, true);

	private static Set<Page> resultPages                                    = new HashSet<Page>();
	
	static {

		functions.put("md5", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? DigestUtils.md5Hex(s[0])
				       : null;

			}

		});
		functions.put("upper", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? s[0].toUpperCase()
				       : null;

			}

		});
		functions.put("lower", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? s[0].toLowerCase()
				       : null;

			}

		});
		functions.put("capitalize", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				return ((s != null) && (s.length > 0) && (s[0] != null))
				       ? StringUtils.capitalize(s[0])
				       : null;

			}

		});
		functions.put("if", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s.length < 3) {

					return "";
				}

				if (s[0].equals("true")) {

					return s[1];
				} else {

					return s[2];
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

				logger.log(Level.FINE, "Comparing {0} to {1}", new java.lang.Object[] { s[0], s[1] });

				return s[0].equals(s[1])
				       ? "true"
				       : "false";

			}

		});
		functions.put("add", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				int result = 0;

				if (s != null) {

					for (int i = 0; i < s.length; i++) {

						try {

							result += Integer.parseInt(s[i]);

						} catch (Throwable t) {}

					}

				}

				return new Integer(result).toString();

			}

		});
		functions.put("active", new Function<String, String>() {

			@Override
			public String apply(String[] s) {

				if (s.length == 0) {

					return "";
				}

				String data = this.dataId.get();
				String page = this.pageId.get();

				if (data != null && page != null) {

					if (data.equals(page)) {

						// return first argument if condition is true
						return s[0];
					} else if (s.length > 1) {

						// return second argument if condition is false and second argument exists
						return s[1];
					}

				}

				return "";

			}

		});

	}
	
	// ----- public methods -----
	public List<AbstractRelationship> getChildRelationships() {

		// fetch all relationships
		List<AbstractRelationship> childRels = getOutgoingRelationships(RelType.CONTAINS);
		
		// sort relationships by position
		Collections.sort(childRels, new Comparator<AbstractRelationship>() {

			@Override
			public int compare(AbstractRelationship o1, AbstractRelationship o2) {

				Integer pos1 = o1.getProperty(ChildrenRelationship.position);
				Integer pos2 = o2.getProperty(ChildrenRelationship.position);

				if (pos1 != null && pos2 != null) {
				
					return pos1.compareTo(pos2);	
				}
				
				return 0;
			}

		});

		return childRels;
	}

	// ----- protected methods -----
	/**
	 * Ensures that the position attributes of the children of this node
	 * are correct. Please note that this method needs to run in the same
	 * transaction as any modifiying operation that changes the order of
	 * child nodes, and therefore this method does _not_ create its own
	 * transaction. However, it will not raise a NotInTransactionException
	 * when called outside of modifying operations, because each setProperty
	 * call creates its own transaction.
	 */
	protected void ensureCorrectChildPositions() {
		
		try {
			
			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					List<AbstractRelationship> childRels = getChildRelationships();
					int position                         = 0;
					
					for (AbstractRelationship childRel : childRels) {
						
						childRel.setProperty(ChildrenRelationship.position, position++);
					}
					
					return null;
				}
				
			});
			
		} catch (FrameworkException fex) {
			
			fex.printStackTrace();
		}
	}
	
	protected int getPositionInParent() {
		
		List<AbstractRelationship> rels = getIncomingRelationships(RelType.CONTAINS);
		if (rels != null && rels.size() == 1) {
			
			// node should have only one parent
			AbstractRelationship rel = rels.get(0);
			
			Integer pos = rel.getProperty(ChildrenRelationship.position);
			if (pos != null) {
				
				return pos.intValue();
			}
			
		} else {
			
			logger.log(Level.WARNING, "Node with id {0} has {1} parents", new Object[] { getProperty(GraphObject.uuid), rels.size() } );
		}
		
		return -1;
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

			if (!doc.equals(otherNode.getOwnerDocument())) {

				throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, WRONG_DOCUMENT_ERR_MESSAGE);
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
	
	protected String indent(final int depth, final boolean newline) {

		StringBuilder indent = new StringBuilder();

		if (newline) {

			indent.append("\n");

		}

		for (int d = 0; d < depth; d++) {

			indent.append("  ");

		}

		return indent.toString();
	}

	protected java.lang.Object getReferencedProperty(SecurityContext securityContext, RenderContext renderContext, String refKey)
		throws FrameworkException {

		AbstractNode node = null;
		String pageId = renderContext.getPageId();
		String[] parts                   = refKey.split("[\\.]+");
		String referenceKey              = parts[parts.length - 1];
		PropertyKey pageIdProperty       = new StringProperty(pageId);
		PropertyKey referenceKeyProperty = new StringProperty(referenceKey);
		
		Page page = renderContext.getPage();
		String componentId = renderContext.getComponentId();
		AbstractNode viewComponent = renderContext.getViewComponent();
		
		

		// walk through template parts
		for (int i = 0; (i < parts.length); i++) {

			String part = parts[i];

			// special keyword "request"
			if ("request".equals(part.toLowerCase())) {

				HttpServletRequest request = securityContext.getRequest();

				if (request != null) {

					return StringUtils.defaultString(request.getParameter(referenceKey));
				}

			}

			// special keyword "component"
			if ("component".equals(part.toLowerCase())) {

				node = PageHelper.getNodeById(securityContext, componentId);

				continue;

			}

			// special keyword "resource"
			if ("resource".equals(part.toLowerCase())) {

				node = PageHelper.getNodeById(securityContext, pageId);

				continue;

			}

			// special keyword "page"
			if ("page".equals(part.toLowerCase())) {

				node = page;

				continue;

			}

			// special keyword "link"
			if ("link".equals(part.toLowerCase())) {

				for (AbstractRelationship rel : getRelationships(RelType.LINK, Direction.OUTGOING)) {

					node = rel.getEndNode();

					break;

				}

				continue;

			}

			// special keyword "parent"
			if ("parent".equals(part.toLowerCase())) {

				for (AbstractRelationship rel : getRelationships(RelType.CONTAINS, Direction.INCOMING)) {

					if (rel.getProperty(pageIdProperty) != null) {

						node = rel.getStartNode();

						break;

					}

				}

				continue;

			}

			// special keyword "owner"
			if ("owner".equals(part.toLowerCase())) {

				for (AbstractRelationship rel : getRelationships(RelType.OWNS, Direction.INCOMING)) {

					node = rel.getStartNode();

					break;

				}

				continue;

			}

			// special keyword "data"
			if ("data".equals(part.toLowerCase())) {

				node = viewComponent;

				continue;

			}

			// special keyword "root": Find containing page
			if ("root".equals(part.toLowerCase())) {

				List<Page> pages = PageHelper.getPages(securityContext, viewComponent);

				if (pages.isEmpty()) {

					continue;
				}

				node = pages.get(0);

				continue;

			}

			// special keyword "result_size"
			if ("result_size".equals(part.toLowerCase())) {

				Set<Page> pages = getResultPages(securityContext, (Page) page);

				if (!pages.isEmpty()) {

					return pages.size();
				}

				return 0;

			}

			// special keyword "rest_result"
			if ("rest_result".equals(part.toLowerCase())) {

				HttpServletRequest request = securityContext.getRequest();

				if (request != null) {

					return StringEscapeUtils.escapeJavaScript(StringUtils.replace(StringUtils.defaultString((String) request.getAttribute(HtmlServlet.REST_RESPONSE)), "\n", ""));
				}

				return 0;

			}

		}

		if (node != null) {

			return node.getProperty(referenceKeyProperty);
		}

		return null;

	}

	protected String getPropertyWithVariableReplacement(SecurityContext securityContext, RenderContext renderContext, PropertyKey<String> key) throws FrameworkException {

		return replaceVariables(securityContext, renderContext, super.getProperty(key));

	}

	protected String getPropertyWithVariableReplacement(RenderContext renderContext, PropertyKey<String> key) throws FrameworkException {

		HttpServletRequest request = renderContext.getRequest();

		if (securityContext.getRequest() == null) {

			securityContext.setRequest(request);
		}

		return replaceVariables(securityContext, renderContext, super.getProperty(key));

	}

	protected String replaceVariables(SecurityContext securityContext, RenderContext renderContext, String rawValue)
		throws FrameworkException {

		String value = null;

		if ((rawValue != null) && (rawValue instanceof String)) {

			value = (String) rawValue;

			// re-use matcher from previous calls
			Matcher matcher = threadLocalTemplateMatcher.get();

			matcher.reset(value);

			while (matcher.find()) {

				String group  = matcher.group();
				String source = group.substring(2, group.length() - 1);

				// fetch referenced property
				String partValue = extractFunctions(securityContext, renderContext, source);

				if (partValue != null) {

					value = value.replace(group, partValue);
				}

			}

		}

		return value;

	}

	protected String extractFunctions(SecurityContext securityContext, RenderContext renderContext, String source)
		throws FrameworkException {

		AbstractNode viewComponent = renderContext.getViewComponent();
		String pageId = renderContext.getPageId();
		
		// re-use matcher from previous calls
		Matcher functionMatcher = threadLocalFunctionMatcher.get();

		functionMatcher.reset(source);

		if (functionMatcher.matches()) {

			String viewComponentId            = viewComponent != null
				? viewComponent.getProperty(AbstractNode.uuid)
				: null;
			String functionGroup              = functionMatcher.group(1);
			String parameter                  = functionMatcher.group(2);
			String functionName               = functionGroup.substring(0, functionGroup.length());
			Function<String, String> function = functions.get(functionName);

			if (function != null) {

				// store thread "state" in function
				function.setDataId(viewComponentId);
				function.setPageId(pageId);

				if (parameter.contains(",")) {

					String[] parameters = split(parameter);
					String[] results    = new String[parameters.length];

					// collect results from comma-separated function parameter
					for (int i = 0; i < parameters.length; i++) {

						results[i] = extractFunctions(securityContext, renderContext, StringUtils.strip(parameters[i]));
					}

					return function.apply(results);

				} else {

					String result = extractFunctions(securityContext, renderContext, StringUtils.strip(parameter));

					return function.apply(new String[] { result });

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
	 * Search string is taken from SecurityContext's http request
	 * Given displayPage is substracted from search result (we don't want to return search result page in search results)
	 *
	 * @param securityContext
	 * @param displayPage
	 * @return
	 */
	protected Set<Page> getResultPages(final SecurityContext securityContext, final Page displayPage) {

		HttpServletRequest request = securityContext.getRequest();
		String search              = request.getParameter("search");

		if ((request == null) || StringUtils.isEmpty(search)) {

			return Collections.EMPTY_SET;
		}

		if (request != null) {

			resultPages = (Set<Page>) request.getAttribute("searchResults");

			if ((resultPages != null) && !resultPages.isEmpty()) {

				return resultPages;
			}

		}

		if (resultPages == null) {

			resultPages = new HashSet<Page>();
		}

		// fetch search results
		// List<GraphObject> results              = ((SearchResultView) startNode).getGraphObjects(request);
		List<SearchAttribute> searchAttributes = new LinkedList<SearchAttribute>();

		searchAttributes.add(Search.andContent(search));
		searchAttributes.add(Search.andExactType(Content.class.getSimpleName()));

		try {

			Result<Content> result = Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(searchAttributes);
			for (Content content : result.getResults()) {

				resultPages.addAll(PageHelper.getPages(securityContext, content));
			}

			// Remove result page itself
			resultPages.remove((Page) displayPage);

		} catch (FrameworkException fe) {

			logger.log(Level.WARNING, "Error while searching in content", fe);

		}

		return resultPages;

	}

	protected String convertValueForHtml(java.lang.Object value) {

		if (value != null) {

			// TODO: do more intelligent conversion here
			return value.toString();
		}

		return null;

	}

	protected String[] split(String source) {

		ArrayList<String> tokens   = new ArrayList<String>(20);
		boolean inDoubleQuotes     = false;
		boolean inSingleQuotes     = false;
		int len                    = source.length();
		int level                  = 0;
		StringBuilder currentToken = new StringBuilder(len);

		for (int i = 0; i < len; i++) {

			char c = source.charAt(i);

			// do not strip away separators in nested functions!
			if ((level != 0) || (c != ',')) {

				currentToken.append(c);
			}

			switch (c) {

				case '(' :
					level++;

					break;

				case ')' :
					level--;

					break;

				case '"' :
					if (inDoubleQuotes) {

						inDoubleQuotes = false;

						level--;

					} else {

						inDoubleQuotes = true;

						level++;

					}

					break;

				case '\'' :
					if (inSingleQuotes) {

						inSingleQuotes = false;

						level--;

					} else {

						inSingleQuotes = true;

						level++;

					}

					break;

				case ',' :
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

	// ----- interface org.w3c.dom.Node -----
	@Override
	public Node getParentNode() {
		return getProperty(parent);
	}

	@Override
	public NodeList getChildNodes() {
		
		StructrNodeList nodeList = new StructrNodeList();
		
		for (AbstractRelationship rel : getChildRelationships()) {
			
			AbstractNode node = rel.getEndNode();
			
			if (node instanceof DOMNode) {
				nodeList.add((DOMNode)node);
			}
		}

		return nodeList;
	}

	@Override
	public Node getFirstChild() {
		
		NodeList _children = getChildNodes();
		
		if (_children.getLength() > 0) {
			
			return _children.item(0);
		}
		
		return null;
	}

	@Override
	public Node getLastChild() {
		
		NodeList _children = getChildNodes();
		int length         = _children.getLength();
		
		if (length > 0) {
			
			return _children.item(length - 1);
		}
		
		return null;
	}

	@Override
	public Node getPreviousSibling() {

		checkReadAccess();
		
		Node _parent = getParentNode();
		if (_parent != null) {
			
			NodeList _children = _parent.getChildNodes();
			int pos = getPositionInParent();
			
			if (pos > 0 && pos < _children.getLength()) {
				
				return _children.item(pos - 1);
			}
		}
		
		return null;
	}

	@Override
	public Node getNextSibling() {

		checkReadAccess();
		
		Node _parent = getParentNode();
		if (_parent != null) {
			
			NodeList _children = _parent.getChildNodes();
			int pos = getPositionInParent();
			
			if (pos >= 0 && pos < _children.getLength() - 1) {
				
				return _children.item(pos + 1);
			}
		}
		
		return null;
	}

	@Override
	public Document getOwnerDocument() {
		return getProperty(page);
	}

	@Override
	public Node insertBefore(final Node newChild, final Node refChild) throws DOMException {

		checkWriteAccess();
		
		checkSameDocument(newChild);
		checkSameDocument(refChild);
		
		checkHierarchy(newChild);
		checkHierarchy(refChild);
		
		final DOMNode newNode = (DOMNode)newChild;
		final DOMNode refNode = (DOMNode)refChild;
		
		// insert children from document fragment
		final StructrNodeList nodesToAdd = new StructrNodeList();
		
		if (newNode instanceof DocumentFragment) {
			
			NodeList nodeList = ((DocumentFragment)newChild).getChildNodes();
			int len           = nodeList.getLength();
			
			for (int i=0; i<len; i++) {
				
				DOMNode node = (DOMNode)nodeList.item(i);
				nodesToAdd.add(node);
			}
			
		} else {

			nodesToAdd.add((DOMNode)newChild);
		}
		
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					List<AbstractRelationship> rels = getChildRelationships();
					int position                    = 0;

					for (AbstractRelationship rel : rels) {

						AbstractNode node = rel.getEndNode();
						if (node instanceof DOMNode) {

							DOMNode domNode = (DOMNode)node;

							if (domNode.isSameNode(refNode)) {

								int addCount           = nodesToAdd.getLength();
								PropertyMap properties = new PropertyMap();

								for (int i=0; i<addCount; i++) {

									Node toAdd = nodesToAdd.item(i);

									if (toAdd instanceof DOMNode) {

										properties.clear();
										properties.put(ChildrenRelationship.position, position);
										DOMNode.children.createRelationship(securityContext, DOMNode.this, (DOMNode)toAdd, properties);						

										position++;
									}

								}
							}
							
							rel.setProperty(ChildrenRelationship.position, position);

							position++;
						}
					}
					
					return null;
				}

			});

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
		
		final DOMNode newNode = (DOMNode)newChild;
		final DOMNode oldNode = (DOMNode)oldChild;
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					if (newChild instanceof DocumentFragment) {
						
						List<AbstractRelationship> rels = getChildRelationships();
						int position                    = 0;
						
						for (AbstractRelationship rel : rels) {
							
							AbstractNode node = rel.getEndNode();
							if (node instanceof DOMNode) {
								
								DOMNode domNode = (DOMNode)node;
								
								if (domNode.isSameNode(oldNode)) {

									// remove old node
									DOMNode.children.removeRelationship(securityContext, DOMNode.this, oldNode);
									
									
									// insert children from document fragment
									NodeList fragmentChildren = newChild.getChildNodes();
									int len                   = fragmentChildren.getLength();
									PropertyMap properties    = new PropertyMap();
									
									for (int i=0; i<len; i++) {
			
										Node fragmentChild = fragmentChildren.item(i);
										
										if (fragmentChild instanceof DOMNode) {

											properties.clear();
											properties.put(ChildrenRelationship.position, position);
											DOMNode.children.createRelationship(securityContext, DOMNode.this, (DOMNode)fragmentChild, properties);						
											
											position++;
										}
										
									}
									
								} else {

									rel.setProperty(ChildrenRelationship.position, position);
								}
								
								position++;
							}
						}
						
						
					} else {

						// save old position
						int oldPosition = oldNode.getPositionInParent();

						// remove old node
						DOMNode.children.removeRelationship(securityContext, DOMNode.this, oldNode);
					
						// insert new node with position from old node
						PropertyMap properties = new PropertyMap();
						properties.put(ChildrenRelationship.position, oldPosition);
						DOMNode.children.createRelationship(securityContext, DOMNode.this, newNode, properties);
						
					}
					
					return null;
				}

			});

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());			
		}					

		return oldChild;
	}

	@Override
	public Node removeChild(final Node node) throws DOMException {

		checkWriteAccess();
		checkSameDocument(node);
		checkHierarchy(node);
		checkIsChild(node);
		
		final DOMNode otherNode = (DOMNode)node;
		
		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					DOMNode.children.removeRelationship(securityContext, DOMNode.this, otherNode);
					ensureCorrectChildPositions();
					
					return null;
				}

			});

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());			
		}					

		return node;
	}

	@Override
	public Node appendChild(final Node node) throws DOMException {

		checkWriteAccess();
		checkSameDocument(node);
		checkHierarchy(node);

		try {

			Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

				@Override
				public Object execute() throws FrameworkException {

					PropertyMap properties = new PropertyMap();
					DOMNode domNode        = (DOMNode)node;
					NodeList children      = getChildNodes();
					int childCount         = children.getLength();

					// create new relationship with position
					properties.put(ChildrenRelationship.position, childCount);
					DOMNode.children.createRelationship(securityContext, DOMNode.this, domNode, properties);

					return null;
				}

			});

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());			
		}					

		return node;
	}

	@Override
	public boolean hasChildNodes() {
		return !getProperty(children).isEmpty();
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void normalize() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isSupported(String string, String string1) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getNamespaceURI() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getPrefix() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setPrefix(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getLocalName() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasAttributes() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getBaseURI() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public short compareDocumentPosition(Node node) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getTextContent() throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setTextContent(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isSameNode(Node node) {

		if (node != null && node instanceof DOMNode) {

			String otherId = ((DOMNode)node).getProperty(GraphObject.uuid);
			String ourId   = getProperty(GraphObject.uuid);
			
			if (ourId != null && otherId != null && ourId.equals(otherId)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String lookupPrefix(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isDefaultNamespace(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String lookupNamespaceURI(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isEqualNode(Node node) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object getFeature(String string, String string1) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object setUserData(String string, Object o, UserDataHandler udh) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object getUserData(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
