/*
 * Copyright (C) 2010-2024 Structr GmbH
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
package org.structr.web.traits.wrappers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.structr.api.Predicate;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.traits.Traits;
import org.structr.core.traits.wrappers.AbstractTraitWrapper;
import org.structr.schema.NonIndexed;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.DOMAttribute;
import org.structr.web.entity.dom.DOMElement;
import org.structr.web.entity.dom.DOMNodeList;
import org.structr.web.entity.dom.Page;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.traits.operations.GetAttributes;
import org.structr.web.traits.operations.OpeningTag;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class DOMElementTraitWrapper extends AbstractTraitWrapper<NodeInterface> implements DOMElement, NamedNodeMap, NonIndexed {

	private static final Gson gson = new GsonBuilder().create();

	/*

		// view configuration
		type.addViewProperty(PropertyView.Public, "isDOMNode");
		type.addViewProperty(PropertyView.Public, "pageId");
		type.addViewProperty(PropertyView.Public, "parent");
		type.addViewProperty(PropertyView.Public, "sharedComponentId");
		type.addViewProperty(PropertyView.Public, "syncedNodesIds");
		type.addViewProperty(PropertyView.Public, "name");
		type.addViewProperty(PropertyView.Public, "children");
		type.addViewProperty(PropertyView.Public, "dataKey");
		type.addViewProperty(PropertyView.Public, "cypherQuery");
		type.addViewProperty(PropertyView.Public, "xpathQuery");
		type.addViewProperty(PropertyView.Public, "restQuery");
		type.addViewProperty(PropertyView.Public, "functionQuery");

		type.addViewProperty(PropertyView.Ui, "hideOnDetail");
		type.addViewProperty(PropertyView.Ui, "hideOnIndex");
		type.addViewProperty(PropertyView.Ui, "sharedComponentConfiguration");
		type.addViewProperty(PropertyView.Ui, "isDOMNode");
		type.addViewProperty(PropertyView.Ui, "pageId");
		type.addViewProperty(PropertyView.Ui, "parent");
		type.addViewProperty(PropertyView.Ui, "sharedComponentId");
		type.addViewProperty(PropertyView.Ui, "syncedNodesIds");
		type.addViewProperty(PropertyView.Ui, "data-structr-id");
		type.addViewProperty(PropertyView.Ui, "renderDetails");
		type.addViewProperty(PropertyView.Ui, "children");
		type.addViewProperty(PropertyView.Ui, "childrenIds");
		type.addViewProperty(PropertyView.Ui, "showForLocales");
		type.addViewProperty(PropertyView.Ui, "hideForLocales");
		type.addViewProperty(PropertyView.Ui, "showConditions");
		type.addViewProperty(PropertyView.Ui, "hideConditions");
		type.addViewProperty(PropertyView.Ui, "dataKey");
		type.addViewProperty(PropertyView.Ui, "cypherQuery");
		type.addViewProperty(PropertyView.Ui, "xpathQuery");
		type.addViewProperty(PropertyView.Ui, "restQuery");
		type.addViewProperty(PropertyView.Ui, "functionQuery");

		final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
		if (licenseManager == null || licenseManager.isModuleLicensed("api-builder")) {

			type.addViewProperty(PropertyView.Public, "flow");
			type.addViewProperty(PropertyView.Ui, "flow");
		}

	}}

	String getTag();
	String getOffsetAttributeName(final String name, final int offset);

	void openingTag(final AsyncBuffer out, final String tag, final EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException;

	Property[] getHtmlAttributes();
	List<String> getHtmlAttributeNames();
	String getEventMapping();
	 */

	public DOMElementTraitWrapper(final Traits traits, final NodeInterface node) {
		super(traits, node);
	}

	// ----- public methods -----
	@Override
	public String getTag() {
		return wrappedObject.getProperty(traits.key("tag"));
	}

	@Override
	public String getEventMapping() {
		return wrappedObject.getProperty(traits.key("eventMapping"));
	}

	@Override
	public String getRenderingMode() {
		return wrappedObject.getProperty(traits.key("data-structr-rendering-mode"));
	}

	@Override
	public String getDelayOrInterval() {
		return wrappedObject.getProperty(traits.key("data-structr-delay-or-interval"));
	}

	@Override
	public String getDataReloadTarget() {
		return wrappedObject.getProperty(traits.key("data-structr-reload-target"));
	}

	public Iterable<PropertyKey> getHtmlAttributes() {
		return traits.getMethod(GetAttributes.class).getHtmlAttributes(this);
	}

	public List<String> getHtmlAttributeNames() {
		return traits.getMethod(GetAttributes.class).getHtmlAttributeNames(this);
	}

	@Override
	public void openingTag(AsyncBuffer out, String tag, RenderContext.EditMode editMode, RenderContext renderContext, int depth) throws FrameworkException {
		traits.getMethod(OpeningTag.class).openingTag(this, out, tag, editMode, renderContext, depth);
	}

	@Override
	public boolean isInsertable() {
		return wrappedObject.getProperty(traits.key("data-structr-insert"));
	}

	@Override
	public boolean isFromWidget() {
		return wrappedObject.getProperty(traits.key("data-structr-from-widget"));
	}

	@Override
	public Iterable<ActionMapping> getTriggeredActions() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("triggeredActions");

		return Iterables.map(n -> n.as(ActionMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public Iterable<ParameterMapping> getParameterMappings() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("parameterMappings");

		return Iterables.map(n -> n.as(ParameterMapping.class), wrappedObject.getProperty(key));
	}

	@Override
	public Map<String, Object> getMappedEvents() {

		final String mapping = getEventMapping();
		if (mapping != null) {

			return gson.fromJson(mapping, Map.class);
		}

		return null;
	}

	@Override
	public String getOffsetAttributeName(final String name, final int offset) {

		int namePosition = -1;
		int index = 0;

		final List<String> keys = Iterables.toList(getWrappedNode().getNode().getPropertyKeys());

		Collections.sort(keys);

		List<String> names = new ArrayList<>(10);

		for (String key : keys) {

			// use html properties only
			if (key.startsWith(PropertyView.Html)) {

				String htmlName = key.substring(HtmlPrefixLength);

				if (name.equals(htmlName)) {

					namePosition = index;
				}

				names.add(htmlName);

				index++;
			}
		}

		int offsetIndex = namePosition + offset;
		if (offsetIndex >= 0 && offsetIndex < names.size()) {

			return names.get(offsetIndex);
		}

		return null;
	}

	@Override
	public Node doImport(final Page newPage) throws DOMException {

		final DOMElement newElement = (DOMElement) newPage.createElement(getTag());

		// copy attributes
		for (String _name : getHtmlAttributeNames()) {

			Attr attr = getAttributeNode(_name);
			if (attr.getSpecified()) {

				newElement.setAttribute(attr.getName(), attr.getValue());
			}
		}

		return newElement;
	}

	@Override
	public void setIdAttribute(final String idString, final boolean isId) throws DOMException {

		checkWriteAccess();

		try {

			wrappedObject.setProperty(traits.key("_html_id"), idString);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}
	}

	@Override
	public String getAttribute(final String name) {

		final HtmlProperty htmlProperty = findOrCreateAttributeKey(name);

		return htmlProperty.getProperty(getSecurityContext(), wrappedObject, true);
	}

	@Override
	public void setAttribute(final String name, final String value) throws DOMException {

		try {
			final HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(getSecurityContext(), wrappedObject, value);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	@Override
	public void removeAttribute(final String name) throws DOMException {

		try {
			final HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(getSecurityContext(), wrappedObject, null);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	@Override
	public Attr getAttributeNode(final String name) {

		final HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
		final String value        = htmlProperty.getProperty(getSecurityContext(), wrappedObject, true);

		if (value != null) {

			boolean explicitlySpecified = true;
			boolean isId = false;

			if (value.equals(htmlProperty.defaultValue())) {
				explicitlySpecified = false;
			}

			return new DOMAttribute((Page) getOwnerDocument(), this, name, value, explicitlySpecified, isId);
		}

		return null;
	}

	@Override
	public Attr setAttributeNode(final Attr attr) throws DOMException {

		// save existing attribute node
		final Attr attribute = getAttributeNode(attr.getName());

		// set value
		setAttribute(attr.getName(), attr.getValue());

		// set parent of attribute node
		if (attr instanceof DOMAttribute) {
			((DOMAttribute) attr).setParent(this);
		}

		return attribute;
	}

	@Override
	public Attr removeAttributeNode(final Attr attr) throws DOMException {

		// save existing attribute node
		final Attr attribute = getAttributeNode(attr.getName());

		// set value
		setAttribute(attr.getName(), null);

		return attribute;
	}

	@Override
	public NodeList getElementsByTagName(final String tagName) {

		DOMNodeList results = new DOMNodeList();

		DOMNodeTraitWrapper.collectNodesByPredicate(getSecurityContext(), this, results, new TagPredicate(tagName), 0, false);

		return results;
	}

	@Override
	public Node setNamedItem(final Node node) throws DOMException {

		if (node instanceof Attr) {
			return setAttributeNode((Attr) node);
		}

		return null;
	}

	@Override
	public Node removeNamedItem(final String name) throws DOMException {

		// save existing attribute node
		Attr attribute = getAttributeNode(name);

		// set value to null
		setAttribute(name, null);

		return attribute;
	}

	@Override
	public Node item(final int i) {

		final List<String> htmlAttributeNames = getHtmlAttributeNames();
		if (i >= 0 && i < htmlAttributeNames.size()) {

			return getAttributeNode(htmlAttributeNames.get(i));
		}

		return null;
	}

	@Override
	public int getLength() {
		return traits.getMethod(GetAttributes.class).getLength(this);
	}

	// ----- org.w3c.Node methods -----
	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getTagName() {
		return getTag();
	}

	// ----- org.w3c.dom.Element -----
	@Override
	public void setIdAttributeNS(final String namespaceURI, final String localName, final boolean isId) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported.");
	}

	@Override
	public void setIdAttributeNode(final Attr idAttr, final boolean isId) throws DOMException {
		throw new UnsupportedOperationException("Attribute nodes not supported in HTML5.");
	}

	@Override
	public String getAttributeNS(final String namespaceURI, final String localName) throws DOMException {
		return null;
	}

	@Override
	public void setAttributeNS(final String namespaceURI, final String qualifiedName, final String value) throws DOMException {
	}

	@Override
	public void removeAttributeNS(final String namespaceURI, final String localName) throws DOMException {
	}

	@Override
	public Attr getAttributeNodeNS(final String namespaceURI, final String localName) throws DOMException {
		return null;
	}

	@Override
	public Attr setAttributeNodeNS(final Attr newAttr) throws DOMException {
		return null;
	}

	@Override
	public NodeList getElementsByTagNameNS(final String namespaceURI, final String localName) throws DOMException {
		return null;
	}

	@Override
	public boolean hasAttribute(final String name) {
		return getAttribute(name) != null;
	}

	@Override
	public boolean hasAttributeNS(final String namespaceURI, final String localName) throws DOMException {
		return false;
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		return null;
	}

	// ----- org.w3c.dom.NamedNodeMap -----
	@Override
	public Node getNamedItem(String name) {
		return getAttributeNode(name);
	}

	@Override
	public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
		return null;
	}

	@Override
	public Node setNamedItemNS(Node arg) throws DOMException {
		return null;
	}

	@Override
	public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
		return null;
	}

	// ----- private static methods -----
	private static int intOrOne(final String source) {

		if (source != null) {

			try {

				return Integer.valueOf(source);

			} catch (Throwable t) {
			}
		}

		return 1;
	}

	private static String toHtmlAttributeName(final String camelCaseName) {

		final StringBuilder buf = new StringBuilder();

		camelCaseName.chars().forEach(c -> {

			if (Character.isUpperCase(c)) {

				buf.append("-");
				c = Character.toLowerCase(c);

			}

			buf.append(Character.toString(c));
		});

		return buf.toString();
	}

	@Override
	public boolean isManualReloadTarget() {
		return false;
	}

	@Override
	public Iterable<DOMElement> getReloadSources() {

		final PropertyKey<Iterable<NodeInterface>> key = traits.key("reloadSources");

		return Iterables.map(n -> n.as(DOMElement.class), wrappedObject.getProperty(key));
	}

	public class TagPredicate implements Predicate<Node> {

		private String tagName = null;

		public TagPredicate(String tagName) {
			this.tagName = tagName;
		}

		@Override
		public boolean accept(Node obj) {

			if (obj instanceof DOMElement) {

				DOMElement elem = (DOMElement)obj;

				if (tagName.equals(elem.getTag())) {
					return true;
				}
			}

			return false;
		}
	}

	private HtmlProperty findOrCreateAttributeKey(final String name) {

		// try to find native html property defined in DOMElement or one of its subclasses
		final PropertyKey key = traits.key("name");;

		if (key != null && key instanceof HtmlProperty) {

			return (HtmlProperty) key;

		} else {

			// create synthetic HtmlProperty
			final HtmlProperty htmlProperty = new HtmlProperty(name);

			htmlProperty.setDeclaringTrait(traits.get("DOMElement"));

			return htmlProperty;
		}
	}
}