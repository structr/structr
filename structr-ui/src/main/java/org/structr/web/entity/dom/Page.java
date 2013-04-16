/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;


import org.structr.common.PropertyView;
import org.structr.web.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.web.entity.Linkable;
import org.structr.core.graph.NodeService;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.html.Html;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

//~--- JDK imports ------------------------------------------------------------


import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.Direction;
import org.structr.core.Predicate;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.CollectionProperty;
import org.structr.core.property.PropertyMap;
import org.structr.web.entity.Condition;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a ownerDocument resource
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class Page extends DOMNode implements Linkable, Document, DOMImplementation {

	public static final Property<Integer> version                           = new IntProperty("version");
	public static final Property<Integer> position                          = new IntProperty("position");
	public static final Property<String> contentType                        = new StringProperty("contentType");
	public static final Property<Integer> cacheForSeconds                   = new IntProperty("cacheForSeconds");
	public static final CollectionProperty<DOMNode> elements                = new CollectionProperty<DOMNode>("elements", DOMNode.class, RelType.PAGE, Direction.INCOMING, true);
	
	public static final org.structr.common.View uiView                      = new org.structr.common.View(Page.class, PropertyView.Ui, contentType, ownerId, cacheForSeconds, version);
	public static final org.structr.common.View publicView                  = new org.structr.common.View(Page.class, PropertyView.Public, linkingElements, contentType, ownerId, cacheForSeconds, version);

	private Html5DocumentType docTypeNode                                   = null;
	
	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.keyword.name(), uiView.properties());

	}

	public Page() {
		
		docTypeNode = new Html5DocumentType(this);
	}
	
	//~--- methods --------------------------------------------------------

	/**
	 * Creates a new Page entity with the given name in the database.
	 * 
	 * @param securityContext the security context to use
	 * @param name the name of the new ownerDocument, defaults to "ownerDocument" if not set
	 * 
	 * @return the new ownerDocument
	 * @throws FrameworkException 
	 */
	public static Page createNewPage(SecurityContext securityContext, String name) throws FrameworkException {
		
		final CreateNodeCommand cmd  = Services.command(securityContext, CreateNodeCommand.class);
		final PropertyMap properties = new PropertyMap();

		properties.put(AbstractNode.name, name != null ? name : "page");
		properties.put(AbstractNode.type, Page.class.getSimpleName());
		properties.put(AbstractNode.visibleToAuthenticatedUsers, true);
		properties.put(AbstractNode.visibleToPublicUsers, true);
		properties.put(Page.contentType, "text/html");
	
		Page newPage = Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Page>() {

			@Override
			public Page execute() throws FrameworkException {

				return (Page)cmd.execute(properties);
			}		
		});
		
		//newPage.appendChild(newPage.createD)
		
		return newPage;
	}
	
	@Override
	protected void checkHierarchy(Node otherNode) throws DOMException {
		
		// verify that this document has only one document element
		if (getDocumentElement() != null) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT);
		}
		
		if (!(otherNode instanceof Html)) {
			
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT);			
		}
		
		super.checkHierarchy(otherNode);
	}

	@Override
	public boolean hasChildNodes() {
		return true;
	}
	
	@Override
	public NodeList getChildNodes() {
		
		DOMNodeList _children = new DOMNodeList();
		
		_children.add(docTypeNode);
		_children.addAll(super.getChildNodes());
		
		return _children;
	}
	
	@Override
	public Node getFirstChild() {
		return docTypeNode;
	}

	public void increaseVersion() throws FrameworkException {

		Integer _version = getProperty(Page.version);

		if (_version == null) {

			setProperty(Page.version, 1);
			
		} else {

			setProperty(Page.version, _version + 1);
		}

	}

	@Override
	public Element createElement(final String tag) throws DOMException {

		final String elementType = EntityContext.normalizeEntityName(tag);
		
		try {
			return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<DOMElement>() {

				@Override
				public DOMElement execute() throws FrameworkException {
					
					// create new content element
					DOMElement element = (DOMElement)Services.command(securityContext, CreateNodeCommand.class).execute(
						new NodeAttribute(AbstractNode.type, elementType),
						new NodeAttribute(DOMElement.tag, tag)
					);
					
					// create relationship from ownerDocument to new text element
					Page.elements.createRelationship(securityContext, Page.this, element);
					
					return element;
				}
			});
			
		} catch (FrameworkException fex) {
			
			// FIXME: what to do with the exception here?
			fex.printStackTrace();
		}
		
		return null;
	}

	@Override
	public DocumentFragment createDocumentFragment() {

		try {
			return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<DocumentFragment>() {

				@Override
				public DocumentFragment execute() throws FrameworkException {
					
					// create new content element
					org.structr.web.entity.dom.DocumentFragment fragment = (org.structr.web.entity.dom.DocumentFragment)Services.command(securityContext, CreateNodeCommand.class).execute(
						new NodeAttribute(AbstractNode.type, org.structr.web.entity.dom.DocumentFragment.class.getSimpleName())
					);
					
					// create relationship from ownerDocument to new text element
					Page.elements.createRelationship(securityContext, Page.this, fragment);
					
					return fragment;
				}
			});
			
		} catch (FrameworkException fex) {
			
			// FIXME: what to do with the exception here?
			fex.printStackTrace();
		}
		
		return null;

	}

	@Override
	public Text createTextNode(final String text) {

		try {
			return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Content>() {

				@Override
				public Content execute() throws FrameworkException {
					
					// create new content element
					Content content = (Content)Services.command(securityContext, CreateNodeCommand.class).execute(
						new NodeAttribute(AbstractNode.type, Content.class.getSimpleName()),
						new NodeAttribute(Content.content,   text)
					);
					
					// create relationship from ownerDocument to new text element
					Page.elements.createRelationship(securityContext, Page.this, content);
					
					return content;
				}
			});
			
		} catch (FrameworkException fex) {
			
			// FIXME: what to do with the exception here?
			fex.printStackTrace();
		}
		
		return null;
	}

	@Override
	public Comment createComment(String string) {

		try {
			return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Comment>() {

				@Override
				public org.structr.web.entity.dom.Comment execute() throws FrameworkException {
					
					// create new content element
					org.structr.web.entity.dom.Comment content = (org.structr.web.entity.dom.Comment)Services.command(securityContext, CreateNodeCommand.class).execute(
						new NodeAttribute(AbstractNode.type, org.structr.web.entity.dom.Comment.class.getSimpleName())
					);
					
					// create relationship from ownerDocument to new text element
					Page.elements.createRelationship(securityContext, Page.this, content);
					
					return content;
				}
			});
			
		} catch (FrameworkException fex) {
			
			// FIXME: what to do with the exception here?
			fex.printStackTrace();
		}
		
		return null;
	}

	@Override
	public CDATASection createCDATASection(String string) throws DOMException {

		try {
			return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Cdata>() {

				@Override
				public Cdata execute() throws FrameworkException {
					
					// create new content element
					Cdata content = (Cdata)Services.command(securityContext, CreateNodeCommand.class).execute(
						new NodeAttribute(AbstractNode.type, Cdata.class.getSimpleName())
					);
					
					// create relationship from ownerDocument to new text element
					Page.elements.createRelationship(securityContext, Page.this, content);
					
					return content;
				}
			});
			
		} catch (FrameworkException fex) {
			
			// FIXME: what to do with the exception here?
			fex.printStackTrace();
		}
		
		return null;
	}

	@Override
	public ProcessingInstruction createProcessingInstruction(String string, String string1) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Attr createAttribute(String name) throws DOMException {
		return new DOMAttribute(this, null, name, null);
	}

	@Override
	public EntityReference createEntityReference(String string) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Element createElementNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported");
	}

	@Override
	public Attr createAttributeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported");
	}

	@Override
	public Node importNode(final Node node, final boolean deep) throws DOMException {
		return importNode(node, deep, true);
	}
	
	private Node importNode(final Node node, final boolean deep, final boolean removeParentFromSourceNode) throws DOMException {
		
		if (node instanceof DOMNode) {
	
			final DOMNode domNode = (DOMNode)node;

			try {

				return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Node>() {

					@Override
					public Node execute() throws FrameworkException {

						// step 1: use type-specific import impl.
						Node importedNode = domNode.doImport(Page.this);

						// step 2: do recursive import?
						if (deep && domNode.hasChildNodes()) {

							// FIXME: is it really a good idea to do the
							// recursion inside of a transaction?

							Node child = domNode.getFirstChild();
							while (child != null) {

								// do not remove parent for child nodes
								importNode(child, deep, false);
								child = child.getNextSibling();
							}
							
						}

						// step 3: remove node from its current parent
						// (Note that this step needs to be done last in
						// (order for the child to be able to find its
						// siblings.)
						if (removeParentFromSourceNode) {

							// only do this for the actual source node, do not remove
							// child nodes from its parents
							Node _parent = domNode.getParentNode();
							if (_parent != null) {
								_parent.removeChild(domNode);
							}
						}
						
						return importedNode;
					}
				});

			} catch (FrameworkException fex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());
			}
		}
		
		return null;	
	}

	@Override
	public Node adoptNode(Node node) throws DOMException {
		return adoptNode(node, true);
	}
	
	private Node adoptNode(final Node node, final boolean removeParentFromSourceNode) throws DOMException {
		
		if (node instanceof DOMNode) {
	
			final DOMNode domNode = (DOMNode)node;

			try {

				return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Node>() {

					@Override
					public Node execute() throws FrameworkException {

						// step 1: use type-specific adopt impl.
						Node adoptedNode = domNode.doAdopt(Page.this);

						// step 2: do recursive import?
						if (domNode.hasChildNodes()) {

							// FIXME: is it really a good idea to do the
							// recursion inside of a transaction?

							Node child = domNode.getFirstChild();
							while (child != null) {

								// do not remove parent for child nodes
								adoptNode(child, false);
								child = child.getNextSibling();
							}
							
						}
						
						// step 3: remove node from its current parent
						
						// (Note that this step needs to be done last in
						// (order for the child to be able to find its
						// siblings.)
						if (removeParentFromSourceNode) {

							// only do this for the actual source node, do not remove
							// child nodes from its parents
							Node _parent = domNode.getParentNode();
							if (_parent != null) {
								_parent.removeChild(domNode);
							}
						}

						return adoptedNode;
					}
				});

			} catch (FrameworkException fex) {

				throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());
			}
		}
		
		return null;	
	}

	@Override
	public void normalizeDocument() {
		normalize();
	}

	@Override
	public Node renameNode(Node node, String string, String string1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_RENAME);
	}

	@Override
	public DocumentType createDocumentType(String string, String string1, String string2) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Document createDocument(String string, String string1, DocumentType dt) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public short getNodeType() {

		return Element.DOCUMENT_NODE;
	}

	@Override
	public DOMImplementation getImplementation() {

		return this;
	}

	@Override
	public Element getDocumentElement() {
		return (Element)super.getFirstChild();
	}

	@Override
	public Element getElementById(final String id) {

		DOMNodeList results = new DOMNodeList();

		collectNodesByPredicate(this, results, new Predicate<Node>() {

			@Override
			public boolean evaluate(SecurityContext securityContext, Node... obj) {
				
				if (obj[0] instanceof DOMElement) {
					
					DOMElement elem = (DOMElement)obj[0];
					
					if (id.equals(elem.getProperty(DOMElement._id))) {					
						return true;
					}
				}
				
				return false;
			}
			
		}, 0, true);
		
		// return first result
		if (results.getLength() == 1) {
			return (DOMElement)results.item(0);
		}
		
		return null;
	}

	@Override
	public String getInputEncoding() {
		return null;
	}

	@Override
	public String getXmlEncoding() {
		return "UTF-8";
	}

	@Override
	public boolean getXmlStandalone() {
		return true;
	}

	@Override
	public String getXmlVersion() {
		return "1.0";
	}

	@Override
	public boolean getStrictErrorChecking() {
		return true;
	}

	@Override
	public String getDocumentURI() {
		return null;
	}

	@Override
	public DOMConfiguration getDomConfig() {
		return null;
	}

	@Override
	public DocumentType getDoctype() {
		return new Html5DocumentType(this);
	}

	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		HttpServletRequest request = securityContext.getRequest();
		Condition condition        = renderContext.getCondition();
		
		renderContext.setPage(this);
		
		renderContext.getBuffer().append("<!DOCTYPE html>");

		// recursively render children
		for (AbstractRelationship rel : getChildRelationships()) {

			if ((condition == null) || ((condition != null) && condition.isSatisfied(request, rel))) {

				DOMNode subNode = (DOMNode)rel.getEndNode();

				if (subNode.isNotDeleted()) {

					subNode.render(securityContext, renderContext, depth);
				}
			}
		}
	}

	@Override
	public boolean hasFeature(String string, String string1) {
		return false;
	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setXmlStandalone(boolean bln) throws DOMException {
	}

	@Override
	public void setXmlVersion(String string) throws DOMException {
	}

	@Override
	public void setStrictErrorChecking(boolean bln) {
	}

	@Override
	public void setDocumentURI(String string) {
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getNodeName() {
		return "#document";
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String string) throws DOMException {
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public boolean hasAttributes() {
		return false;
	}

	@Override
	public NodeList getElementsByTagName(final String tagName) {
		
		DOMNodeList results = new DOMNodeList();

		collectNodesByPredicate(this, results, new Predicate<Node>() {

			@Override
			public boolean evaluate(SecurityContext securityContext, Node... obj) {
				
				if (obj[0] instanceof DOMElement) {
					
					DOMElement elem = (DOMElement)obj[0];

					if (tagName.equals(elem.getProperty(DOMElement.tag))) {					
						return true;
					}
				}
				
				return false;
			}
			
		}, 0, false);
		
		return results;
	}

	@Override
	public NodeList getElementsByTagNameNS(String string, String tagName) {
		throw new UnsupportedOperationException("Namespaces not supported.");
	}

	// ----- interface DOMAdoptable -----
	@Override
	public Node doAdopt(Page page) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_ADOPT_DOC);
	}

	// ----- interface DOMImportable -----
	@Override
	public Node doImport(Page newPage) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_IMPORT_DOC);
	}
}
