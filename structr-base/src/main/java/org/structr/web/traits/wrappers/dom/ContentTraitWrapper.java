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
package org.structr.web.traits.wrappers.dom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.graph.NodeInterface;
import org.structr.core.script.Scripting;
import org.structr.core.traits.Traits;
import org.structr.web.ContentHandler;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.traits.definitions.dom.ContentTraitDefinition;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Represents a content node. This class implements the org.w3c.dom.Text interface.
 * All methods in the W3C Text interface are based on the raw database content.
 */
public class ContentTraitWrapper extends DOMNodeTraitWrapper implements Content {

	public ContentTraitWrapper(final Traits traits, final NodeInterface wrappedObject) {
		super(traits, wrappedObject);
	}

	@Override public String getContent() {
		return wrappedObject.getProperty(traits.key(ContentTraitDefinition.CONTENT_PROPERTY));
	}

	@Override public void setContent(final String content) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ContentTraitDefinition.CONTENT_PROPERTY), content);
	}

	@Override public void setContentType(final String contentType) throws FrameworkException {
		wrappedObject.setProperty(traits.key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), contentType);
	}

	@Override
	public final String getContentType() {
		return wrappedObject.getProperty(traits.key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY));
	}

	@Override
	public String getNodeValue() {

		try {

			return getData();

		} catch (FrameworkException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void setData(final String data) throws FrameworkException {

		this.checkWriteAccess();

		try {

			this.setContent(data);

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());

		}
	}

	public int getLength() {

		final String text = this.getContent();
		if (text != null) {

			return text.length();
		}

		return 0;
	}

	public String substringData(int offset, int count) throws FrameworkException {

		this.checkReadAccess();

		String text = this.getContent();
		if (text != null) {

			try {

				return text.substring(offset, offset + count);

			} catch (IndexOutOfBoundsException iobex) {

				throw new FrameworkException(422, INDEX_SIZE_ERR_MESSAGE);
			}
		}

		return "";
	}

	public void appendData(final String data) throws FrameworkException {

		this.checkWriteAccess();

		try {
			final String text = this.getContent();

			this.setContent(text.concat(data));

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());

		}
	}

	public void insertData(final int offset, final String data) throws FrameworkException {

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

			throw new FrameworkException(422, fex.toString());

		}
	}

	public void deleteData(final int offset, final int count) throws FrameworkException {

		this.checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			final String text      = this.getContent();
			final String leftPart  = text.substring(0, offset);
			final String rightPart = text.substring(offset + count);

			this.setContent(leftPart.concat(rightPart));

		} catch (FrameworkException fex) {

			throw new FrameworkException(422, fex.toString());

		}
	}

	public void replaceData(final int offset, final int count, final String data) throws FrameworkException {

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

			throw new FrameworkException(422, fex.toString());

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

	public void setTextContent(final String value) throws FrameworkException {
		setData(value);
	}

	public String getData() throws FrameworkException {

		checkReadAccess();
		return getContent();
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

					final Object value = Scripting.evaluate(renderContext, node, script, "content", row, node.getUuid(), Settings.WrapJSInMainFunction.getValue(false));
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

				final Logger logger        = LoggerFactory.getLogger(Content.class);
				final DOMNode domNode      = node.as(DOMNode.class);
				final boolean isShadowPage = domNode.isSharedComponent();

				// catch exception to prevent status 500 error pages in frontend.
				if (!isShadowPage) {

					final DOMNode ownerDocument = domNode.getOwnerDocumentAsSuperUser();
					DOMNode.logScriptingError(logger, t, "Error while evaluating script in page {}[{}], Content[{}]", ownerDocument.getName(), ownerDocument.getUuid(), node.getUuid());

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
				content = DOMNode.escapeForHtml(content);
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
