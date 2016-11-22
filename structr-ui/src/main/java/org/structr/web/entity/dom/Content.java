/**
 * Copyright (C) 2010-2016 Structr GmbH
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
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.markup.confluence.ConfluenceDialect;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;
import net.java.textilej.parser.markup.textile.TextileDialect;
import net.java.textilej.parser.markup.trac.TracWikiDialect;
import org.apache.commons.lang3.StringUtils;
import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Asciidoctor.Factory;
import org.pegdown.Parser;
import org.pegdown.PegDownProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Adapter;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.schema.NonIndexed;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import static org.structr.web.entity.dom.DOMNode.hideOnDetail;
import static org.structr.web.entity.dom.DOMNode.hideOnIndex;
import org.structr.web.entity.html.Textarea;
import org.structr.web.entity.relation.Sync;
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
 *
 */
public class Content extends DOMNode implements Text, NonIndexed {

	private static final Logger logger                                                   = LoggerFactory.getLogger(Content.class.getName());
	public static final Property<String> contentType                                     = new StringProperty("contentType").indexed();
	public static final Property<String> content                                         = new StringProperty("content").indexed();
	public static final Property<Boolean> isContent                                      = new ConstantBooleanProperty("isContent", true);

	private static final Map<String, Adapter<String, String>> contentConverters          = new LinkedHashMap<>();

	private static final ThreadLocalAsciiDocProcessor asciiDocProcessor                  = new ThreadLocalAsciiDocProcessor();
	private static final ThreadLocalTracWikiProcessor tracWikiProcessor                  = new ThreadLocalTracWikiProcessor();
	private static final ThreadLocalTextileProcessor textileProcessor                    = new ThreadLocalTextileProcessor();
	private static final ThreadLocalPegDownProcessor pegDownProcessor                    = new ThreadLocalPegDownProcessor();
	private static final ThreadLocalMediaWikiProcessor mediaWikiProcessor                = new ThreadLocalMediaWikiProcessor();
	private static final ThreadLocalConfluenceProcessor confluenceProcessor              = new ThreadLocalConfluenceProcessor();

	public static final org.structr.common.View uiView                                   = new org.structr.common.View(Content.class, PropertyView.Ui,
		content, contentType, parent, pageId, syncedNodes, sharedComponent, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		hideOnDetail, hideOnIndex, showForLocales, hideForLocales, showConditions, hideConditions, isContent, isDOMNode
	);

	public static final org.structr.common.View publicView                               = new org.structr.common.View(Content.class, PropertyView.Public,
		content, contentType, parent, pageId, syncedNodes, sharedComponent, dataKey, restQuery, cypherQuery, xpathQuery, functionQuery,
		hideOnDetail, hideOnIndex, showForLocales, hideForLocales, showConditions, hideConditions, isContent, isDOMNode
	);
	//~--- static initializers --------------------------------------------

	static {

		contentConverters.put("text/markdown", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return pegDownProcessor.get().markdownToHtml(s);
				}

				return "";
			}

		});
		contentConverters.put("text/textile", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return textileProcessor.get().parseToHtml(s);
				}

				return "";

			}

		});
		contentConverters.put("text/mediawiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return mediaWikiProcessor.get().parseToHtml(s);
				}

				return "";
			}

		});
		contentConverters.put("text/tracwiki", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return tracWikiProcessor.get().parseToHtml(s);
				}

				return "";

			}

		});
		contentConverters.put("text/confluence", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return confluenceProcessor.get().parseToHtml(s);
				}

				return "";

			}

		});
		contentConverters.put("text/asciidoc", new Adapter<String, String>() {

			@Override
			public String adapt(String s) throws FrameworkException {

				if (s != null) {
					return asciiDocProcessor.get().render(s, new HashMap<String, Object>());
				}

				return "";

			}

		});
	}

	@Override
	public boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (super.isValid(errorBuffer)) {

			if (getProperty(Content.contentType) == null) {
				setProperty(Content.contentType, "text/plain");
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			for (final Sync rel : getOutgoingRelationships(Sync.class)) {

				final Content syncedNode = (Content) rel.getTargetNode();
				final PropertyMap map    = new PropertyMap();

				// sync content only
				map.put(content, getProperty(content));
				map.put(contentType, getProperty(contentType));
				map.put(name, getProperty(name));

				syncedNode.setProperties(securityContext, map);
			}

			final Sync rel = getIncomingRelationship(Sync.class);
			if (rel != null) {

				final Content otherNode = (Content) rel.getSourceNode();
				if (otherNode != null) {

					final PropertyMap map = new PropertyMap();

					// sync both ways
					map.put(content, getProperty(content));
					map.put(contentType, getProperty(contentType));
					map.put(name, getProperty(name));

					otherNode.setProperties(otherNode.getSecurityContext(), map);
				}
			}

			return true;
		}

		return false;
	}

	@Override
	public boolean contentEquals(DOMNode otherNode) {

		if (otherNode instanceof Content) {

			final String content1 = getTextContent();
			final String content2 = ((Content)otherNode).getTextContent();

			if (content1 == null && content2 == null) {
				return true;
			}

			if (content1 != null && content2 != null) {

				return content1.equals(content2);
			}
		}

		return false;
	}

	@Override
	public void updateFromNode(final DOMNode newNode) throws FrameworkException {

		if (newNode instanceof Content) {

			setProperties(securityContext, new PropertyMap(content, newNode.getProperty(Content.content)));

		}

	}

	@Override
	public String getIdHash() {

		final DOMNode _parent = getProperty(DOMNode.parent);
		if (_parent != null) {

			String dataHash = _parent.getProperty(DOMNode.dataHashProperty);
			if (dataHash == null) {
				dataHash = _parent.getIdHash();
			}

			return dataHash + "Content" + treeGetChildPosition(this);
		}

		return super.getIdHash();
	}

	@Override
	public void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {

		try {
			final EditMode edit = renderContext.getEditMode(securityContext.getUser(false));
			if (EditMode.DEPLOYMENT.equals(edit)) {

				final AsyncBuffer buf = renderContext.getBuffer();

				// output ownership comments
				renderDeploymentExportComments(buf, true);

				// EditMode "deployment" means "output raw content, do not interpret in any way
				buf.append(escapeForHtml(getProperty(Content.content)));

				return;
			}

			if (isDeleted() || isHidden() || !displayForLocale(renderContext) || !displayForConditions(renderContext)) {
				return;
			}

			final String id            = getUuid();
			final boolean inBody       = renderContext.inBody();
			final AsyncBuffer out      = renderContext.getBuffer();

			String _contentType = getProperty(contentType);

			// fetch content with variable replacement
			String _content = getPropertyWithVariableReplacement(renderContext, Content.content);

			if (!(EditMode.RAW.equals(edit) || EditMode.WIDGET.equals(edit)) && (_contentType == null || ("text/plain".equals(_contentType)))) {

				_content = escapeForHtml(_content);

			}

			if (EditMode.CONTENT.equals(edit) && inBody && this.isGranted(Permission.write, securityContext)) {

				if ("text/javascript".equals(_contentType)) {

					// Javascript will only be given some local vars
					out.append("// data-structr-type='").append(getType()).append("'\n// data-structr-id='").append(id).append("'\n");

				} else if ("text/css".equals(_contentType)) {

					// CSS will only be given some local vars
					out.append("/* data-structr-type='").append(getType()).append("'*/\n/* data-structr-id='").append(id).append("'*/\n");

				} else {

					// In edit mode, add an artificial comment tag around content nodes within body to make them editable
					final String cleanedContent = StringUtils.remove(StringUtils.remove(org.apache.commons.lang3.StringUtils.replace(getProperty(Content.content), "\n", "\\\\n"), "<!--"), "-->");
					out.append("<!--data-structr-id=\"".concat(id)
						.concat("\" data-structr-raw-value=\"").concat(escapeForHtmlAttributes(cleanedContent)).concat("\"-->"));

				}

			}

			// examine content type and apply converter
			if (_contentType != null) {

				final Adapter<String, String> converter = contentConverters.get(_contentType);

				if (converter != null) {

					try {

						// apply adapter
						_content = converter.adapt(_content);
					} catch (FrameworkException fex) {

						logger.warn("Unable to convert content: {}", fex.getMessage());

					}

				}

			}

			// replace newlines with <br /> for rendering
			if (((_contentType == null) || _contentType.equals("text/plain")) && (_content != null) && !_content.isEmpty()) {

				final DOMNode _parent = getProperty(Content.parent);
				if (_parent == null || !(_parent instanceof Textarea)) {

					_content = _content.replaceAll("[\\n]{1}", "<br>");
				}
			}

			if (_content != null) {

				// insert whitespace to make element clickable
				if (EditMode.CONTENT.equals(edit) && _content.length() == 0) {
					_content = "--- empty ---";
				}

				out.append(_content);
			}

			if (EditMode.CONTENT.equals(edit) && inBody && !("text/javascript".equals(getProperty(contentType))) && !("text/css".equals(getProperty(contentType)))) {

				out.append("<!---->");
			}

		} catch (Throwable t) {

			// catch exception to prevent ugly status 500 error pages in frontend.
			logger.error("", t);

		}

	}

	@Override
	public boolean isSynced() {
		return false;
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

						// first part goes into existing text element
						setProperties(securityContext, new PropertyMap(content, firstPart));

						// second part goes into new text element
						Text newNode = document.createTextNode(secondPart);

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
		try {
			setProperties(securityContext, new PropertyMap(content, data));

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
	public void appendData(final String data) throws DOMException {

		checkWriteAccess();

		try {
			String text = getProperty(content);
			setProperties(securityContext, new PropertyMap(content, text.concat(data)));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	@Override
	public void insertData(final int offset, final String data) throws DOMException {

		checkWriteAccess();

		try {

			String text = getProperty(content);

			String leftPart  = text.substring(0, offset);
			String rightPart = text.substring(offset);

			StringBuilder buf = new StringBuilder(text.length() + data.length() + 1);
			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			// finally, set content to concatenated left, data and right parts
			setProperties(securityContext, new PropertyMap(content, buf.toString()));


		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	@Override
	public void deleteData(final int offset, final int count) throws DOMException {

		checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			String text = getProperty(content);

			String leftPart  = text.substring(0, offset);
			String rightPart = text.substring(offset + count);

			setProperties(securityContext, new PropertyMap(content, leftPart.concat(rightPart)));

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());

		}
	}

	@Override
	public void replaceData(final int offset, final int count, final String data) throws DOMException {

		checkWriteAccess();

		// finally, set content to concatenated left and right parts
		try {

			String text = getProperty(content);

			String leftPart  = text.substring(0, offset);
			String rightPart = text.substring(offset + count);

			StringBuilder buf = new StringBuilder(leftPart.length() + data.length() + rightPart.length());
			buf.append(leftPart);
			buf.append(data);
			buf.append(rightPart);

			setProperties(securityContext, new PropertyMap(content, buf.toString()));

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

			return new PegDownProcessor(Parser.ALL);

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

	private static class ThreadLocalAsciiDocProcessor extends ThreadLocal<Asciidoctor> {

		@Override
		protected Asciidoctor initialValue() {

			return Factory.create();
		}
	}
}
