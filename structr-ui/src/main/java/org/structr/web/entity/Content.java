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

import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;

import org.structr.common.PropertyView;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.search.Search;
import org.structr.web.common.PageHelper;
import org.structr.web.entity.html.*;
import org.structr.web.validator.DynamicValidator;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.property.EntityIdProperty;
import org.structr.core.property.EntityProperty;
import org.structr.web.property.DynamicContentProperty;
import org.w3c.dom.DOMException;
import org.w3c.dom.Text;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a content container
 *
 * @author Axel Morgner
 */
public class Content extends HtmlElement implements Text {

	private static final Logger logger         = Logger.getLogger(Content.class.getName());

	public static final Property<String>               content          = new DynamicContentProperty("content");
	public static final Property<String>               contentType      = new StringProperty("contentType");
	public static final Property<Integer>              size             = new IntProperty("size");
	public static final Property<String>               dataKey          = new StringProperty("data-key");
		
	public static final EntityProperty<TypeDefinition> typeDefinition   = new EntityProperty<TypeDefinition>("typeDefinition", TypeDefinition.class, RelType.IS_A, true);
	public static final Property<String>               typeDefinitionId = new EntityIdProperty("typeDefinitionId", typeDefinition);
	
	public static final org.structr.common.View uiView = new org.structr.common.View(Content.class, PropertyView.Ui,
		content, contentType, size,  dataKey, typeDefinitionId
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(Content.class, PropertyView.Public,
		content, contentType, size, dataKey, typeDefinitionId
	);

	//~--- static initializers --------------------------------------------

	static {
		
		EntityContext.registerSearchablePropertySet(Content.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Content.class, NodeService.NodeIndex.keyword.name(),  uiView.properties());
		
		EntityContext.registerPropertyValidator(Content.class, content, new DynamicValidator(content));

	}

	//~--- methods --------------------------------------------------------

	/**
	 * Do necessary updates on all containing pages
	 *
	 * @throws FrameworkException
	 */
	private void updatePages(SecurityContext securityContext) throws FrameworkException {

		List<Page> pages = PageHelper.getPages(securityContext, this);

		for (Page page : pages) {

			page.unlockReadOnlyPropertiesOnce();
			page.increaseVersion();

		}

	}

	@Override
	public void afterModification(SecurityContext securityContext) {

		try {

			updatePages(securityContext);

		} catch (FrameworkException ex) {

			logger.log(Level.WARNING, "Updating page versions failed", ex);

		}

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public java.lang.Object getPropertyForIndexing(final PropertyKey key) {

		if (key.equals(Content.content)) {

			String value = getProperty(Content.content);

			if (value != null) {

				return Search.escapeForLucene(value);
			}

		}

		return getProperty(key);

	}

	public String getPropertyWithVariableReplacement(HttpServletRequest request, AbstractNode page, String pageId, String componentId, AbstractNode viewComponent, PropertyKey<String> key) throws FrameworkException {

		if (securityContext.getRequest() == null) {

			securityContext.setRequest(request);
		}

		return HtmlElement.replaceVariables(securityContext, page, this, pageId, componentId, viewComponent, super.getProperty(key));

	}

	public TypeDefinition getTypeDefinition() {
		return getProperty(Content.typeDefinition);
	}

	@Override
	public short getNodeType() {
		return TEXT_NODE;
	}
	
	// ----- interface org.w3c.dom.Text -----

	@Override
	public Text splitText(int i) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isElementContentWhitespace() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getWholeText() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Text replaceWholeText(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getData() throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setData(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getLength() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String substringData(int i, int i1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void appendData(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void insertData(int i, String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void deleteData(int i, int i1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void replaceData(int i, int i1, String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
