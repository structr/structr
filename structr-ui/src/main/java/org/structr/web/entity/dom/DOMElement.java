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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;


import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.EntityContext;
import org.structr.core.graph.NodeService.NodeIndex;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.StringProperty;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.RenderContext;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.collections.map.LRUMap;
import org.structr.core.Predicate;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.IntProperty;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class DOMElement extends DOMNode implements Element, NamedNodeMap {

	private static final Logger logger                            = Logger.getLogger(DOMElement.class.getName());
	private static final int HtmlPrefixLength                     = PropertyView.Html.length();
	private static final Map<String, HtmlProperty> htmlProperties = new LRUMap(200);	// use LURMap here to avoid infinite growing
	
	private DecimalFormat decimalFormat                           = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));	
	
	public static final Property<Integer> version                 = new IntProperty("version");
	public static final Property<String> tag                      = new StringProperty("tag");
	public static final Property<String> path                     = new StringProperty("path");
	
	public static final Property<String> _title                   = new HtmlProperty("title");
	public static final Property<String> _tabindex                = new HtmlProperty("tabindex");
	public static final Property<String> _style                   = new HtmlProperty("style");
	public static final Property<String> _spellcheck              = new HtmlProperty("spellcheck");
	public static final Property<String> _onwaiting               = new HtmlProperty("onwaiting");
	public static final Property<String> _onvolumechange          = new HtmlProperty("onvolumechange");
	public static final Property<String> _ontimeupdate            = new HtmlProperty("ontimeupdate");
	public static final Property<String> _onsuspend               = new HtmlProperty("onsuspend");
	public static final Property<String> _onsubmit                = new HtmlProperty("onsubmit");
	public static final Property<String> _onstalled               = new HtmlProperty("onstalled");
	public static final Property<String> _onshow                  = new HtmlProperty("onshow");
	public static final Property<String> _onselect                = new HtmlProperty("onselect");
	public static final Property<String> _onseeking               = new HtmlProperty("onseeking");
	public static final Property<String> _onseeked                = new HtmlProperty("onseeked");
	public static final Property<String> _onscroll                = new HtmlProperty("onscroll");
	public static final Property<String> _onreset                 = new HtmlProperty("onreset");
	public static final Property<String> _onreadystatechange      = new HtmlProperty("onreadystatechange");
	public static final Property<String> _onratechange            = new HtmlProperty("onratechange");
	public static final Property<String> _onprogress              = new HtmlProperty("onprogress");
	public static final Property<String> _onplaying               = new HtmlProperty("onplaying");
	public static final Property<String> _onplay                  = new HtmlProperty("onplay");
	public static final Property<String> _onpause                 = new HtmlProperty("onpause");
	public static final Property<String> _onmousewheel            = new HtmlProperty("onmousewheel");
	public static final Property<String> _onmouseup               = new HtmlProperty("onmouseup");
	public static final Property<String> _onmouseover             = new HtmlProperty("onmouseover");
	public static final Property<String> _onmouseout              = new HtmlProperty("onmouseout");
	public static final Property<String> _onmousemove             = new HtmlProperty("onmousemove");
	public static final Property<String> _onmousedown             = new HtmlProperty("onmousedown");
	public static final Property<String> _onloadstart             = new HtmlProperty("onloadstart");
	public static final Property<String> _onloadedmetadata        = new HtmlProperty("onloadedmetadata");
	public static final Property<String> _onloadeddata            = new HtmlProperty("onloadeddata");
	public static final Property<String> _onload                  = new HtmlProperty("onload");
	public static final Property<String> _onkeyup                 = new HtmlProperty("onkeyup");
	public static final Property<String> _onkeypress              = new HtmlProperty("onkeypress");
	public static final Property<String> _onkeydown               = new HtmlProperty("onkeydown");
	public static final Property<String> _oninvalid               = new HtmlProperty("oninvalid");
	public static final Property<String> _oninput                 = new HtmlProperty("oninput");
	public static final Property<String> _onfocus                 = new HtmlProperty("onfocus");
	public static final Property<String> _onerror                 = new HtmlProperty("onerror");
	public static final Property<String> _onended                 = new HtmlProperty("onended");
	public static final Property<String> _onemptied               = new HtmlProperty("onemptied");
	public static final Property<String> _ondurationchange        = new HtmlProperty("ondurationchange");
	public static final Property<String> _ondrop                  = new HtmlProperty("ondrop");
	public static final Property<String> _ondragstart             = new HtmlProperty("ondragstart");
	public static final Property<String> _ondragover              = new HtmlProperty("ondragover");
	public static final Property<String> _ondragleave             = new HtmlProperty("ondragleave");
	public static final Property<String> _ondragenter             = new HtmlProperty("ondragenter");
	public static final Property<String> _ondragend               = new HtmlProperty("ondragend");
	public static final Property<String> _ondrag                  = new HtmlProperty("ondrag");
	public static final Property<String> _ondblclick              = new HtmlProperty("ondblclick");
	public static final Property<String> _oncontextmenu           = new HtmlProperty("oncontextmenu");
	public static final Property<String> _onclick                 = new HtmlProperty("onclick");
	public static final Property<String> _onchange                = new HtmlProperty("onchange");
	public static final Property<String> _oncanplaythrough        = new HtmlProperty("oncanplaythrough");
	public static final Property<String> _oncanplay               = new HtmlProperty("oncanplay");
	public static final Property<String> _onblur                  = new HtmlProperty("onblur");
 
	// Event-handler attributes
	public static final Property<String> _onabort                 = new HtmlProperty("onabort");
	public static final Property<String> _lang                    = new HtmlProperty("lang");
	public static final Property<String> _id                      = new HtmlProperty("id");
	public static final Property<String> _hidden                  = new HtmlProperty("hidden");
	public static final Property<String> _dropzone                = new HtmlProperty("dropzone");
	public static final Property<String> _draggable               = new HtmlProperty("draggable");
	public static final Property<String> _dir                     = new HtmlProperty("dir");

	// needed for Importer
	public static final Property<String> _data                    = new HtmlProperty("data");
	public static final Property<String> _contextmenu             = new HtmlProperty("contextmenu");
	public static final Property<String> _contenteditable         = new HtmlProperty("contenteditable");
	public static final Property<String> _class                   = new HtmlProperty("class");

	// Core attributes
	public static final Property<String> _accesskey               = new HtmlProperty("accesskey");
	
	public static final org.structr.common.View publicView        = new org.structr.common.View(DOMElement.class, PropertyView.Public,
										name, tag, path, parent
	);
	
	public static final org.structr.common.View uiView            = new org.structr.common.View(DOMElement.class, PropertyView.Ui, name, tag, path, parent, children, _accesskey, _class,
										_contenteditable, _contextmenu, _dir, _draggable, _dropzone, _hidden, _id, _lang, _spellcheck, _style, _tabindex,
										_title, _onabort, _onblur, _oncanplay, _oncanplaythrough, _onchange, _onclick, _oncontextmenu, _ondblclick, _ondrag,
										_ondragend, _ondragenter, _ondragleave, _ondragover, _ondragstart, _ondrop, _ondurationchange, _onemptied, _onended,
										_onerror, _onfocus, _oninput, _oninvalid, _onkeydown, _onkeypress, _onkeyup, _onload, _onloadeddata,
										_onloadedmetadata, _onloadstart, _onmousedown, _onmousemove, _onmouseout, _onmouseover, _onmouseup, _onmousewheel,
										_onpause, _onplay, _onplaying, _onprogress, _onratechange, _onreadystatechange, _onreset, _onscroll, _onseeked,
										_onseeking, _onselect, _onshow, _onstalled, _onsubmit, _onsuspend, _ontimeupdate, _onvolumechange, _onwaiting
	);
	
	public static final org.structr.common.View htmlView          = new org.structr.common.View(DOMElement.class, PropertyView.Html, _accesskey, _class, _contenteditable, _contextmenu, _dir,
										_draggable, _dropzone, _hidden, _id, _lang, _spellcheck, _style, _tabindex, _title, _onabort, _onblur, _oncanplay,
										_oncanplaythrough, _onchange, _onclick, _oncontextmenu, _ondblclick, _ondrag, _ondragend, _ondragenter, _ondragleave,
										_ondragover, _ondragstart, _ondrop, _ondurationchange, _onemptied, _onended, _onerror, _onfocus, _oninput, _oninvalid,
										_onkeydown, _onkeypress, _onkeyup, _onload, _onloadeddata, _onloadedmetadata, _onloadstart, _onmousedown, _onmousemove,
										_onmouseout, _onmouseover, _onmouseup, _onmousewheel, _onpause, _onplay, _onplaying, _onprogress, _onratechange,
										_onreadystatechange, _onreset, _onscroll, _onseeked, _onseeking, _onselect, _onshow, _onstalled, _onsubmit, _onsuspend,
										_ontimeupdate, _onvolumechange, _onwaiting
	);

	static {
		
		EntityContext.registerSearchablePropertySet(DOMElement.class, NodeIndex.fulltext.name(), publicView.properties());
		EntityContext.registerSearchablePropertySet(DOMElement.class, NodeIndex.keyword.name(), publicView.properties());

	}
	//~--- methods --------------------------------------------------------

	public boolean avoidWhitespace() {

		return false;

	}

	public Property[] getHtmlAttributes() {

		return htmlView.properties();

	}

	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		double start = System.nanoTime();
		
		boolean edit         = renderContext.getEdit();
		boolean inBody       = renderContext.inBody();
		boolean isVoid       = isVoidElement();
		StringBuilder buffer = renderContext.getBuffer();
		String pageId        = renderContext.getPageId();
		String id            = getUuid();
		String tag           = getProperty(DOMElement.tag);

		buffer.append(indent(depth, true));
		
		if (StringUtils.isNotBlank(tag)) {

			renderContext.setInBody(tag.equals("body"));

			buffer.append("<").append(tag);

			if (edit) {

				if (depth == 1) {

					buffer.append(" structr_page_id='").append(pageId).append("'");
				}

				buffer.append(" structr_element_id=\"").append(id).append("\"");
				buffer.append(" structr_type=\"").append(getType()).append("\"");
				buffer.append(" structr_name=\"").append(getName()).append("\"");

			}

			// FIXME: this will not include arbitrary data-* attributes
			for (PropertyKey attribute : EntityContext.getPropertySet(getClass(), PropertyView.Html)) {

				try {

					String value = getPropertyWithVariableReplacement(securityContext, renderContext, attribute);

					if ((value != null) && StringUtils.isNotBlank(value)) {

						String key = attribute.jsonName().substring(PropertyView.Html.length());

						buffer.append(" ").append(key).append("=\"").append(value).append("\"");

					}

				} catch (Throwable t) {

					t.printStackTrace();

				}

			}
			
			buffer.append(">");

			// recursively render children
			List<AbstractRelationship> rels = getChildRelationships();

			for (AbstractRelationship rel : rels) {

				DOMNode subNode = (DOMNode) rel.getEndNode();

				if (subNode.isNotDeleted()) {

					subNode.render(securityContext, renderContext, depth + 1);
				}

			}
			
			// render end tag, if needed (= if not singleton tags)
			if (StringUtils.isNotBlank(tag) && (!isVoid)) {
				
				buffer.append(indent(depth, true));

				buffer.append("</").append(tag).append(">");
			}

		}
	
		double end = System.nanoTime();

		logger.log(Level.FINE, "Render node {0} in {1} seconds", new java.lang.Object[] { getUuid(), decimalFormat.format((end - start) / 1000000000.0) });
		

	}

	public boolean isVoidElement() {
		return false;
	}
	
	public String getOffsetAttributeName(String name, int offset) {
		
		int namePosition    = -1;
		int index           = 0;
		
		List<String> names = new ArrayList<String>(10);
		for (String key : this.getNode().getPropertyKeys()) {

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
	
	public List<String> getHtmlAttributeNames() {
		
		List<String> names = new ArrayList<String>(10);
		
		for (String key : this.getNode().getPropertyKeys()) {

			// use html properties only
			if (key.startsWith(PropertyView.Html)) {

				names.add(key.substring(HtmlPrefixLength));
			}
		}
		
		return names;
	}

	// ----- protected methods -----
	protected HtmlProperty findOrCreateAttributeKey(String name) {
		
		HtmlProperty htmlProperty = null;
		
		synchronized(htmlProperties) {
		
			htmlProperty = htmlProperties.get(name);
		}
		
		if (htmlProperty == null) {
		
			// try to find native html property defined in
			// DOMElement or one of its subclasses
			PropertyKey key = EntityContext.getPropertyKeyForJSONName(getClass(), name, false);
			
			if (key != null && key instanceof HtmlProperty) {
				
				htmlProperty = (HtmlProperty)key;
				
			} else {
				
				// create synthetic HtmlProperty
				htmlProperty = new HtmlProperty(name);
				htmlProperty.setDeclaringClass(DOMElement.class);
			}
			
			// cache property
			synchronized (htmlProperties) {

				htmlProperties.put(name, htmlProperty);
			}
			
		}
		
		return htmlProperty;
	}
	
	// ----- interface org.w3c.dom.Element -----
	
	@Override
	public String getTagName() {

		return getProperty(tag);
	}

	@Override
	public String getAttribute(String name) {
		
		HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
		
		return htmlProperty.getProperty(securityContext, this, true);
	}

	@Override
	public void setAttribute(String name, String value) throws DOMException {

		HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
		if (htmlProperty != null) {
			
			try {
				htmlProperty.setProperty(securityContext, this, value);
				
			} catch (FrameworkException fex) {
				
				throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());
			}
		}
	}

	@Override
	public void removeAttribute(String name) throws DOMException {

		HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
		if (htmlProperty != null) {
			
			try {
				htmlProperty.setProperty(securityContext, this, null);
				
			} catch (FrameworkException fex) {
				
				throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());
			}
		}
	}

	@Override
	public Attr getAttributeNode(String name) {
		
		HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
		String value              = htmlProperty.getProperty(securityContext, this, true);
		
		if (value != null) {
			
			return new DOMAttribute((Page)getOwnerDocument(), this, name, value);
		}

		return null;
	}

	@Override
	public Attr setAttributeNode(final Attr attr) throws DOMException {

		// save existing attribute node
		Attr attribute = getAttributeNode(attr.getName());
		
		// set value
		setAttribute(attr.getName(), attr.getValue());

		// set parent of attribute node
		if (attr instanceof DOMAttribute) {
			((DOMAttribute)attr).setParent(this);
		}
		
		return attribute;
	}

	@Override
	public Attr removeAttributeNode(final Attr attr) throws DOMException {

		// save existing attribute node
		Attr attribute = getAttributeNode(attr.getName());
		
		// set value
		setAttribute(attr.getName(), null);
		
		return attribute;
	}

	@Override
	public NodeList getElementsByTagName(final String tagName) {
		
		StructrNodeList results = new StructrNodeList();

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
	public String getAttributeNS(String string, String string1) throws DOMException {
		return null;
	}

	@Override
	public void setAttributeNS(String string, String string1, String string2) throws DOMException {
	}

	@Override
	public void removeAttributeNS(String string, String string1) throws DOMException {
	}

	@Override
	public Attr getAttributeNodeNS(String string, String string1) throws DOMException {
		return null;
	}

	@Override
	public Attr setAttributeNodeNS(Attr attr) throws DOMException {
		return null;
	}

	@Override
	public NodeList getElementsByTagNameNS(String string, String string1) throws DOMException {
		return null;
	}

	@Override
	public boolean hasAttribute(String name) {
		return getAttribute(name) != null;
	}

	@Override
	public boolean hasAttributeNS(String string, String string1) throws DOMException {
		return false;
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		return null;
	}

	@Override
	public void setIdAttribute(String idString, boolean isId) throws DOMException {

		checkWriteAccess();

		try {
			setProperty(DOMElement._id, idString);
			
		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());			
		}
	}

	@Override
	public void setIdAttributeNS(String string, String string1, boolean bln) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported.");
	}

	@Override
	public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
		throw new UnsupportedOperationException("Attribute nodes not supported in HTML5.");
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getNodeName() {
		return getTagName();
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String string) throws DOMException {
		// the nodeValue of an Element cannot be set
	}

	@Override
	public short getNodeType() {

		return ELEMENT_NODE;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return this;
	}

	@Override
	public boolean hasAttributes() {
		return getLength() > 0;
	}


	// ----- interface org.w3c.dom.NamedNodeMap -----
	@Override
	public Node getNamedItem(String name) {
		return getAttributeNode(name);
	}

	@Override
	public Node setNamedItem(Node node) throws DOMException {
		
		if (node instanceof Attr) {
			return setAttributeNode((Attr)node);
		}
		
		return null;
	}

	@Override
	public Node removeNamedItem(String name) throws DOMException {

		// save existing attribute node
		Attr attribute = getAttributeNode(name);
		
		// set value to null
		setAttribute(name, null);
		
		return attribute;
	}

	@Override
	public Node item(int i) {
		
		List<String> htmlAttributeNames = getHtmlAttributeNames();
		if (i >= 0 && i < htmlAttributeNames.size()) {
			
			return getAttributeNode(htmlAttributeNames.get(i));
		}
		
		return null;
	}

	@Override
	public int getLength() {
		return getHtmlAttributeNames().size();
	}

	@Override
	public Node getNamedItemNS(String string, String string1) throws DOMException {
		return null;
	}

	@Override
	public Node setNamedItemNS(Node node) throws DOMException {
		return null;
	}

	@Override
	public Node removeNamedItemNS(String string, String string1) throws DOMException {
		return null;
	}
}
