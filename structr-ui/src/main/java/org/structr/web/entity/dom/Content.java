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

import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.markup.confluence.ConfluenceDialect;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.java.textilej.parser.markup.trac.TracWikiDialect;

import org.apache.commons.lang.StringEscapeUtils;

import org.pegdown.PegDownProcessor;

import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.EntityContext;
import org.structr.core.graph.NodeService;
import org.structr.core.graph.search.Search;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.web.common.RenderContext;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a content node. This class implements the org.w3c.dom.Text interface.
 * All methods in the W3C Text interface are based on the raw database content.
 *
 * @author Axel Morgner
 */
public class Content extends DOMNode implements Text {

	private static final Logger logger                                                   = Logger.getLogger(Content.class.getName());
	public static final Property<String> contentType                                     = new StringProperty("contentType");
	public static final Property<String> content                                         = new StringProperty("content");
	public static final Property<Integer> size                                           = new IntProperty("size");

	private static final Map<String, Adapter<String, String>> contentConverters          = new LinkedHashMap<String, Adapter<String, String>>();

	private static final ThreadLocalTracWikiProcessor tracWikiProcessor                  = new ThreadLocalTracWikiProcessor();
	private static final ThreadLocalTextileProcessor textileProcessor                    = new ThreadLocalTextileProcessor();
	private static final ThreadLocalPegDownProcessor pegDownProcessor                    = new ThreadLocalPegDownProcessor();
	private static final ThreadLocalMediaWikiProcessor mediaWikiProcessor                = new ThreadLocalMediaWikiProcessor();
	private static final ThreadLocalConfluenceProcessor confluenceProcessor              = new ThreadLocalConfluenceProcessor();

	public static final org.structr.common.View uiView                                   = new org.structr.common.View(Content.class, PropertyView.Ui, content, contentType, size, parent, pageId);
	public static final org.structr.common.View publicView                               = new org.structr.common.View(Content.class, PropertyView.Public, content, contentType, size, parent, pageId);
	//~--- static initializers --------------------------------------------

	static {

		EntityContext.registerSearchablePropertySet(Content.class, NodeService.NodeIndex.fulltext.name(), uiView.properties());
		EntityContext.registerSearchablePropertySet(Content.class, NodeService.NodeIndex.keyword.name(), uiView.properties());

		contentConverters.put("text/markdown", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				return pegDownProcessor.get().markdownToHtml(s);

			}

		});
		contentConverters.put("text/textile", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				return textileProcessor.get().parseToHtml(s);

			}

		});
		contentConverters.put("text/mediawiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				return mediaWikiProcessor.get().parseToHtml(s);

			}

		});
		contentConverters.put("text/tracwiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				return tracWikiProcessor.get().parseToHtml(s);

			}

		});
		contentConverters.put("text/confluence", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				return confluenceProcessor.get().parseToHtml(s);

			}

		});
		contentConverters.put("text/plain", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				return StringEscapeUtils.escapeHtml(s);

			}

		});

	}

	//~--- methods --------------------------------------------------------


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

	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		String id            = getUuid();
		boolean edit         = renderContext.getEdit();
		boolean inBody       = renderContext.inBody();
		StringBuilder buffer = renderContext.getBuffer();

		// In edit mode, add an artificial 'div' tag around content nodes within body
		// to make them editable
		if (edit && inBody) {

			buffer.append("<span data-structr_content_id=\"").append(id).append("\">");
		}

		// fetch content with variable replacement
		String _content = getPropertyWithVariableReplacement(renderContext, Content.content);

		// examine content type and apply converter
		String _contentType = getProperty(Content.contentType);

		if (_contentType != null) {

			Adapter<String, String> converter = contentConverters.get(_contentType);

			if (converter != null) {

				try {

					// apply adapter
					_content = converter.adapt(_content);
				} catch (FrameworkException fex) {

					logger.log(Level.WARNING, "Unable to convert content: {0}", fex.getMessage());

				}

			}

		}

		// replace newlines with <br /> for rendering
		if (((_contentType == null) || _contentType.equals("text/plain")) && (_content != null) && !_content.isEmpty()) {

			_content = _content.replaceAll("[\\n]{1}", "<br>");
		}

		if (_content != null) {

			//buffer.append(indent(depth, true)).append(_content);
			buffer.append(_content);
		}
		
		if (edit && inBody) {

			buffer.append("</span>");
		}

	}

	// ----- interface org.w3c.dom.Text -----
	
	@Override
	public Text splitText(int offset) throws DOMException {

		checkWriteAccess();
		
		String text = getProperty(content);
		
		if (text != null) {

			int len = text.length();
			
			if (offset < 0 || offset > len) {
				
				throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);
				
			} else {
				
				final String firstPart  = text.substring(0, offset);
				final String secondPart = text.substring(offset);
				
				final Document document  = getOwnerDocument();
				final Node parent        = getParentNode();
				
				if (document != null && parent != null) {
					
					try {

						return Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<Text>() {

							@Override
							public Text execute() throws FrameworkException {

								// first part goes into existing text element
								setProperty(content, firstPart);
								
								// second part goes into new text element
								Text newNode = document.createTextNode(secondPart);
								
								// make new node a child of old parent
								parent.appendChild(newNode);
								
								return newNode;
							}
						});

					} catch (FrameworkException fex) {
			
						throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());			
					}					
					
				} else {
						
					throw new DOMException(DOMException.INVALID_STATE_ERR, CANNOT_SPLIT_TEXT_WITHOUT_PARENT);
				}
			}
		}
		
		throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);		
	}

	@Override
	public boolean isElementContentWhitespace() {
		
		checkReadAccess();
		
		String text = getProperty(content);
		
		if (text != null) {
		
			return !text.matches("[\\S]*");
		}
		
		return false;
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
		
		checkReadAccess();
		
		return getProperty(content);
	}

	@Override
	public void setData(String data) throws DOMException {
		
		checkWriteAccess();
		
		try {
			
			setProperty(content, data);
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}
	}

	@Override
	public int getLength() {
		
		String text = getProperty(content);
		
		if (text != null) {
			
			return text.length();
		}
		
		return 0;
	}

	@Override
	public String substringData(int offset, int count) throws DOMException {
		
		checkReadAccess();

		String text = getProperty(content);
		
		if (text != null) {

			try {
				
				return text.substring(offset, offset + count);
				
			} catch (IndexOutOfBoundsException iobex) {
				
				throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);
			}
		}
		
		return "";
	}

	@Override
	public void appendData(String data) throws DOMException {
		
		checkWriteAccess();
		
		String text = getProperty(content);
		
		// set content to concatenated text and data
		try {
			
			setProperty(content, text.concat(data));
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}
	}

	@Override
	public void insertData(int offset, String data) throws DOMException {
		
		checkWriteAccess();
		
		String text = getProperty(content);

		String leftPart  = text.substring(0, offset);
		String rightPart = text.substring(offset);

		StringBuilder buf = new StringBuilder(text.length() + data.length() + 1);
		buf.append(leftPart);
		buf.append(data);
		buf.append(rightPart);
		
		// finally, set content to concatenated left, data and right parts
		try {
			
			setProperty(content, buf.toString());
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}		
	}

	@Override
	public void deleteData(int offset, int count) throws DOMException {
		
		checkWriteAccess();
		
		String text = getProperty(content);
		
		String leftPart  = text.substring(0, offset);
		String rightPart = text.substring(offset + count);
		
		// finally, set content to concatenated left and right parts
		try {
			
			setProperty(content, leftPart.concat(rightPart));
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}
	}

	@Override
	public void replaceData(int offset, int count, String data) throws DOMException {
		
		checkWriteAccess();
		
		String text = getProperty(content);
		
		String leftPart  = text.substring(0, offset);
		String rightPart = text.substring(offset + count);
		
		StringBuilder buf = new StringBuilder(leftPart.length() + data.length() + rightPart.length());
		buf.append(leftPart);
		buf.append(data);
		buf.append(rightPart);

		// finally, set content to concatenated left and right parts
		try {
			
			setProperty(content, buf.toString());
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}
	}
	
	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getTextContent() throws DOMException {
		return getData();
	}

	@Override
	public void setTextContent(String textContent) throws DOMException {
		setData(textContent);
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public short getNodeType() {
		return TEXT_NODE;
	}

	@Override
	public String getNodeName() {
		return "#text";
	}

	@Override
	public String getNodeValue() throws DOMException {
		return getData();
	}

	@Override
	public void setNodeValue(String data) throws DOMException {
		setData(data);
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public boolean hasAttributes() {
		return false;
	}

	// ----- interface DOMImportable -----
	@Override
	public Node doImport(Page newPage) throws DOMException {
		
		// for #text elements, importing is basically a clone operation
		return newPage.createTextNode(getData());
	}

	//~--- inner classes --------------------------------------------------

	private static class ThreadLocalConfluenceProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new ConfluenceDialect());

		}

	}


	private static class ThreadLocalMediaWikiProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new MediaWikiDialect());

		}

	}

	private static class ThreadLocalPegDownProcessor extends ThreadLocal<PegDownProcessor> {

		@Override
		protected PegDownProcessor initialValue() {

			return new PegDownProcessor();

		}

	}


	private static class ThreadLocalTextileProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new TextileDialect());

		}

	}


	private static class ThreadLocalTracWikiProcessor extends ThreadLocal<MarkupParser> {

		@Override
		protected MarkupParser initialValue() {

			return new MarkupParser(new TracWikiDialect());

		}

	}
}
