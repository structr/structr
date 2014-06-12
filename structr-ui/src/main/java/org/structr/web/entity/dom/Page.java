/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Structr is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr. If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity.dom;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.ftpserver.ftplet.FtpFile;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.Syncable;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.Predicate;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractUser;
import org.structr.core.entity.Principal;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.NodeInterface;
import static org.structr.core.graph.NodeInterface.owner;
import org.structr.core.graph.Tx;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.RelationProperty;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.files.ftp.AbstractStructrFtpFile;
import org.structr.files.ftp.StructrFtpFile;
import org.structr.schema.SchemaHelper;
import org.structr.web.Importer;
import org.structr.web.common.RenderContext;
import org.structr.web.common.StringRenderBuffer;
import org.structr.web.diff.InvertibleModificationOperation;
import org.structr.web.entity.File;
import org.structr.web.entity.Linkable;
import static org.structr.web.entity.Linkable.linkingElements;
import static org.structr.web.entity.dom.DOMNode.children;
import org.structr.web.entity.html.Html;
import org.structr.web.entity.html.relation.ResourceLink;
import org.structr.web.entity.relation.PageLink;
import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

//~--- classes ----------------------------------------------------------------
/**
 * Represents a ownerDocument resource
 *
 * @author Axel Morgner
 * @author Christian Morgner
 */
public class Page extends DOMNode implements Linkable, Document, DOMImplementation, FtpFile {

	private static final Logger logger = Logger.getLogger(Page.class.getName());

	public static final Property<Integer> version = new IntProperty("version").indexed();
	public static final Property<Integer> position = new IntProperty("position").indexed();
	public static final Property<String> contentType = new StringProperty("contentType").indexed();
	public static final Property<Integer> cacheForSeconds = new IntProperty("cacheForSeconds");
	public static final Property<String> showOnErrorCodes = new StringProperty("showOnErrorCodes").indexed();
	public static final Property<List<DOMNode>> elements = new StartNodes<>("elements", PageLink.class);

	public static final org.structr.common.View publicView = new org.structr.common.View(Page.class, PropertyView.Public,
		children, linkingElements, contentType, owner, cacheForSeconds, version, showOnErrorCodes
	);

	public static final org.structr.common.View uiView = new org.structr.common.View(Page.class, PropertyView.Ui,
		children, linkingElements, contentType, owner, cacheForSeconds, version, position, showOnErrorCodes
	);

	private Html5DocumentType docTypeNode = null;

	public Page() {

		docTypeNode = new Html5DocumentType(this);
	}

	//~--- methods --------------------------------------------------------
	@Override
	public boolean contentEquals(DOMNode otherNode) {
		return false;
	}

	@Override
	public void updateFromNode(final DOMNode newNode) throws FrameworkException {
		// do nothing
	}

	@Override
	public void updateFromPropertyMap(final PropertyMap properties) throws FrameworkException {

		if (properties.containsKey(NodeInterface.name)) {

			final String newName = properties.get(NodeInterface.name);
			if (newName != null) {

				setProperty(NodeInterface.name, newName);
			}
		}
	}

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = true;

		valid &= nonEmpty(AbstractNode.name, errorBuffer);
		valid &= super.isValid(errorBuffer);

		return valid;
	}

	@Override
	public boolean flush() {
		return true;
	}

	/**
	 * Creates a new Page entity with the given name in the database.
	 *
	 * @param securityContext the security context to use
	 * @param name the name of the new ownerDocument, defaults to
	 * "ownerDocument" if not set
	 *
	 * @return the new ownerDocument
	 * @throws FrameworkException
	 */
	public static Page createNewPage(SecurityContext securityContext, String name) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);
		final PropertyMap properties = new PropertyMap();

		properties.put(AbstractNode.name, name != null ? name : "page");
		properties.put(AbstractNode.type, Page.class.getSimpleName());
		properties.put(Page.contentType, "text/html");

		return app.create(Page.class, properties);
	}

	@Override
	protected void checkHierarchy(Node otherNode) throws DOMException {

		// verify that this document has only one document element
		if (getDocumentElement() != null) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_DOCUMENT);
		}

		if (!(otherNode instanceof Html)) {

			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, HIERARCHY_REQUEST_ERR_MESSAGE_ELEMENT);
		}

		super.checkHierarchy(otherNode);
	}

	@Override
	public boolean hasChildNodes() {
		return true;
	}

	@Override
	public NodeList getChildNodes() {

		DOMNodeList _children = new DOMNodeList();

		_children.add(docTypeNode);
		_children.addAll(super.getChildNodes());

		return _children;
	}

	@Override
	public Node getFirstChild() {
		return docTypeNode;
	}

	public void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(Page.version);
		if (_version == null) {

			setProperty(Page.version, 1);

		} else {

			setProperty(Page.version, _version + 1);
		}
	}

	@Override
	public Element createElement(final String tag) throws DOMException {

		final String elementType = StringUtils.capitalize(tag);
		final App app = StructrApp.getInstance(securityContext);

		String c = Content.class.getSimpleName();

		// Avoid creating an (invalid) 'Content' DOMElement
		if (elementType == null || c.equals(elementType)) {

			logger.log(Level.WARNING, "Blocked attempt to create a DOMElement of type {0}", c);

			return null;

		}

		final Page _page = this;

		// create new content element
		DOMElement element;
		try {
			final Class entityClass = SchemaHelper.getEntityClassForRawType(elementType);
			if (entityClass != null) {

				element = (DOMElement) app.create(entityClass, new NodeAttribute(DOMElement.tag, tag));
				element.doAdopt(_page);

				return element;
			}

		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return null;

	}

	@Override
	public DocumentFragment createDocumentFragment() {

		final App app = StructrApp.getInstance(securityContext);

		try {

			// create new content element
			org.structr.web.entity.dom.DocumentFragment fragment = app.create(org.structr.web.entity.dom.DocumentFragment.class);

			// create relationship from ownerDocument to new text element
			((RelationProperty<DOMNode>) Page.elements).addSingleElement(securityContext, Page.this, fragment);
			// Page.elements.createRelationship(securityContext, Page.this, fragment);

			return fragment;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			fex.printStackTrace();
		}

		return null;

	}

	@Override
	public Text createTextNode(final String text) {

		final App app = StructrApp.getInstance(securityContext);

		try {

			// create new content element
			Content content = (Content) StructrApp.getInstance(securityContext).command(CreateNodeCommand.class).execute(
				new NodeAttribute(AbstractNode.type, Content.class.getSimpleName()),
				new NodeAttribute(Content.content, text)
			);

			// create relationship from ownerDocument to new text element
			((RelationProperty<DOMNode>) Page.elements).addSingleElement(securityContext, Page.this, content);
			//Page.elements.createRelationship(securityContext, Page.this, content);

			return content;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			fex.printStackTrace();
		}

		return null;
	}

	@Override
	public Comment createComment(String comment) {

		final App app = StructrApp.getInstance(securityContext);

		try {

			// create new content element
			org.structr.web.entity.dom.Comment commentNode = (org.structr.web.entity.dom.Comment) StructrApp.getInstance(securityContext).command(CreateNodeCommand.class).execute(
				new NodeAttribute(AbstractNode.type, org.structr.web.entity.dom.Comment.class.getSimpleName()),
				new NodeAttribute(Content.content, comment)
			);

			// create relationship from ownerDocument to new text element
			((RelationProperty<DOMNode>) Page.elements).addSingleElement(securityContext, Page.this, commentNode);
			//Page.elements.createRelationship(securityContext, Page.this, content);

			return commentNode;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			fex.printStackTrace();
		}

		return null;
	}

	@Override
	public CDATASection createCDATASection(String string) throws DOMException {

		final App app = StructrApp.getInstance(securityContext);

		try {

			// create new content element
			Cdata content = (Cdata) StructrApp.getInstance(securityContext).command(CreateNodeCommand.class).execute(
				new NodeAttribute(AbstractNode.type, Cdata.class.getSimpleName())
			);

			// create relationship from ownerDocument to new text element
			((RelationProperty<DOMNode>) Page.elements).addSingleElement(securityContext, Page.this, content);
			//Page.elements.createRelationship(securityContext, Page.this, content);

			return content;

		} catch (FrameworkException fex) {

			// FIXME: what to do with the exception here?
			fex.printStackTrace();
		}

		return null;
	}

	@Override
	public ProcessingInstruction createProcessingInstruction(String string, String string1) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Attr createAttribute(String name) throws DOMException {
		return new DOMAttribute(this, null, name, null);
	}

	@Override
	public EntityReference createEntityReference(String string) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Element createElementNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported");
	}

	@Override
	public Attr createAttributeNS(String string, String string1) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported");
	}

	@Override
	public Node importNode(final Node node, final boolean deep) throws DOMException {
		return importNode(node, deep, true);
	}

	private Node importNode(final Node node, final boolean deep, final boolean removeParentFromSourceNode) throws DOMException {

		if (node instanceof DOMNode) {

			final DOMNode domNode = (DOMNode) node;

			// step 1: use type-specific import impl.
			Node importedNode = domNode.doImport(Page.this);

			// step 2: do recursive import?
			if (deep && domNode.hasChildNodes()) {

				// FIXME: is it really a good idea to do the
				// recursion inside of a transaction?
				Node child = domNode.getFirstChild();

				while (child != null) {

					// do not remove parent for child nodes
					importNode(child, deep, false);
					child = child.getNextSibling();

					logger.log(Level.INFO, "sibling is {0}", child);
				}

			}

			// step 3: remove node from its current parent
			// (Note that this step needs to be done last in
			// (order for the child to be able to find its
			// siblings.)
			if (removeParentFromSourceNode) {

				// only do this for the actual source node, do not remove
				// child nodes from its parents
				Node _parent = domNode.getParentNode();
				if (_parent != null) {
					_parent.removeChild(domNode);
				}
			}

			return importedNode;

		}

		return null;
	}

	@Override
	public Node adoptNode(Node node) throws DOMException {
		return adoptNode(node, true);
	}

	private Node adoptNode(final Node node, final boolean removeParentFromSourceNode) throws DOMException {

		if (node instanceof DOMNode) {

			final DOMNode domNode = (DOMNode) node;

			// step 2: do recursive import?
			if (domNode.hasChildNodes()) {

				Node child = domNode.getFirstChild();
				while (child != null) {

					// do not remove parent for child nodes
					adoptNode(child, false);
					child = child.getNextSibling();
				}

			}

			// step 3: remove node from its current parent
			// (Note that this step needs to be done last in
			// (order for the child to be able to find its
			// siblings.)
			if (removeParentFromSourceNode) {

				// only do this for the actual source node, do not remove
				// child nodes from its parents
				Node _parent = domNode.getParentNode();
				if (_parent != null) {
					_parent.removeChild(domNode);
				}
			}

			// step 1: use type-specific adopt impl.
			Node adoptedNode = domNode.doAdopt(Page.this);

			return adoptedNode;

		}

		return null;
	}

	@Override
	public void normalizeDocument() {
		normalize();
	}

	@Override
	public Node renameNode(Node node, String string, String string1) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_RENAME);
	}

	@Override
	public DocumentType createDocumentType(String string, String string1, String string2) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public Document createDocument(String string, String string1, DocumentType dt) throws DOMException {

		throw new UnsupportedOperationException("Not supported yet.");

	}

	@Override
	public String toString() {

		return getClass().getSimpleName() + " " + getName() + " [" + getUuid() + "] (" + getTextContent() + ")";
	}

	/**
	 * Return the content of this page depending on edit mode
	 *
	 * @param editMode
	 * @return
	 * @throws FrameworkException
	 */
	public String getContent(final RenderContext.EditMode editMode) throws FrameworkException {

		final RenderContext ctx = new RenderContext(null, null, editMode, Locale.GERMAN);
		final StringRenderBuffer buffer = new StringRenderBuffer();
		ctx.setBuffer(buffer);
		render(securityContext, ctx, 0);

		// extract source
		return buffer.getBuffer().toString();

	}

	//~--- get methods ----------------------------------------------------
	@Override
	public short getNodeType() {

		return Element.DOCUMENT_NODE;
	}

	@Override
	public DOMImplementation getImplementation() {

		return this;
	}

	@Override
	public Element getDocumentElement() {
		return (Element) super.getFirstChild();
	}

	@Override
	public Element getElementById(final String id) {

		DOMNodeList results = new DOMNodeList();

		collectNodesByPredicate(this, results, new Predicate<Node>() {

			@Override
			public boolean evaluate(SecurityContext securityContext, Node... obj) {

				if (obj[0] instanceof DOMElement) {

					DOMElement elem = (DOMElement) obj[0];

					if (id.equals(elem.getProperty(DOMElement._id))) {
						return true;
					}
				}

				return false;
			}

		}, 0, true);

		// return first result
		if (results.getLength() == 1) {
			return (DOMElement) results.item(0);
		}

		return null;
	}

	@Override
	public String getInputEncoding() {
		return null;
	}

	@Override
	public String getXmlEncoding() {
		return "UTF-8";
	}

	@Override
	public boolean getXmlStandalone() {
		return true;
	}

	@Override
	public String getXmlVersion() {
		return "1.0";
	}

	@Override
	public boolean getStrictErrorChecking() {
		return true;
	}

	@Override
	public String getDocumentURI() {
		return null;
	}

	@Override
	public DOMConfiguration getDomConfig() {
		return null;
	}

	@Override
	public DocumentType getDoctype() {
		return new Html5DocumentType(this);
	}

	@Override
	public void render(SecurityContext securityContext, RenderContext renderContext, int depth) throws FrameworkException {

		renderContext.setPage(this);

		renderContext.getBuffer().append("<!DOCTYPE html>\n");

		// Skip DOCTYPE node
		DOMNode subNode = (DOMNode) this.getFirstChild().getNextSibling();

		while (subNode != null) {

			if (subNode.isNotDeleted() && securityContext.isVisible(subNode)) {

				subNode.render(securityContext, renderContext, depth);
			}

			subNode = (DOMNode) subNode.getNextSibling();

		}

	}

	@Override
	public boolean hasFeature(String string, String string1) {
		return false;
	}

	@Override
	public boolean isSynced() {
		return false;
	}

	//~--- set methods ----------------------------------------------------
	@Override
	public void setXmlStandalone(boolean bln) throws DOMException {
	}

	@Override
	public void setXmlVersion(String string) throws DOMException {
	}

	@Override
	public void setStrictErrorChecking(boolean bln) {
	}

	@Override
	public void setDocumentURI(String string) {
	}

	// ----- interface org.w3c.dom.Node -----
	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public String getNodeName() {
		return "#document";
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String string) throws DOMException {
	}

	@Override
	public NamedNodeMap getAttributes() {
		return null;
	}

	@Override
	public boolean hasAttributes() {
		return false;
	}

	@Override
	public NodeList getElementsByTagName(final String tagName) {

		DOMNodeList results = new DOMNodeList();

		collectNodesByPredicate(this, results, new Predicate<Node>() {

			@Override
			public boolean evaluate(SecurityContext securityContext, Node... obj) {

				if (obj[0] instanceof DOMElement) {

					DOMElement elem = (DOMElement) obj[0];

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
	public NodeList getElementsByTagNameNS(String string, String tagName) {
		throw new UnsupportedOperationException("Namespaces not supported.");
	}

	// ----- interface DOMAdoptable -----
	@Override
	public Node doAdopt(Page page) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_ADOPT_DOC);
	}

	// ----- interface DOMImportable -----
	@Override
	public Node doImport(Page newPage) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, NOT_SUPPORTED_ERR_MESSAGE_IMPORT_DOC);
	}

	@Override
	public String getPath() {
		return "/".concat(getProperty(name));
	}

	// ----- diff methods -----
	@Export
	public void diff(final String file) throws FrameworkException {

		final App app = StructrApp.getInstance(securityContext);

		try (final Tx tx = app.tx()) {

			final String source = IOUtils.toString(new FileInputStream(file));
			final List<InvertibleModificationOperation> changeSet = new LinkedList<>();
			final Page diffPage = Importer.parsePageFromSource(securityContext, source, this.getProperty(Page.name) + "diff");

			// build change set
			changeSet.addAll(Importer.diffPages(this, diffPage));

			for (final InvertibleModificationOperation op : changeSet) {

				System.out.println(op);

				op.apply(app, this, diffPage);
			}

			// delete remaining children
			for (final DOMNode child : diffPage.getProperty(Page.elements)) {
				app.delete(child);
			}

			// delete imported page
			app.delete(diffPage);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	// ----- interface FtpFile -----
	@Override
	public String getAbsolutePath() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			String path = getName();
			return path;
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error in getName() of abstract ftp file", fex);
		}
		return null;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isFile() {
		return true;
	}

	@Override
	public boolean doesExist() {
		return true;
	}

	@Override
	public boolean isReadable() {
		return true;
	}

	@Override
	public boolean isWritable() {
		return true;
	}

	@Override
	public boolean isRemovable() {
		return true;
	}

	private Principal getOwner() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			Principal owner = getProperty(File.owner);
			return owner;
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error while getting owner of " + this, fex);
		}
		return null;
	}

	@Override
	public String getOwnerName() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			Principal owner = getOwner();
			return owner != null ? owner.getProperty(AbstractUser.name) : "";
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error while getting owner name of " + this, fex);
		}
		return null;
	}

	@Override
	public String getGroupName() {
		try (Tx tx = StructrApp.getInstance().tx()) {

			Principal owner = getOwner();

			if (owner != null) {
				List<Principal> parents = owner.getParents();
				if (!parents.isEmpty()) {

					return parents.get(0).getProperty(AbstractNode.name);

				}
			}

		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error while getting group name of " + this, fex);
		}

		return "";
	}

	@Override
	public int getLinkCount() {
		return 1;
	}

	@Override
	public long getLastModified() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			return getProperty(lastModifiedDate).getTime();
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error while last modified date of " + this, fex);
		}
		return 0L;
	}

	@Override
	public boolean setLastModified(long time) {
		try (Tx tx = StructrApp.getInstance().tx()) {
			setProperty(lastModifiedDate, new Date(time));
			tx.success();
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return true;
	}

	@Override
	public long getSize() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			return getContent(RenderContext.EditMode.RAW).length();
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error while last modified date of " + this, fex);
		}
		return 0L;
	}

	@Override
	public String getName() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			return getProperty(name);
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error in getName() of page", fex);
		}
		return null;
	}

	@Override
	public boolean isHidden() {
		try (Tx tx = StructrApp.getInstance().tx()) {
			return getProperty(hidden);
		} catch (FrameworkException fex) {
			logger.log(Level.SEVERE, "Error in isHidden() of page", fex);
		}
		return true;
	}

	@Override
	public boolean mkdir() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public boolean delete() {
		final App app = StructrApp.getInstance();

		try (Tx tx = StructrApp.getInstance().tx()) {
			app.delete(this);
			tx.success();
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return true;
	}

	@Override
	public boolean move(FtpFile target) {
		try (Tx tx = StructrApp.getInstance().tx()) {

			logger.log(Level.INFO, "move()");

			final AbstractStructrFtpFile targetFile = (AbstractStructrFtpFile) target;
			final String path = targetFile instanceof StructrFtpFile ? "/" : targetFile.getAbsolutePath();

			try {

				if (!("/".equals(path))) {
					final String newName = path.contains("/") ? StringUtils.substringAfterLast(path, "/") : path;
					setProperty(AbstractNode.name, newName);
				}

			} catch (FrameworkException ex) {
				logger.log(Level.SEVERE, "Could not move ftp file", ex);
				return false;
			}

			tx.success();

			return true;
		} catch (FrameworkException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return false;
	}

	@Override
	public List<FtpFile> listFiles() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public OutputStream createOutputStream(long offset) throws IOException {

		final Page origPage = this;

		OutputStream out = new ByteArrayOutputStream() {

			@Override
			public void flush() throws IOException {


				final String source = toString();

				final App app = StructrApp.getInstance();
				try (Tx tx = app.tx()) {

					// parse page from modified source
					Page modifiedPage = Importer.parsePageFromSource(securityContext, source, "__FTP_Temporary_Page__");

					final List<InvertibleModificationOperation> changeSet = Importer.diffPages(origPage, modifiedPage);

					for (final InvertibleModificationOperation op : changeSet) {

						// execute operation
						op.apply(app, origPage, modifiedPage);

					}

					app.delete(modifiedPage);

					tx.success();

				} catch (FrameworkException fex) {
					fex.printStackTrace();
				}

				super.flush();

			}

		};

		return out;
	}

	@Override
	public InputStream createInputStream(long offset) throws IOException {
		try (Tx tx = StructrApp.getInstance().tx()) {
			return new ByteArrayInputStream(getContent(RenderContext.EditMode.RAW).getBytes("UTF-8"));
		} catch (FrameworkException fex) {
		}
		return null;
	}

	// ----- interface Syncable -----
	@Override
	public List<Syncable> getSyncData() {

		final List<Syncable> data = super.getSyncData();

		data.addAll(getProperty(Linkable.linkingElements));

		for (final ResourceLink link : getRelationships(ResourceLink.class)) {
			data.add(link);
		}

		return data;
	}
}
