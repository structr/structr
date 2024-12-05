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
package org.structr.web.entity.dom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.Adapter;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Favoritable;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.NonIndexed;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.converter.ContentConverters;
import org.structr.web.entity.html.Textarea;
import org.w3c.dom.*;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Represents a content node. This class implements the org.w3c.dom.Text interface.
 * All methods in the W3C Text interface are based on the raw database content.
 */
public class Content extends DOMNode implements Text, NonIndexed {

	public static final Property<String> contentProperty     = new StringProperty("content").indexed().partOfBuiltInSchema();
	public static final Property<String> contentTypeProperty = new StringProperty("contentType").indexed().partOfBuiltInSchema();
	public static final Property<Boolean> isContentProperty  = new ConstantBooleanProperty("isContent", true).partOfBuiltInSchema();

	public static final View defaultView = new View(Content.class, PropertyView.Public,
		isContentProperty, contentTypeProperty, contentProperty, isDOMNodeProperty, pageIdProperty, parentProperty, sharedComponentIdProperty, syncedNodesIdsProperty, hideConditionsProperty, showConditionsProperty, hideForLocalesProperty,
		showForLocalesProperty, sharedComponentConfigurationProperty, dataKeyProperty, cypherQueryProperty,
		restQueryProperty, functionQueryProperty
	);

	public static final View uiView = new View(Content.class, PropertyView.Ui,
		isContentProperty, contentTypeProperty, contentProperty, sharedComponentConfigurationProperty, isDOMNodeProperty, pageIdProperty, parentProperty, sharedComponentIdProperty, syncedNodesIdsProperty,
		showForLocalesProperty, hideForLocalesProperty, showConditionsProperty, hideConditionsProperty, dataKeyProperty, cypherQueryProperty, restQueryProperty,
		functionQueryProperty
	);

	public String getContent() {
		return getProperty(contentProperty);
	}

	public void setContent(final String content) throws FrameworkException {
		setProperty(contentProperty, content);
	}

	public String getContentType() {
		return getProperty(contentTypeProperty);
	}

	public void setContentType(final String contentType) throws FrameworkException {
		setProperty(contentTypeProperty, contentType);
	}

	public void updateFromNode(final DOMNode newNode) throws FrameworkException {

		if (newNode instanceof Content content) {

			this.setContent(content.getContent());
		}
	}

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		if (getContentType() == null) {
			setContentType("text/plain");
		}
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		final String uuid = this.getUuid();
		if (uuid != null) {

			// acknowledge all events for this node when it is modified
			RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
		}

		final PropertyMap map = new PropertyMap();

		// sync content only
		map.put(contentProperty,     this.getContent());
		map.put(contentTypeProperty, this.getContentType());
		map.put(name,                this.getProperty(name));

		for (final DOMNode syncedNode : this.getSyncedNodes()) {

			syncedNode.setProperties(securityContext, map);
		}

		final DOMNode sharedComponent = this.getSharedComponent();
		if (sharedComponent != null) {

			sharedComponent.setProperties(sharedComponent.getSecurityContext(), map);
		}
	}

	@Override
	public boolean contentEquals(final Node otherNode) {

		if (otherNode instanceof Content content) {

			final String content1 = this.getTextContent();
			final String content2 = content.getTextContent();

			if (content1 == null && content2 == null) {
				return true;
			}

			if (content1 != null && content2 != null) {

				return content1.equals(content2);
			}
		}

		return false;
	}

	public void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {

		final SecurityContext securityContext = this.getSecurityContext();

		try {

			final EditMode edit = renderContext.getEditMode(securityContext.getUser(false));
			if (EditMode.DEPLOYMENT.equals(edit)) {

				final AsyncBuffer buf = renderContext.getBuffer();

				// output ownership comments
				renderDeploymentExportComments(buf, true);

				// EditMode "deployment" means "output raw content, do not interpret in any way
				buf.append(escapeForHtml(this.getContent()));

				return;
			} else if (EditMode.CONTENT.equals(edit)) {

			}

			if (!this.shouldBeRendered(renderContext)) {
				return;
			}

			final RenderContextContentHandler handler = new RenderContextContentHandler(this, renderContext);
			final String id                           = this.getUuid();
			final AsyncBuffer out                     = renderContext.getBuffer();
			final String _contentType                 = this.getContentType();

			// apply configuration for shared component if present
			final String _sharedComponentConfiguration = this.getSharedComponentConfiguration();
			if (StringUtils.isNotBlank(_sharedComponentConfiguration)) {

				Scripting.evaluate(renderContext, this, "${" + _sharedComponentConfiguration.trim() + "}", "sharedComponentConfiguration", 0, this.getUuid());
			}

			// determine some postprocessing flags
			if (!(EditMode.RAW.equals(edit) || EditMode.WIDGET.equals(edit)) && (_contentType == null || ("text/plain".equals(_contentType)))) {

				handler.setEscapeForHtml(true);
			}

			if (EditMode.CONTENT.equals(edit) && this.isGranted(Permission.write, securityContext)) {

				if ("text/javascript".equals(_contentType)) {

					// Javascript will only be given some local vars
					out.append("// data-structr-type='").append(this.getType()).append("'\n// data-structr-id='").append(id).append("'\n");

				} else if ("text/css".equals(_contentType)) {

					// CSS will only be given some local vars
					out.append("/* data-structr-type='").append(this.getType()).append("'*/\n/* data-structr-id='").append(id).append("'*/\n");

				} else {

					// In edit mode, add an artificial comment tag around content nodes within body to make them editable
					//final String cleanedContent = StringUtils.remove(StringUtils.remove(org.apache.commons.lang3.StringUtils.replace(this.getContent(), "\n", "\\\\n"), "<!--"), "-->");
					final String cleanedContent = org.apache.commons.lang3.StringUtils.replace(this.getContent(), "\n", "\\\\n");
					out.append("<!--data-structr-id=\"".concat(id).concat("\" data-structr-raw-value=\"").concat(escapeForHtmlAttributes(cleanedContent)).concat("\"-->"));
				}
			}

			if (_contentType != null) {

				handler.setConverter(ContentConverters.getConverterForType(_contentType));
			}

			// replace newlines with <br /> for rendering
			if (((_contentType == null) || _contentType.equals("text/plain"))) {

				final DOMNode _parent = this.getParent();
				if (_parent == null || !(_parent instanceof Textarea)) {

					handler.setReplaceNewlines(true);
				}
			}

			// render content with support for async output
			renderContentWithScripts(getContent(), handler);

			// empty content placeholder for Structr UI
			if (EditMode.CONTENT.equals(edit)) {

				if (handler.isEmpty()) {

					//out.append("--- empty ---");
				}

				if (!("text/javascript".equals(_contentType) && !("text/css".equals(_contentType)))) {

					out.append("<!---->");
				}
			}

		} catch (Throwable t) {

			final Logger logger        = LoggerFactory.getLogger(Content.class);
			final boolean isShadowPage = this.isSharedComponent();

			// catch exception to prevent status 500 error pages in frontend.
			if (!isShadowPage) {

				final DOMNode ownerDocument = this.getOwnerDocumentAsSuperUser();
				DOMNode.logScriptingError(logger, t, "Error while evaluating script in page {}[{}], Content[{}]", ownerDocument.getProperty(AbstractNode.name), ownerDocument.getProperty(AbstractNode.id), this.getUuid());

			} else {

				DOMNode.logScriptingError(logger, t, "Error while evaluating script in shared component, Content[{}]", this.getUuid());
			}
		}
	}

	// ----- interface Favoritable -----
	public String getContext() {

		if (StringUtils.isNotBlank(getName())) {
			return getName();
		}

		return getPagePath();
	}

	public String getFavoriteContent() {
		return getContent();
	}

	public String getFavoriteContentType() {
		return getContentType();
	}

	public void setFavoriteContent(final String content) throws FrameworkException {
		setContent(content);
	}

	// ----- interface org.w3c.dom.Text -----
	public String getTextContent() throws DOMException {
		return getData();
	}

	public Text splitText(final int offset) throws DOMException {

		this.checkWriteAccess();

		final String text = this.getContent();
		if (text != null) {

			int len = text.length();

			if (offset < 0 || offset > len) {

				throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);

			} else {

				final String firstPart  = text.substring(0, offset);
				final String secondPart = text.substring(offset);

				final Document document  = this.getOwnerDocument();
				final Node parent        = this.getParentNode();

				if (document != null && parent != null) {

					try {

						// first part goes into existing text element
						this.setContent(firstPart);

						// second part goes into new text element
						final Text newNode = document.createTextNode(secondPart);

						// make new node a child of old parent
						parent.appendChild(newNode);


						return newNode;

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

	public boolean isElementContentWhitespace() {

		this.checkReadAccess();

		String text = this.getContent();

		if (text != null) {

			return !text.matches("[\\S]*");
		}

		return false;
	}

	public void setData(final String data) throws DOMException {

		this.checkWriteAccess();

		try {

			this.setContent(data);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	public int getLength() {

		final String text = this.getContent();
		if (text != null) {

			return text.length();
		}

		return 0;
	}

	public String substringData(int offset, int count) throws DOMException {

		this.checkReadAccess();

		String text = this.getContent();
		if (text != null) {

			try {

				return text.substring(offset, offset + count);

			} catch (IndexOutOfBoundsException iobex) {

				throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);
			}
		}

		return "";
	}

	public void appendData(final String data) throws DOMException {

		this.checkWriteAccess();

		try {
			final String text = this.getContent();

			this.setContent(text.concat(data));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	public void insertData(final int offset, final String data) throws DOMException {

		this.checkWriteAccess();

		try {

			final String text       = this.getContent();
			final String leftPart   = text.substring(0, offset);
			final String rightPart  = text.substring(offset);
			final StringBuilder buf = new StringBuilder(text.length() + data.length() + 1);

			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			// finally, set content to concatenated left, data and right parts
			this.setContent(buf.toString());

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	public void deleteData(final int offset, final int count) throws DOMException {

		this.checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			final String text      = this.getContent();
			final String leftPart  = text.substring(0, offset);
			final String rightPart = text.substring(offset + count);

			this.setContent(leftPart.concat(rightPart));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	public void replaceData(final int offset, final int count, final String data) throws DOMException {

		this.checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			final String text       = this.getContent();
			final String leftPart   = text.substring(0, offset);
			final String rightPart  = text.substring(offset + count);
			final StringBuilder buf = new StringBuilder(leftPart.length() + data.length() + rightPart.length());

			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			this.setContent(buf.toString());

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	public static void renderContentWithScripts(final String source, final ContentHandler handler) throws FrameworkException, IOException {

		if (source != null) {

			final StringBuilder scriptBuffer = new StringBuilder();
			final StringBuilder textBuffer   = new StringBuilder();
			final int length                 = source.length();
			boolean ignoreNext               = false;
			boolean inComment                = false;
			boolean inSingleQuotes           = false;
			boolean inDoubleQuotes           = false;
			boolean inTemplate               = false;
			boolean hasSlash                 = false;
			boolean hasBackslash             = false;
			boolean hasDollar                = false;
			int startRow                     = 0;
			int level                        = 0;
			int row                          = 0;
			int column                       = 0;

			for (int i=0; i<length; i++) {

				final char c = source.charAt(i);

				switch (c) {

					case '\\':
						hasBackslash  = !hasBackslash;
						break;

					case '\'':
						if (inTemplate && !inDoubleQuotes && !hasBackslash && !inComment) {
							inSingleQuotes = !inSingleQuotes;
						}
						hasDollar = false;
						hasBackslash = false;
						break;

					case '\"':
						if (inTemplate && !inSingleQuotes && !hasBackslash && !inComment) {
							inDoubleQuotes = !inDoubleQuotes;
						}
						hasDollar = false;
						hasBackslash = false;
						break;

					case '$':
						if (!inComment) {

							hasDollar    = true;
							hasBackslash = false;
						}
						break;

					case '{':
						if (!inTemplate && hasDollar && !inComment) {

							startRow   = row;
							inTemplate = true;

							// extract and handle content from non-script buffer
							textBuffer.setLength(Math.max(0, textBuffer.length() - 1));

							if (textBuffer.length() > 0) {

								// call handler
								handler.handleText(textBuffer.toString());
							}

							// switch to other buffer
							textBuffer.setLength(0);
							scriptBuffer.append("$");

							handler.possibleStartOfScript(row, column-1);

						} else if (inTemplate && !inSingleQuotes && !inDoubleQuotes && !inComment) {
							level++;
						}

						hasDollar = false;
						hasBackslash = false;
						break;

					case '}':

						if (!inSingleQuotes && !inDoubleQuotes && inTemplate && !inComment && level-- == 0) {

							inTemplate = false;
							level      = 0;

							// append missing }
							scriptBuffer.append("}");

							// call handler
							handler.handleScript(scriptBuffer.toString(), startRow, column);

							// switch to other buffer
							scriptBuffer.setLength(0);

							ignoreNext = true;
						}
						hasDollar    = false;
						hasBackslash = false;
						hasSlash = false;
						break;

					case '/':

						if (inTemplate && !inComment && !inSingleQuotes && !inDoubleQuotes) {

							if (hasSlash) {

								inComment = true;
								hasSlash  = false;

							} else {

								hasSlash = true;
							}
						}
						break;

					case '\r':
					case '\n':
						inComment = false;
						column = 0;
						row++;
						break;

					default:
						hasDollar = false;
						hasBackslash = false;
						break;
				}

				if (ignoreNext) {

					ignoreNext = false;

				} else {

					if (inTemplate) {

						scriptBuffer.append(c);

					} else {

						textBuffer.append(c);
					}
				}

				column++;
			}

			if (scriptBuffer.length() > 0) {

				// something's wrong, content ended inside of script template block
				handler.handleIncompleteScript(scriptBuffer.toString());
			}

			if (textBuffer.length() > 0) {

				// handle text
				handler.handleText(textBuffer.toString());
			}
		}
	}

	@Override
	public String getContextName() {
		return StringUtils.defaultString(getProperty(AbstractNode.name), "#text");
	}

	@Override
	public String getNodeName() {
		return "#text";
	}

	@Override
	public String getNodeValue() {
		return getData();
	}

	@Override
	public void setNodeValue(final String value) {
		setData(value);
	}

	@Override
	public short getNodeType() {
		return TEXT_NODE;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	public boolean hasAttributes() {
		return false;
	}

	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public void setTextContent(final String value) {
		setData(value);
	}

	@Override
	public Node doImport(final Page page) {
		return page.createTextNode(getData());
	}

	@Override
	public String getWholeText() {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public Text replaceWholeText(final String text) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public String getData() {
		checkReadAccess();
		return getProperty(contentProperty);
	}

	public interface ContentHandler {

		void handleScript(final String script, final int row, final int column) throws FrameworkException, IOException;
		void handleIncompleteScript(final String script) throws FrameworkException, IOException;
		void handleText(final String text) throws FrameworkException;
		void possibleStartOfScript(final int row, final int column);
	}

	public class RenderContextContentHandler implements ContentHandler {

		private Adapter<String, String> converter = null;
		private RenderContext renderContext       = null;
		private Content node                      = null;
		private boolean replaceNewlines           = false;
		private boolean escapeForHtml             = false;
		private boolean isEmpty                   = true;

		public RenderContextContentHandler(final Content node, final RenderContext renderContext) {

			this.renderContext = renderContext;
			this.node          = node;
		}

		public void setConverter(final Adapter<String, String> converter) {
			this.converter = converter;
		}

		public void setReplaceNewlines(final boolean replaceNewlines) {
			this.replaceNewlines = replaceNewlines;
		}

		public void setEscapeForHtml(final boolean escapeForHtml) {
			this.escapeForHtml = escapeForHtml;
		}

		public boolean isEmpty() {
			return isEmpty;
		}

		@Override
		public void handleIncompleteScript(final String script) throws FrameworkException, IOException {
		}

		@Override
		public void handleScript(final String script, final int row, final int column) throws FrameworkException, IOException {

			try {

				if (renderContext.returnRawValue()) {

					if (StringUtils.isNotBlank(script)) {

						renderContext.getBuffer().append(transform(script));
						isEmpty = false;
					}

				} else {

					final Object value = Scripting.evaluate(renderContext, node, script, "content", row, node.getUuid());
					if (value != null) {

						String content = null;

						// Convert binary data to String with charset from response
						if (value instanceof byte[]) {

							content = StringUtils.toEncodedString((byte[]) value, Charset.forName(renderContext.getResponse().getCharacterEncoding()));

						} else {

							content = value.toString();
						}

						if (StringUtils.isNotBlank(content)) {

							renderContext.getBuffer().append(transform(content));
							isEmpty = false;
						}
					}
				}

			} catch (Throwable t) {

				final Logger logger = LoggerFactory.getLogger(Content.class);
				final boolean isShadowPage = node.isSharedComponent();

				// catch exception to prevent status 500 error pages in frontend.
				if (!isShadowPage) {

					final DOMNode ownerDocument = node.getOwnerDocumentAsSuperUser();
					DOMNode.logScriptingError(logger, t, "Error while evaluating script in page {}[{}], Content[{}]", ownerDocument.getProperty(AbstractNode.name), ownerDocument.getProperty(AbstractNode.id), node.getUuid());

				} else {

					DOMNode.logScriptingError(logger, t, "Error while evaluating script in shared component, Content[{}]", node.getUuid());
				}
			}

		}

		@Override
		public void handleText(final String text) throws FrameworkException {

			if (!text.isEmpty()) {
				isEmpty = false;
			}

			renderContext.getBuffer().append(transform(text));
		}

		@Override
		public void possibleStartOfScript(final int row, final int column) {
		}

		private String transform(final String src) throws FrameworkException {

			String content = src;

			if (escapeForHtml) {
				content = escapeForHtml(content);
			}

			if (converter != null) {
				content = converter.adapt(content);
			}

			if (replaceNewlines) {
				content = content.replaceAll("[\\n]{1}", "<br>");
			}

			return content;
		}
	}
}
