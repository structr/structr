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
package org.structr.web.traits.definitions.dom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.event.RuntimeEventLog;
import org.structr.core.Adapter;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.entity.Relation;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.RelationshipTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.AbstractNodeTraitDefinition;
import org.structr.core.traits.definitions.NodeInterfaceTraitDefinition;
import org.structr.core.traits.operations.FrameworkMethod;
import org.structr.core.traits.operations.LifecycleMethod;
import org.structr.core.traits.operations.graphobject.OnCreation;
import org.structr.core.traits.operations.graphobject.OnModification;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.converter.ContentConverters;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.dom.DOMNode;
import org.structr.web.entity.dom.Page;
import org.structr.web.traits.operations.*;
import org.structr.web.traits.wrappers.dom.ContentTraitWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

/**
 * Represents a content node. This class implements the org.w3c.dom.Text interface.
 * All methods in the W3C Text interface are based on the raw database content.
 */
public class ContentTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String CONTENT_PROPERTY      = "content";
	public static final String CONTENT_TYPE_PROPERTY = "contentType";
	public static final String IS_CONTENT_PROPERTY   = "isContent";

	public ContentTraitDefinition() {
		super(StructrTraits.CONTENT);
	}

	@Override
	public Map<Class, LifecycleMethod> getLifecycleMethods() {

		return Map.of(

			OnCreation.class,
			new OnCreation() {

				@Override
				public void onCreation(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

					final Content content = ((NodeInterface) obj).as(Content.class);

					if (content.getContentType() == null) {
						content.setContentType("text/plain");
					}
				}
			},

			OnModification.class,
			new OnModification() {

				@Override
				public void onModification(final GraphObject obj, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

					final Content content = ((NodeInterface) obj).as(Content.class);
					final DOMNode domNode = content.as(DOMNode.class);

					// acknowledge all events for this node when it is modified
					RuntimeEventLog.acknowledgeAllEventsForId(content.getUuid());

					final PropertyMap map = new PropertyMap();
					final Traits traits   = obj.getTraits();

					// sync content only
					map.put(traits.key(ContentTraitDefinition.CONTENT_PROPERTY),      content.getContent());
					map.put(traits.key(ContentTraitDefinition.CONTENT_TYPE_PROPERTY), content.getContentType());
					map.put(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY),   obj.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY)));

					for (final DOMNode syncedNode : domNode.getSyncedNodes()) {

						syncedNode.setProperties(securityContext, map);
					}

					final DOMNode sharedComponent = domNode.getSharedComponent();
					if (sharedComponent != null) {

						sharedComponent.setProperties(sharedComponent.getSecurityContext(), map);
					}
				}
			}
		);
	}

	@Override
	public Map<Class, FrameworkMethod> getFrameworkMethods() {

		return Map.of(

			UpdateFromNode.class,
			new UpdateFromNode() {
				@Override
				public void updateFromNode(final NodeInterface node, final DOMNode otherNode) throws FrameworkException {

					if (otherNode.getTraits().contains(StructrTraits.CONTENT)) {

						final Content thisNode = node.as(Content.class);
						final Content content = otherNode.as(Content.class);

						thisNode.setContent(content.getContent());
					}
				}
			},

			DoImport.class,
			new DoImport() {

				@Override
				public DOMNode doImport(final DOMNode node, final Page page) {
					return page.createTextNode(node.as(Content.class).getContent());
				}
			},

			GetContextName.class,
			new GetContextName() {

				@Override
				public String getContextName(final NodeInterface node) {

					final Traits traits = node.getTraits();

					return StringUtils.defaultString(node.getProperty(traits.key(NodeInterfaceTraitDefinition.NAME_PROPERTY)), "#text");
				}
			},

			RenderContent.class,
			new RenderContent() {

				@Override
				public void renderContent(final DOMNode node, final RenderContext renderContext, final int depth) throws FrameworkException {

					final SecurityContext securityContext = node.getSecurityContext();
					final Content content = node.as(Content.class);

					try {

						final EditMode edit = renderContext.getEditMode(securityContext.getUser(false));
						if (EditMode.DEPLOYMENT.equals(edit)) {

							final AsyncBuffer buf = renderContext.getBuffer();

							// output ownership comments
							node.renderDeploymentExportComments(buf, true);

							// EditMode "deployment" means "output raw content, do not interpret in any way
							buf.append(DOMNode.escapeForHtml(content.getContent()));

							return;
						} else if (EditMode.CONTENT.equals(edit)) {

						}

						if (!node.shouldBeRendered(renderContext)) {
							return;
						}

						final RenderContextContentHandler handler = new RenderContextContentHandler(content, renderContext);
						final String id = node.getUuid();
						final AsyncBuffer out = renderContext.getBuffer();
						final String _contentType = content.getContentType();

						// apply configuration for shared component if present
						final String _sharedComponentConfiguration = node.getSharedComponentConfiguration();
						if (StringUtils.isNotBlank(_sharedComponentConfiguration)) {

							Scripting.evaluate(renderContext, node, "${" + _sharedComponentConfiguration.trim() + "}", "sharedComponentConfiguration", 0, node.getUuid());
						}

						// determine some postprocessing flags
						if (!(EditMode.RAW.equals(edit) || EditMode.WIDGET.equals(edit)) && (_contentType == null || ("text/plain".equals(_contentType)))) {

							handler.setEscapeForHtml(true);
						}

						if (EditMode.CONTENT.equals(edit) && node.isGranted(Permission.write, securityContext)) {

							if ("text/javascript".equals(_contentType)) {

								// Javascript will only be given some local vars
								out.append("// data-structr-type='").append(node.getType()).append("'\n// data-structr-id='").append(id).append("'\n");

							} else if ("text/css".equals(_contentType)) {

								// CSS will only be given some local vars
								out.append("/* data-structr-type='").append(node.getType()).append("'*/\n/* data-structr-id='").append(id).append("'*/\n");

							} else {

								// In edit mode, add an artificial comment tag around content nodes within body to make them editable
								//final String cleanedContent = StringUtils.remove(StringUtils.remove(org.apache.commons.lang3.StringUtils.replace(this.getContent(), "\n", "\\\\n"), "<!--"), "-->");
								final String cleanedContent = StringUtils.replace(content.getContent(), "\n", "\\\\n");
								out.append("<!--data-structr-id=\"".concat(id).concat("\" data-structr-raw-value=\"").concat(DOMNode.escapeForHtmlAttributes(cleanedContent)).concat("\"-->"));
							}
						}

						if (_contentType != null) {

							handler.setConverter(ContentConverters.getConverterForType(_contentType));
						}

						// replace newlines with <br /> for rendering
						if (((_contentType == null) || _contentType.equals("text/plain"))) {

							final DOMNode _parent = node.getParent();
							if (_parent == null || !(_parent.is("Textarea"))) {

								handler.setReplaceNewlines(true);
							}
						}

						// render content with support for async output
						renderContentWithScripts(content.getContent(), handler);

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

						final Logger logger = LoggerFactory.getLogger(Content.class);
						final boolean isShadowPage = node.isSharedComponent();

						// catch exception to prevent status 500 error pages in frontend.
						if (!isShadowPage) {

							final DOMNode ownerDocument = node.getOwnerDocumentAsSuperUser();
							DOMNode.logScriptingError(logger, t, "Error while evaluating script in page {}[{}], Content[{}]", ownerDocument.getName(), ownerDocument.getUuid(), node.getUuid());

						} else {

							DOMNode.logScriptingError(logger, t, "Error while evaluating script in shared component, Content[{}]", node.getUuid());
						}
					}
				}
			},

			GetNodeValue.class,
			new GetNodeValue() {

				@Override
				public String getNodeValue(final NodeInterface node) {
					return node.getProperty(node.getTraits().key(CONTENT_PROPERTY));
				}
			},

			ContentEquals.class,
			new ContentEquals() {

				@Override
				public boolean contentEquals(final DOMNode thisNode, final DOMNode otherNode) {

					if (otherNode.is(StructrTraits.CONTENT)) {

						final String content1 = thisNode.as(Content.class).getContent();
						final String content2 = otherNode.as(Content.class).getContent();

						if (content1 == null && content2 == null) {
							return true;
						}

						if (content1 != null && content2 != null) {

							return content1.equals(content2);
						}
					}

					return false;

				}
			}
		);
	}

	@Override
	public Map<Class, RelationshipTraitFactory> getRelationshipTraitFactories() {
		return Map.of();
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Content.class, (traits, node) -> new ContentTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<AbstractMethod> getDynamicMethods() {
		return Set.of();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> contentProperty     = new StringProperty(CONTENT_PROPERTY);
		final Property<String> contentTypeProperty = new StringProperty(CONTENT_TYPE_PROPERTY).indexed();
		final Property<Boolean> isContentProperty  = new ConstantBooleanProperty(IS_CONTENT_PROPERTY, true);

		return Set.of(
			contentProperty,
			contentTypeProperty,
			isContentProperty
		);
	}

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(
			PropertyView.Public,
			newSet(
					IS_CONTENT_PROPERTY, CONTENT_TYPE_PROPERTY, CONTENT_PROPERTY, DOMNodeTraitDefinition.IS_DOM_NODE_PROPERTY,
					DOMNodeTraitDefinition.PAGE_ID_PROPERTY, DOMNodeTraitDefinition.PARENT_PROPERTY, DOMNodeTraitDefinition.SHARED_COMPONENT_ID_PROPERTY,
					DOMNodeTraitDefinition.SYNCED_NODES_IDS_PROPERTY, DOMNodeTraitDefinition.SHARED_COMPONENT_CONFIGURATION_PROPERTY,
					DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY, DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY, DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY,
					DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY, DOMNodeTraitDefinition.DATA_KEY_PROPERTY, DOMNodeTraitDefinition.CYPHER_QUERY_PROPERTY,
					DOMNodeTraitDefinition.REST_QUERY_PROPERTY, DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					IS_CONTENT_PROPERTY, CONTENT_TYPE_PROPERTY, CONTENT_PROPERTY, DOMNodeTraitDefinition.SHARED_COMPONENT_CONFIGURATION_PROPERTY,
					DOMNodeTraitDefinition.IS_DOM_NODE_PROPERTY, DOMNodeTraitDefinition.PAGE_ID_PROPERTY, DOMNodeTraitDefinition.PARENT_PROPERTY,
					DOMNodeTraitDefinition.SHARED_COMPONENT_ID_PROPERTY, DOMNodeTraitDefinition.SYNCED_NODES_IDS_PROPERTY,
					DOMNodeTraitDefinition.SHOW_FOR_LOCALES_PROPERTY, DOMNodeTraitDefinition.HIDE_FOR_LOCALES_PROPERTY, DOMNodeTraitDefinition.SHOW_CONDITIONS_PROPERTY,
					DOMNodeTraitDefinition.HIDE_CONDITIONS_PROPERTY, DOMNodeTraitDefinition.DATA_KEY_PROPERTY, DOMNodeTraitDefinition.CYPHER_QUERY_PROPERTY,
					DOMNodeTraitDefinition.REST_QUERY_PROPERTY, DOMNodeTraitDefinition.FUNCTION_QUERY_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}

	// ----- private methods -----
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
				final boolean isShadowPage = node.as(DOMNode.class).isSharedComponent();

				// catch exception to prevent status 500 error pages in frontend.
				if (!isShadowPage) {

					final DOMNode ownerDocument = node.as(DOMNode.class).getOwnerDocumentAsSuperUser();
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
