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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.property.IntProperty;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class DOMElement extends DOMNode implements Element {

	private static final Logger logger                             = Logger.getLogger(DOMElement.class.getName());
	
	private DecimalFormat decimalFormat                            = new DecimalFormat("0.000000000", DecimalFormatSymbols.getInstance(Locale.ENGLISH));	

	
	public static final Property<Integer> version                  = new IntProperty("version");
	public static final Property<String> tag                       = new StringProperty("tag");
	public static final Property<String> path                      = new StringProperty("path");
	
	public static final Property<String> _title                  = new HtmlProperty("title");
	public static final Property<String> _tabindex               = new HtmlProperty("tabindex");
	public static final Property<String> _style                  = new HtmlProperty("style");
	public static final Property<String> _spellcheck             = new HtmlProperty("spellcheck");
	public static final Property<String> _onwaiting              = new HtmlProperty("onwaiting");
	public static final Property<String> _onvolumechange         = new HtmlProperty("onvolumechange");
	public static final Property<String> _ontimeupdate           = new HtmlProperty("ontimeupdate");
	public static final Property<String> _onsuspend              = new HtmlProperty("onsuspend");
	public static final Property<String> _onsubmit               = new HtmlProperty("onsubmit");
	public static final Property<String> _onstalled              = new HtmlProperty("onstalled");
	public static final Property<String> _onshow                 = new HtmlProperty("onshow");
	public static final Property<String> _onselect               = new HtmlProperty("onselect");
	public static final Property<String> _onseeking              = new HtmlProperty("onseeking");
	public static final Property<String> _onseeked               = new HtmlProperty("onseeked");
	public static final Property<String> _onscroll               = new HtmlProperty("onscroll");
	public static final Property<String> _onreset                = new HtmlProperty("onreset");
	public static final Property<String> _onreadystatechange     = new HtmlProperty("onreadystatechange");
	public static final Property<String> _onratechange           = new HtmlProperty("onratechange");
	public static final Property<String> _onprogress             = new HtmlProperty("onprogress");
	public static final Property<String> _onplaying              = new HtmlProperty("onplaying");
	public static final Property<String> _onplay                 = new HtmlProperty("onplay");
	public static final Property<String> _onpause                = new HtmlProperty("onpause");
	public static final Property<String> _onmousewheel           = new HtmlProperty("onmousewheel");
	public static final Property<String> _onmouseup              = new HtmlProperty("onmouseup");
	public static final Property<String> _onmouseover            = new HtmlProperty("onmouseover");
	public static final Property<String> _onmouseout             = new HtmlProperty("onmouseout");
	public static final Property<String> _onmousemove            = new HtmlProperty("onmousemove");
	public static final Property<String> _onmousedown            = new HtmlProperty("onmousedown");
	public static final Property<String> _onloadstart            = new HtmlProperty("onloadstart");
	public static final Property<String> _onloadedmetadata       = new HtmlProperty("onloadedmetadata");
	public static final Property<String> _onloadeddata           = new HtmlProperty("onloadeddata");
	public static final Property<String> _onload                 = new HtmlProperty("onload");
	public static final Property<String> _onkeyup                = new HtmlProperty("onkeyup");
	public static final Property<String> _onkeypress             = new HtmlProperty("onkeypress");
	public static final Property<String> _onkeydown              = new HtmlProperty("onkeydown");
	public static final Property<String> _oninvalid              = new HtmlProperty("oninvalid");
	public static final Property<String> _oninput                = new HtmlProperty("oninput");
	public static final Property<String> _onfocus                = new HtmlProperty("onfocus");
	public static final Property<String> _onerror                = new HtmlProperty("onerror");
	public static final Property<String> _onended                = new HtmlProperty("onended");
	public static final Property<String> _onemptied              = new HtmlProperty("onemptied");
	public static final Property<String> _ondurationchange       = new HtmlProperty("ondurationchange");
	public static final Property<String> _ondrop                 = new HtmlProperty("ondrop");
	public static final Property<String> _ondragstart            = new HtmlProperty("ondragstart");
	public static final Property<String> _ondragover             = new HtmlProperty("ondragover");
	public static final Property<String> _ondragleave            = new HtmlProperty("ondragleave");
	public static final Property<String> _ondragenter            = new HtmlProperty("ondragenter");
	public static final Property<String> _ondragend              = new HtmlProperty("ondragend");
	public static final Property<String> _ondrag                 = new HtmlProperty("ondrag");
	public static final Property<String> _ondblclick             = new HtmlProperty("ondblclick");
	public static final Property<String> _oncontextmenu          = new HtmlProperty("oncontextmenu");
	public static final Property<String> _onclick                = new HtmlProperty("onclick");
	public static final Property<String> _onchange               = new HtmlProperty("onchange");
	public static final Property<String> _oncanplaythrough       = new HtmlProperty("oncanplaythrough");
	public static final Property<String> _oncanplay              = new HtmlProperty("oncanplay");
	public static final Property<String> _onblur                 = new HtmlProperty("onblur");

	// Event-handler attributes
	public static final Property<String> _onabort   = new HtmlProperty("onabort");
	public static final Property<String> _lang      = new HtmlProperty("lang");
	public static final Property<String> _id        = new HtmlProperty("id");
	public static final Property<String> _hidden    = new HtmlProperty("hidden");
	public static final Property<String> _dropzone  = new HtmlProperty("dropzone");
	public static final Property<String> _draggable = new HtmlProperty("draggable");
	public static final Property<String> _dir       = new HtmlProperty("dir");

	// needed for Importer
	public static final Property<String> _data            = new HtmlProperty("data");
	public static final Property<String> _contextmenu     = new HtmlProperty("contextmenu");
	public static final Property<String> _contenteditable = new HtmlProperty("contenteditable");
	public static final Property<String> _class           = new HtmlProperty("class");

	// Core attributes
	public static final Property<String> _accesskey = new HtmlProperty("accesskey");
	
	public static final org.structr.common.View publicView = new org.structr.common.View(DOMElement.class, PropertyView.Public, name, tag, path, parent);
	
	public static final org.structr.common.View uiView             = new org.structr.common.View(DOMElement.class, PropertyView.Ui, name, tag, path, parent, children, _accesskey, _class,
										 _contenteditable, _contextmenu, _dir, _draggable, _dropzone, _hidden, _id, _lang, _spellcheck, _style, _tabindex,
										 _title, _onabort, _onblur, _oncanplay, _oncanplaythrough, _onchange, _onclick, _oncontextmenu, _ondblclick, _ondrag,
										 _ondragend, _ondragenter, _ondragleave, _ondragover, _ondragstart, _ondrop, _ondurationchange, _onemptied, _onended,
										 _onerror, _onfocus, _oninput, _oninvalid, _onkeydown, _onkeypress, _onkeyup, _onload, _onloadeddata,
										 _onloadedmetadata, _onloadstart, _onmousedown, _onmousemove, _onmouseout, _onmouseover, _onmouseup, _onmousewheel,
										 _onpause, _onplay, _onplaying, _onprogress, _onratechange, _onreadystatechange, _onreset, _onscroll, _onseeked,
										 _onseeking, _onselect, _onshow, _onstalled, _onsubmit, _onsuspend, _ontimeupdate, _onvolumechange, _onwaiting);
	public static final org.structr.common.View htmlView   = new org.structr.common.View(DOMElement.class, PropertyView.Html, _accesskey, _class, _contenteditable, _contextmenu, _dir,
									 _draggable, _dropzone, _hidden, _id, _lang, _spellcheck, _style, _tabindex, _title, _onabort, _onblur, _oncanplay,
									 _oncanplaythrough, _onchange, _onclick, _oncontextmenu, _ondblclick, _ondrag, _ondragend, _ondragenter, _ondragleave,
									 _ondragover, _ondragstart, _ondrop, _ondurationchange, _onemptied, _onended, _onerror, _onfocus, _oninput, _oninvalid,
									 _onkeydown, _onkeypress, _onkeyup, _onload, _onloadeddata, _onloadedmetadata, _onloadstart, _onmousedown, _onmousemove,
									 _onmouseout, _onmouseover, _onmouseup, _onmousewheel, _onpause, _onplay, _onplaying, _onprogress, _onratechange,
									 _onreadystatechange, _onreset, _onscroll, _onseeked, _onseeking, _onselect, _onshow, _onstalled, _onsubmit, _onsuspend,
									 _ontimeupdate, _onvolumechange, _onwaiting);

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

			final HttpServletRequest request = renderContext.getRequest();

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

//			boolean whitespaceOnly = false;
//			int lastNewline        = buffer.lastIndexOf("\n");
//
//			whitespaceOnly = StringUtils.isBlank((lastNewline > -1)
//				? buffer.substring(lastNewline)
//				: buffer.toString());
//
//			if (!avoidWhitespace()) {
//
//				if (whitespaceOnly) {
//
//					buffer.replace(buffer.length() - 2, buffer.length(), "");
//
//				} else {
//
//					buffer.append(indent(depth - 1, true));
//
//				}
//
//			}

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

//				if (!avoidWhitespace()) {
//
//					buffer.append(indent(depth - 1, true));
//
//				}

			}

		}
	
//		if (!isVoid) {
//
//			buffer.append(ind);
//		}

		double end = System.nanoTime();

		logger.log(Level.FINE, "Render node {0} in {1} seconds", new java.lang.Object[] { getUuid(), decimalFormat.format((end - start) / 1000000000.0) });
		

	}

	public boolean isVoidElement() {

		return false;
	}

	// ----- interface org.w3c.dom.Element -----
	
	@Override
	public String getTagName() {
		return getProperty(tag);
	}

	@Override
	public String getAttribute(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setAttribute(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void removeAttribute(String string) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr getAttributeNode(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr setAttributeNode(Attr attr) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr removeAttributeNode(Attr attr) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public NodeList getElementsByTagName(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getAttributeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setAttributeNS(String string, String string1, String string2) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void removeAttributeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr getAttributeNodeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Attr setAttributeNodeNS(Attr attr) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public NodeList getElementsByTagNameNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasAttribute(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasAttributeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setIdAttribute(String string, boolean bln) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setIdAttributeNS(String string, String string1, boolean bln) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setIdAttributeNode(Attr attr, boolean bln) throws DOMException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	// ----- interface org.w3c.dom.Node -----
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
	}

	@Override
	public short getNodeType() {
		return ELEMENT_NODE;
	}

	@Override
	public NamedNodeMap getAttributes() {
		
		// TODO: implement attribute operations here
		
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
