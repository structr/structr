/*
 *  Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.entity.dom;


import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.Linkable;
import org.structr.core.graph.NodeService;
import org.structr.core.property.EntityProperty;
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
 * Represents a page resource
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class Page extends DOMNode implements Linkable, Document, DocumentType, DOMImplementation {

	public static final Property<Integer> version                           = new IntProperty("version");
	public static final Property<Integer> position                          = new IntProperty("position");
	public static final Property<String> contentType                        = new StringProperty("contentType");
	public static final Property<Integer> cacheForSeconds                   = new IntProperty("cacheForSeconds");
	public static final EntityProperty<Html> html                           = new EntityProperty<Html>("html", Html.class, RelType.ROOT, true);
	public static final CollectionProperty<DOMNode> elements                = new CollectionProperty<DOMNode>("elements", DOMNode.class, RelType.PAGE, Direction.INCOMING, true);
	
	public static final org.structr.common.View uiView                      = new org.structr.common.View(Page.class, PropertyView.Ui, contentType, owner, cacheForSeconds, version);
	public static final org.structr.common.View publicView                  = new org.structr.common.View(Page.class, PropertyView.Public, linkingElements, contentType, owner, cacheForSeconds, version);

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.keyword.name(), uiView.properties());

	}

	//~--- methods --------------------------------------------------------

	/**
	 * Creates a new Page entity with the given name in the database.
	 * 
	 * @param securityContext the security context to use
	 * @param name the name of the new page, defaults to "page" if not set
	 * 
	 * @return the new page
	 * @throws FrameworkException 
	 */
	public static Page createNewPage(SecurityContext securityContext, String name) throws FrameworkException {
		
		final CreateNodeCommand cmd  = Services.command(securityContext, CreateNodeCommand.class);
		final PropertyMap properties = new PropertyMap();

		properties.put(AbstractNode.name, name != null ? name : "page");
		properties.put(AbstractNode.type, Page.class.getSimpleName());
		properties.put(AbstractNode.visibleToAuthenticatedUsers, true);
		properties.put(Page.contentType, "text/html");
		
		return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Page>() {

			@Override
			public Page execute() throws FrameworkException {

				return (Page)cmd.execute(properties);
			}		
		});
	}
	
	@Override
	protected void checkHierarchy(Node otherNode) throws DOMException {
		
		// verify that this document has only one document element
		if (getProperty(html) != null) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT);
		}
		
		if (!(otherNode instanceof Html)) {
			
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT);			
		}
		
		super.checkHierarchy(otherNode);
	}

	@Override
	public Node appendChild(final Node child) throws DOMException {
		
		Node node = super.appendChild(child);

		try {

			// create additional ROOT relationship
			Page.html.createRelationship(securityContext, this, (DOMNode)child);
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());			
		}
		
		return node;
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
					
					// create relationship from page to new text element
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
					
					// create relationship from page to new text element
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
					
					// create relationship from page to new text element
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
					
					// create relationship from page to new text element
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
					
					// create relationship from page to new text element
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
	public Attr createAttribute(String string) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public EntityReference createEntityReference(String string) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Node importNode(Node node, boolean bln) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Element createElementNS(String string, String string1) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Attr createAttributeNS(String string, String string1) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Node adoptNode(Node node) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public void normalizeDocument() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Node renameNode(Node node, String string, String string1) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

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
	public DocumentType getDoctype() {

		return this;
	}

	@Override
	public DOMImplementation getImplementation() {

		return this;
	}

	@Override
	public Element getDocumentElement() {
		return getProperty(html);
	}

	@Override
	public Element getElementById(String string) {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public String getInputEncoding() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public String getXmlEncoding() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public boolean getXmlStandalone() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public String getXmlVersion() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public boolean getStrictErrorChecking() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public String getDocumentURI() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public DOMConfiguration getDomConfig() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public NamedNodeMap getEntities() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public NamedNodeMap getNotations() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public String getPublicId() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public String getSystemId() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public String getInternalSubset() {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		HttpServletRequest request = securityContext.getRequest();
		Condition condition        = renderContext.getCondition();
		
		renderContext.setPage(this);

//              if (!edit) {
//
//                      Date nodeLastMod = startNode.getLastModifiedDate();
//
//                      if ((lastModified == null) || nodeLastMod.after(lastModified)) {
//
//                              lastModified = nodeLastMod;
//
//                      }
//
//              }
		
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

		throw new UnsupportedOperationException("Not supported yet.");

	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setXmlStandalone(boolean bln) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public void setXmlVersion(String string) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public void setStrictErrorChecking(boolean bln) {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public void setDocumentURI(String string) {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	// ----- interface org.w3c.dom.Node -----
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
	public NodeList getElementsByTagName(String tagName) {
		
		StructrNodeList results = new StructrNodeList();

		collectElementsByTagName(this, results, tagName, 0);
		
		return results;
	}

	@Override
	public NodeList getElementsByTagNameNS(String string, String string1) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	// ----- private methods -----
	private void collectElementsByTagName(DOMNode startNode, StructrNodeList results, String tagName, int depth) {
		
		if (startNode instanceof DOMElement) {
			
			if (tagName.equals(startNode.getProperty(DOMElement.tag))) {
				results.add(startNode);
			}
			
			NodeList _children = startNode.getChildNodes();
			int len            = _children.getLength();
			
			for (int i=0; i<len; i++) {
				
				DOMNode child = (DOMNode)_children.item(i);
				
				collectElementsByTagName(child, results, tagName, depth+1);
			}
		}
	}
}
