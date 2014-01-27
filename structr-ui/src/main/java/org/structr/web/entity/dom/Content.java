/**
 * Copyright (C) 2010-2014 Structr, c/o Morgner UG (haftungsbeschränkt) <structr@structr.org>
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.markup.confluence.ConfluenceDialect;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.java.textilej.parser.markup.trac.TracWikiDialect;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.pegdown.PegDownProcessor;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.graph.search.Search;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.web.common.RenderContext;
import org.structr.core.property.IntProperty;
import org.structr.core.property.StringProperty;

//~--- JDK imports ------------------------------------------------------------

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Permission;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.web.common.RenderContext.EditMode;
import static org.structr.web.entity.dom.DOMNode.hideOnDetail;
import static org.structr.web.entity.dom.DOMNode.hideOnIndex;

//~--- classes ----------------------------------------------------------------

/**
 * Represents a content node. This class implements the org.w3c.dom.Text interface.
 * All methods in the W3C Text interface are based on the raw database content.
 *
 * @author Axel Morgner
 */
public class Content extends DOMNode implements Text {

	private static final Logger logger                                                   = Logger.getLogger(Content.class.getName());
	public static final Property<String> contentType                                     = new StringProperty("contentType").indexed();
	public static final Property<String> content                                         = new StringProperty("content").indexed();
	public static final Property<Integer> size                                           = new IntProperty("size").indexed();
	
	private static final Map<String, Adapter<String, String>> contentConverters          = new LinkedHashMap<String, Adapter<String, String>>();

	private static final ThreadLocalTracWikiProcessor tracWikiProcessor                  = new ThreadLocalTracWikiProcessor();
	private static final ThreadLocalTextileProcessor textileProcessor                    = new ThreadLocalTextileProcessor();
	private static final ThreadLocalPegDownProcessor pegDownProcessor                    = new ThreadLocalPegDownProcessor();
	private static final ThreadLocalMediaWikiProcessor mediaWikiProcessor                = new ThreadLocalMediaWikiProcessor();
	private static final ThreadLocalConfluenceProcessor confluenceProcessor              = new ThreadLocalConfluenceProcessor();

	public static final org.structr.common.View uiView                                   = new org.structr.common.View(Content.class, PropertyView.Ui,
		content, contentType, size, parent, pageId, hideOnDetail, hideOnIndex, showForLocales, hideForLocales, showConditions, hideConditions);

	public static final org.structr.common.View publicView                               = new org.structr.common.View(Content.class, PropertyView.Public,
		content, contentType, size, parent, pageId, hideOnDetail, hideOnIndex, showForLocales, hideForLocales, showConditions, hideConditions);
	//~--- static initializers --------------------------------------------

	static {

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
//		contentConverters.put("text/plain", new Adapter<String, String>() {
//
//			@Override
//			public String adapt(String s) throws FrameworkException {
//
//				return StringEscapeUtils.escapeHtml(s);
//
//			}
//
//		});

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

		return super.getPropertyForIndexing(key);

	}

	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {
	
		if (isDeleted() || isHidden() || !displayForLocale(renderContext) || !displayForConditions(securityContext, renderContext)) {
			return;
		}

		String id            = getUuid();
		EditMode edit        = renderContext.getEditMode(securityContext.getUser(false));
		boolean inBody       = renderContext.inBody();
		StringBuilder buffer = renderContext.getBuffer();
		
		String _contentType = getProperty(contentType);

		// fetch content with variable replacement
		String _content = getPropertyWithVariableReplacement(securityContext, renderContext, Content.content);
		
		if (!(EditMode.RAW.equals(edit)) && (_contentType == null || ("text/plain".equals(_contentType)))) {

			_content = escapeForHtml(_content);

		}

		if (EditMode.CONTENT.equals(edit) && inBody && securityContext.isAllowed(this, Permission.write)) {

			if ("text/javascript".equals(_contentType)) {
				
				// Javascript will only be given some local vars
				// TODO: Is this neccessary?
				buffer.append("// data-structr-type='").append(getType()).append("'\n// data-structr-id='").append(id).append("'\n");
				
			} else if ("text/css".equals(_contentType)) {
				
				// CSS will only be given some local vars
				// TODO: Is this neccessary?
				buffer.append("/* data-structr-type='").append(getType()).append("'*/\n/* data-structr-id='").append(id).append("'*/\n");
				
			} else {
				
//				// In edit mode, add an artificial 'span' tag around content nodes within body to make them editable
//				buffer.append("<span data-structr-raw-value=\"").append(getProperty(Content.content))
//					//.append("\" data-structr-content-type=\"").append(StringUtils.defaultString(getProperty(Content.contentType), ""))
//					.append("\" data-structr-type=\"").append(getType())
//					.append("\" data-structr-id=\"").append(id).append("\">");
				
//				int l = buffer.length();
//				buffer.replace(l-1, l, " data-structr-raw-value=\""
//					.concat(getProperty(Content.content))
//					.concat("\" data-structr-type=\"").concat(getType())
//					.concat("\" data-structr-id=\"").concat(id).concat("\">"));
				
				buffer.append("<!--data-structr-id=\"".concat(id)
					.concat("\" data-structr-raw-value=\"").concat(getProperty(Content.content).replaceAll("\n", "\\\\n")).concat("\"-->"));
					//.concat("\" data-structr-raw-value=\"").concat(getProperty(Content.content)).concat("\"-->"));
				
			}
			
		}

		// No contentType-specific rendering in DATA edit mode
		//if (!edit.equals(EditMode.DATA)) {
			
			// examine content type and apply converter

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
		//}

		if (_content != null) {

			//buffer.append(indent(depth, true)).append(_content);
			
			// insert whitespace to make element clickable
			if (EditMode.CONTENT.equals(edit) && _content.length() == 0) {
				_content = "--- empty ---";
			}
			
			buffer.append(_content);
		}
		
		if (EditMode.CONTENT.equals(edit) && inBody && !("text/javascript".equals(getProperty(contentType))) && !("text/css".equals(getProperty(contentType)))) {

//			buffer.append("</span>");
			buffer.append("<!---->");
		}

	}

//	@Override
//	protected Object getEditModeValue(final SecurityContext securityContext, final RenderContext renderContext, final GraphObject dataObject, final PropertyKey referenceKeyProperty, final Object defaultValue) {
//
//		Object value      = dataObject.getProperty(StructrApp.getConfiguration().getPropertyKeyForJSONName(dataObject.getClass(), referenceKeyProperty.jsonName()));
//		boolean canWrite  = dataObject instanceof AbstractNode ? securityContext.isAllowed((AbstractNode) dataObject, Permission.write) : true;
//
//		if (getProperty(Content.editable) && EditMode.DATA.equals(renderContext.getEditMode(securityContext.getUser(false))) && renderContext.inBody() && canWrite && !referenceKeyProperty.isReadOnly()) {
//
//			String editModeValue = "<span data-structr-type=\"" + referenceKeyProperty.typeName()
//				+ "\" data-structr-id=\"" + dataObject.getUuid()
////				+ "\" data-structr-content-type=\"" + StringUtils.defaultString(dataObject.getProperty(Content.contentType), "")
////				+ "\" data-structr-visible-to-authenticated-users=\"" + dataObject.getProperty(AbstractNode.visibleToAuthenticatedUsers)
////				+ "\" data-structr-visible-to-public-users=\"" + dataObject.getProperty(AbstractNode.visibleToPublicUsers)
//				+ "\" data-structr-key=\"" + referenceKeyProperty.jsonName() + "\">" + value + "</span>";
//
//			logger.log(Level.FINEST, "Edit mode value: {0}", editModeValue);
//
//			return editModeValue;
//			
//		} else {
//
//			return value != null ? value : defaultValue;
//
//		}
//		
//	}

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
					
					final App app = StructrApp.getInstance(securityContext);
					try {
						app.beginTx();

						// first part goes into existing text element
						setProperty(content, firstPart);

						// second part goes into new text element
						Text newNode = document.createTextNode(secondPart);

						// make new node a child of old parent
						parent.appendChild(newNode);

						app.commitTx();
						
						return newNode;

					} catch (FrameworkException fex) {
			
						throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());			
						
					} finally {
						
						app.finishTx();
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
	public void setData(final String data) throws DOMException {
		
		checkWriteAccess();
		
		final App app = StructrApp.getInstance(securityContext);
		try {
			app.beginTx();
			setProperty(content, data);
			app.commitTx();
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
			
		} finally {
			
			app.finishTx();
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
	public void appendData(final String data) throws DOMException {
		
		checkWriteAccess();

		final App app = StructrApp.getInstance(securityContext);
		try {
			app.beginTx();
			String text = getProperty(content);
			setProperty(content, text.concat(data));
			app.commitTx();
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
			
		} finally {
			
			app.finishTx();
		}
	}

	@Override
	public void insertData(final int offset, final String data) throws DOMException {
		
		checkWriteAccess();

		final App app = StructrApp.getInstance(securityContext);
		try {
			app.beginTx();

			String text = getProperty(content);

			String leftPart  = text.substring(0, offset);
			String rightPart = text.substring(offset);

			StringBuilder buf = new StringBuilder(text.length() + data.length() + 1);
			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			// finally, set content to concatenated left, data and right parts
			setProperty(content, buf.toString());

			app.commitTx();
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
			
		} finally {
			
			app.finishTx();
		}
	}

	@Override
	public void deleteData(final int offset, final int count) throws DOMException {
		
		checkWriteAccess();
		
		// finally, set content to concatenated left and right parts
		final App app = StructrApp.getInstance(securityContext);
		try {
			app.beginTx();

			String text = getProperty(content);

			String leftPart  = text.substring(0, offset);
			String rightPart = text.substring(offset + count);

			setProperty(content, leftPart.concat(rightPart));
			
			app.commitTx();
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
			
		} finally {
			
			app.finishTx();
		}
	}

	@Override
	public void replaceData(final int offset, final int count, final String data) throws DOMException {
		
		checkWriteAccess();

		// finally, set content to concatenated left and right parts
		final App app = StructrApp.getInstance(securityContext);
		try {
			app.beginTx();

			String text = getProperty(content);

			String leftPart  = text.substring(0, offset);
			String rightPart = text.substring(offset + count);

			StringBuilder buf = new StringBuilder(leftPart.length() + data.length() + rightPart.length());
			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			setProperty(content, buf.toString());
			
			app.commitTx();
			
		} catch (FrameworkException fex) {
			
			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
			
		} finally {
			
			app.finishTx();
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
