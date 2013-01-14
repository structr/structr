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



package org.structr.web.entity;


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
import org.structr.web.entity.html.HtmlElement;

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
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.neo4j.graphdb.Direction;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.CollectionProperty;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a page resource
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class Page extends HtmlElement implements Linkable, Document, DocumentType, DOMImplementation {

	public static final Property<String> contentType             = new StringProperty("contentType");
	public static final Property<Integer> cacheForSeconds        = new IntProperty("cacheForSeconds");
	public static final EntityProperty<Html> html                = new EntityProperty<Html>("html", Html.class, RelType.CONTAINS, true);
	public static final CollectionProperty<HtmlElement> elements = new CollectionProperty<HtmlElement>("elements", HtmlElement.class, RelType.PAGE, Direction.INCOMING, true);
	
	public static final org.structr.common.View uiView           = new org.structr.common.View(Page.class, PropertyView.Ui, contentType, owner, cacheForSeconds, version);
	public static final org.structr.common.View publicView       = new org.structr.common.View(Page.class, PropertyView.Public, linkingElements, contentType, owner, cacheForSeconds, version);

	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Page.class, NodeService.NodeIndex.keyword.name(), uiView.properties());

	}

	//~--- methods --------------------------------------------------------

	public void increaseVersion() throws FrameworkException {

		Long _version = getProperty(Page.version);

		if (_version == null) {

			setProperty(Page.version, 1L);
		} else {

			setProperty(Page.version, _version + 1);
		}

	}

	@Override
	public Element createElement(String tag) throws DOMException {

		final String elementType = EntityContext.normalizeEntityName(tag);
		
		try {
			return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<HtmlElement>() {

				@Override
				public HtmlElement execute() throws FrameworkException {
					
					// create new content element
					HtmlElement element = (HtmlElement)Services.command(securityContext, CreateNodeCommand.class).execute(
						new NodeAttribute(AbstractNode.type, elementType)
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

		throw new UnsupportedOperationException("Not supported yet.");

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

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public CDATASection createCDATASection(String string) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

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

		return this;

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
		List<AbstractRelationship> rels = Component.getChildRelationships(request, this);

		for (AbstractRelationship rel : rels) {

			if ((condition == null) || ((condition != null) && condition.isSatisfied(request, rel))) {

				HtmlElement subNode = (HtmlElement) rel.getEndNode();

				if (subNode.isNotDeleted() && subNode.isNotDeleted()) {

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

}
