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
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.common.ConstantBooleanTrue;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.Adapter;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Favoritable;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.converter.ContentConverters;
import org.structr.web.entity.html.Textarea;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

import static org.structr.web.entity.dom.DOMNode.escapeForHtml;
import static org.structr.web.entity.dom.DOMNode.escapeForHtmlAttributes;

/**
 * Represents a content node. This class implements the org.w3c.dom.Text interface.
 * All methods in the W3C Text interface are based on the raw database content.
 */
public interface Content extends DOMNode, Text, NonIndexed, Favoritable {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("Content");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/Content"));
		type.setImplements(URI.create("#/definitions/Favoritable"));
		type.setExtends(URI.create("#/definitions/DOMNode"));
		type.setCategory("ui");

		type.addBooleanProperty("isContent",  PropertyView.Public, PropertyView.Ui).setReadOnly(true).addTransformer(ConstantBooleanTrue.class.getName());
		type.addStringProperty("contentType", PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("content",     PropertyView.Public, PropertyView.Ui).setIndexed(true);

		type.addPropertyGetter("contentType", String.class);
		type.addPropertySetter("contentType", String.class);
		type.addPropertyGetter("content",     String.class);
		type.addPropertySetter("content",     String.class);

		type.overrideMethod("updateFromNode",             false, Content.class.getName() + ".updateFromNode(this, arg0);");
		type.overrideMethod("renderContent",              false, Content.class.getName() + ".renderContent(this, arg0, arg1);");
		type.overrideMethod("contentEquals",              false, "return " + Content.class.getName() + ".contentEquals(this, arg0);");
		type.overrideMethod("getContextName",             false, "return StringUtils.defaultString(getProperty(AbstractNode.name), \"#text\");");
		type.overrideMethod("doImport",                   false, "return arg0.createTextNode(getData());");
		type.overrideMethod("onCreation",                 true,  "if (getContentType() == null) { setContentType(\"text/plain\"); }");
		type.overrideMethod("onModification",             true,  Content.class.getName() + ".onModification(this, arg0, arg1, arg2);");

		// ----- interface Favoritable -----
		type.overrideMethod("setFavoriteContent",         false, "setContent(arg0);");
		type.overrideMethod("getFavoriteContent",         false, "return getContent();");
		type.overrideMethod("getFavoriteContentType",     false, "return getContentType();");
		type.overrideMethod("getContext",                 false, "if (StringUtils.isNotBlank(getName())) { return getName(); } else { return getPagePath(); }");

		// ----- interface org.w3c.dom.Node -----
		type.overrideMethod("getTextContent",        false, "return getData();");
		type.overrideMethod("setTextContent",        false, "setData(arg0);");
		type.overrideMethod("getLocalName",          false, "return null;");
		type.overrideMethod("getNodeType",           false, "return TEXT_NODE;");
		type.overrideMethod("getNodeName",           false, "return \"#text\";");
		type.overrideMethod("getNodeValue",          false, "return getData();");
		type.overrideMethod("setNodeValue",          false, "setData(arg0);");
		type.overrideMethod("getAttributes",         false, "return null;");
		type.overrideMethod("hasAttributes",         false, "return false;");

		// ----- interface org.w3c.dom.text -----
		type.overrideMethod("getData",                    false, "checkReadAccess(); return getProperty(contentProperty);");
		type.overrideMethod("getWholeText",               false, "throw new UnsupportedOperationException(\"Not supported.\");");
		type.overrideMethod("replaceWholeText",           false, "throw new UnsupportedOperationException(\"Not supported.\");");
		type.overrideMethod("isElementContentWhitespace", false, "return " + Content.class.getName() + ".isElementContentWhitespace(this);");
		type.overrideMethod("splitText",                  false, "return " + Content.class.getName() + ".splitText(this, arg0);");
		type.overrideMethod("getLength",                  false, "return " + Content.class.getName() + ".getLength(this);");
		type.overrideMethod("substringData",              false, "return " + Content.class.getName() + ".substringData(this, arg0, arg1);");
		type.overrideMethod("setData",                    false, Content.class.getName() + ".setData(this, arg0);");
		type.overrideMethod("appendData",                 false, Content.class.getName() + ".appendData(this, arg0);");
		type.overrideMethod("insertData",                 false, Content.class.getName() + ".insertData(this, arg0, arg1);");
		type.overrideMethod("deleteData",                 false, Content.class.getName() + ".deleteData(this, arg0, arg1);");
		type.overrideMethod("replaceData",                false, Content.class.getName() + ".replaceData(this, arg0, arg1, arg2);");

		type.addViewProperty(PropertyView.Public, "isDOMNode");
		type.addViewProperty(PropertyView.Public, "pageId");
		type.addViewProperty(PropertyView.Public, "parent");
		type.addViewProperty(PropertyView.Public, "sharedComponentId");
		type.addViewProperty(PropertyView.Public, "syncedNodesIds");
		type.addViewProperty(PropertyView.Public, "hideConditions");
		type.addViewProperty(PropertyView.Public, "showConditions");
		type.addViewProperty(PropertyView.Public, "hideForLocales");
		type.addViewProperty(PropertyView.Public, "showForLocales");
		type.addViewProperty(PropertyView.Public, "sharedComponentConfiguration");
		type.addViewProperty(PropertyView.Public, "hideOnIndex");
		type.addViewProperty(PropertyView.Public, "hideOnDetail");
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
		type.addViewProperty(PropertyView.Ui, "showForLocales");
		type.addViewProperty(PropertyView.Ui, "hideForLocales");
		type.addViewProperty(PropertyView.Ui, "showConditions");
		type.addViewProperty(PropertyView.Ui, "hideConditions");
		type.addViewProperty(PropertyView.Ui, "dataKey");
		type.addViewProperty(PropertyView.Ui, "cypherQuery");
		type.addViewProperty(PropertyView.Ui, "xpathQuery");
		type.addViewProperty(PropertyView.Ui, "restQuery");
		type.addViewProperty(PropertyView.Ui, "functionQuery");
		type.addViewProperty(PropertyView.Ui, "flow");
	}}

	String getContentType();
	String getContent();
	void setContent(final String content) throws FrameworkException;
	void setContentType(final String contentType) throws FrameworkException;

	static void onModification(final Content thisContent, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		final String uuid = thisContent.getUuid();
		if (uuid != null) {

			// acknowledge all events for this node when it is modified
			RuntimeEventLog.getEvents(e -> uuid.equals(e.getData().get("id"))).stream().forEach(e -> e.acknowledge());
		}

		final PropertyMap map = new PropertyMap();

		// sync content only
		map.put(StructrApp.key(Content.class, "content"),     thisContent.getContent());
		map.put(StructrApp.key(Content.class, "contentType"), thisContent.getContentType());
		map.put(StructrApp.key(Content.class, "name"),        thisContent.getProperty(StructrApp.key(Content.class, "name")));

		for (final DOMNode syncedNode : thisContent.getSyncedNodes()) {

			syncedNode.setProperties(securityContext, map);
		}

		final DOMNode sharedComponent = thisContent.getSharedComponent();
		if (sharedComponent != null) {

			sharedComponent.setProperties(sharedComponent.getSecurityContext(), map);
		}
	}

	public static boolean contentEquals(final Content thisNode, final DOMNode otherNode) {

		if (otherNode instanceof Content) {

			final String content1 = thisNode.getTextContent();
			final String content2 = ((Content) otherNode).getTextContent();

			if (content1 == null && content2 == null) {
				return true;
			}

			if (content1 != null && content2 != null) {

				return content1.equals(content2);
			}
		}

		return false;
	}

	public static void updateFromNode(final Content thisContent, final DOMNode newNode) throws FrameworkException {

		if (newNode instanceof Content) {

			final PropertyKey<String> contentKey = StructrApp.key(Content.class, "content");

			thisContent.setProperties(thisContent.getSecurityContext(), new PropertyMap(contentKey, newNode.getProperty(contentKey)));

		}

	}

	public static void renderContent(final Content thisNode, final RenderContext renderContext, final int depth) throws FrameworkException {

		final SecurityContext securityContext = thisNode.getSecurityContext();

		try {

			final EditMode edit = renderContext.getEditMode(securityContext.getUser(false));
			if (EditMode.DEPLOYMENT.equals(edit)) {

				final AsyncBuffer buf = renderContext.getBuffer();

				// output ownership comments
				DOMNode.renderDeploymentExportComments(thisNode, buf, true);

				// EditMode "deployment" means "output raw content, do not interpret in any way
				buf.append(escapeForHtml(thisNode.getContent()));

				return;
			} else if (EditMode.CONTENT.equals(edit)) {

			}

			if (!thisNode.shouldBeRendered(renderContext)) {
				return;
			}

			final RenderContextContentHandler handler = new RenderContextContentHandler(thisNode, renderContext);
			final String id                           = thisNode.getUuid();
			final AsyncBuffer out                     = renderContext.getBuffer();
			final String _contentType                 = thisNode.getContentType();

			// apply configuration for shared component if present
			final String _sharedComponentConfiguration = thisNode.getSharedComponentConfiguration();
			if (StringUtils.isNotBlank(_sharedComponentConfiguration)) {

				Scripting.evaluate(renderContext, thisNode, "${" + _sharedComponentConfiguration.trim() + "}", "sharedComponentConfiguration", 0, thisNode.getUuid());
			}

			// determine some postprocessing flags
			if (!(EditMode.RAW.equals(edit) || EditMode.WIDGET.equals(edit)) && (_contentType == null || ("text/plain".equals(_contentType)))) {

				handler.setEscapeForHtml(true);
			}

			if (EditMode.CONTENT.equals(edit) && thisNode.isGranted(Permission.write, securityContext)) {

				if ("text/javascript".equals(_contentType)) {

					// Javascript will only be given some local vars
					out.append("// data-structr-type='").append(thisNode.getType()).append("'\n// data-structr-id='").append(id).append("'\n");

				} else if ("text/css".equals(_contentType)) {

					// CSS will only be given some local vars
					out.append("/* data-structr-type='").append(thisNode.getType()).append("'*/\n/* data-structr-id='").append(id).append("'*/\n");

				} else {

					// In edit mode, add an artificial comment tag around content nodes within body to make them editable
					//final String cleanedContent = StringUtils.remove(StringUtils.remove(org.apache.commons.lang3.StringUtils.replace(thisNode.getContent(), "\n", "\\\\n"), "<!--"), "-->");
					final String cleanedContent = org.apache.commons.lang3.StringUtils.replace(thisNode.getContent(), "\n", "\\\\n");
					out.append("<!--data-structr-id=\"".concat(id).concat("\" data-structr-raw-value=\"").concat(escapeForHtmlAttributes(cleanedContent)).concat("\"-->"));
				}
			}

			if (_contentType != null) {

				handler.setConverter(ContentConverters.getConverterForType(_contentType));
			}

			// replace newlines with <br /> for rendering
			if (((_contentType == null) || _contentType.equals("text/plain"))) {

				final DOMNode _parent = thisNode.getParent();
				if (_parent == null || !(_parent instanceof Textarea)) {

					handler.setReplaceNewlines(true);
				}
			}

			// render content with support for async output
			Content.renderContentWithScripts(thisNode.getProperty(StructrApp.key(Content.class, "content")), handler);

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
			final boolean isShadowPage = DOMNode.isSharedComponent(thisNode);

			// catch exception to prevent status 500 error pages in frontend.
			if (!isShadowPage) {

				final DOMNode ownerDocument = thisNode.getOwnerDocumentAsSuperUser();
				DOMNode.logScriptingError(logger, t, "Error while evaluating script in page {}[{}], Content[{}]", ownerDocument.getProperty(AbstractNode.name), ownerDocument.getProperty(AbstractNode.id), thisNode.getUuid());

			} else {

				DOMNode.logScriptingError(logger, t, "Error while evaluating script in shared component, Content[{}]", thisNode.getUuid());
			}
		}
	}

	// ----- interface org.w3c.dom.Text -----
	public static Text splitText(final Content thisNode, final int offset) throws DOMException {

		thisNode.checkWriteAccess();

		final String text = thisNode.getContent();
		if (text != null) {

			int len = text.length();

			if (offset < 0 || offset > len) {

				throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);

			} else {

				final String firstPart  = text.substring(0, offset);
				final String secondPart = text.substring(offset);

				final Document document  = thisNode.getOwnerDocument();
				final Node parent        = thisNode.getParentNode();

				if (document != null && parent != null) {

					try {

						// first part goes into existing text element
						thisNode.setContent(firstPart);

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

	public static boolean isElementContentWhitespace(final Content thisNode) {

		thisNode.checkReadAccess();

		String text = thisNode.getContent();

		if (text != null) {

			return !text.matches("[\\S]*");
		}

		return false;
	}

	public static void setData(final Content thisNode, final String data) throws DOMException {

		thisNode.checkWriteAccess();

		try {

			thisNode.setContent(data);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	public static int getLength(final Content thisNode) {

		final String text = thisNode.getContent();
		if (text != null) {

			return text.length();
		}

		return 0;
	}

	public static String substringData(final Content thisNode, int offset, int count) throws DOMException {

		thisNode.checkReadAccess();

		String text = thisNode.getContent();
		if (text != null) {

			try {

				return text.substring(offset, offset + count);

			} catch (IndexOutOfBoundsException iobex) {

				throw new DOMException(DOMException.INDEX_SIZE_ERR, INDEX_SIZE_ERR_MESSAGE);
			}
		}

		return "";
	}

	public static void appendData(final Content thisNode, final String data) throws DOMException {

		thisNode.checkWriteAccess();

		try {
			final String text = thisNode.getContent();

			thisNode.setContent(text.concat(data));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	public static void insertData(final Content thisNode, final int offset, final String data) throws DOMException {

		thisNode.checkWriteAccess();

		try {

			final String text       = thisNode.getContent();
			final String leftPart   = text.substring(0, offset);
			final String rightPart  = text.substring(offset);
			final StringBuilder buf = new StringBuilder(text.length() + data.length() + 1);

			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			// finally, set content to concatenated left, data and right parts
			thisNode.setContent(buf.toString());

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	public static void deleteData(final Content thisNode, final int offset, final int count) throws DOMException {

		thisNode.checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			final String text      = thisNode.getContent();
			final String leftPart  = text.substring(0, offset);
			final String rightPart = text.substring(offset + count);

			thisNode.setContent(leftPart.concat(rightPart));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	public static void replaceData(final Content thisNode, final int offset, final int count, final String data) throws DOMException {

		thisNode.checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			final String text       = thisNode.getContent();
			final String leftPart   = text.substring(0, offset);
			final String rightPart  = text.substring(offset + count);
			final StringBuilder buf = new StringBuilder(leftPart.length() + data.length() + rightPart.length());

			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			thisNode.setContent(buf.toString());

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

	static interface ContentHandler {

		void handleScript(final String script, final int row, final int column) throws FrameworkException, IOException;
		void handleIncompleteScript(final String script) throws FrameworkException, IOException;
		void handleText(final String text) throws FrameworkException;
		void possibleStartOfScript(final int row, final int column);
	}

	static class RenderContextContentHandler implements ContentHandler {

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
							//StringUtils.toEncodedString((byte[]) value, renderContext.getPage().getProperty(StructrApp.key(Page.class, "contentType")));
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
				final boolean isShadowPage = DOMNode.isSharedComponent(node);

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
