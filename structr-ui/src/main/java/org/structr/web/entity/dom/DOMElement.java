/**
 * Copyright (C) 2010-2017 Structr GmbH
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

import java.net.URI;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public interface DOMElement extends DOMNode, Element, NamedNodeMap, NonIndexed {

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("DOMElement");

		type.setImplements(URI.create("https://structr.org/v1.1/definitions/DOMElement"));
		type.setExtends(URI.create("#/definitions/DOMNode"));

	}}

	String getOffsetAttributeName(final String name, final int offset);

	/*

	private static final Logger logger = LoggerFactory.getLogger(DOMElement.class.getName());
	private static final int HtmlPrefixLength = PropertyView.Html.length();

	private static final String STRUCTR_ACTION_PROPERTY = "data-structr-action";

	private static final Map<String, HtmlProperty> htmlProperties = new LRUMap(1000);	// use LURMap here to avoid infinite growing
	private static final String lowercaseBodyName = Body.class.getSimpleName().toLowerCase();

	public static final Property<String> tag              = new StringProperty("tag").indexed().category(PAGE_CATEGORY);
 	public static final Property<String> path             = new StringProperty("path").indexed();
	public static final Property<String> partialUpdateKey = new StringProperty("partialUpdateKey").indexed();

	// Event-handler attributes
	public static final Property<String> _onabort = new HtmlProperty("onabort");
	public static final Property<String> _onblur = new HtmlProperty("onblur");
	public static final Property<String> _oncanplay = new HtmlProperty("oncanplay");
	public static final Property<String> _oncanplaythrough = new HtmlProperty("oncanplaythrough");
	public static final Property<String> _onchange = new HtmlProperty("onchange");
	public static final Property<String> _onclick = new HtmlProperty("onclick");
	public static final Property<String> _oncontextmenu = new HtmlProperty("oncontextmenu");
	public static final Property<String> _ondblclick = new HtmlProperty("ondblclick");
	public static final Property<String> _ondrag = new HtmlProperty("ondrag");
	public static final Property<String> _ondragend = new HtmlProperty("ondragend");
	public static final Property<String> _ondragenter = new HtmlProperty("ondragenter");
	public static final Property<String> _ondragleave = new HtmlProperty("ondragleave");
	public static final Property<String> _ondragover = new HtmlProperty("ondragover");
	public static final Property<String> _ondragstart = new HtmlProperty("ondragstart");
	public static final Property<String> _ondrop = new HtmlProperty("ondrop");
	public static final Property<String> _ondurationchange = new HtmlProperty("ondurationchange");
	public static final Property<String> _onemptied = new HtmlProperty("onemptied");
	public static final Property<String> _onended = new HtmlProperty("onended");
	public static final Property<String> _onerror = new HtmlProperty("onerror");
	public static final Property<String> _onfocus = new HtmlProperty("onfocus");
	public static final Property<String> _oninput = new HtmlProperty("oninput");
	public static final Property<String> _oninvalid = new HtmlProperty("oninvalid");
	public static final Property<String> _onkeydown = new HtmlProperty("onkeydown");
	public static final Property<String> _onkeypress = new HtmlProperty("onkeypress");
	public static final Property<String> _onkeyup = new HtmlProperty("onkeyup");
	public static final Property<String> _onload = new HtmlProperty("onload");
	public static final Property<String> _onloadeddata = new HtmlProperty("onloadeddata");
	public static final Property<String> _onloadedmetadata = new HtmlProperty("onloadedmetadata");
	public static final Property<String> _onloadstart = new HtmlProperty("onloadstart");
	public static final Property<String> _onmousedown = new HtmlProperty("onmousedown");
	public static final Property<String> _onmousemove = new HtmlProperty("onmousemove");
	public static final Property<String> _onmouseout = new HtmlProperty("onmouseout");
	public static final Property<String> _onmouseover = new HtmlProperty("onmouseover");
	public static final Property<String> _onmouseup = new HtmlProperty("onmouseup");
	public static final Property<String> _onmousewheel = new HtmlProperty("onmousewheel");
	public static final Property<String> _onpause = new HtmlProperty("onpause");
	public static final Property<String> _onplay = new HtmlProperty("onplay");
	public static final Property<String> _onplaying = new HtmlProperty("onplaying");
	public static final Property<String> _onprogress = new HtmlProperty("onprogress");
	public static final Property<String> _onratechange = new HtmlProperty("onratechange");
	public static final Property<String> _onreadystatechange = new HtmlProperty("onreadystatechange");
	public static final Property<String> _onreset = new HtmlProperty("onreset");
	public static final Property<String> _onscroll = new HtmlProperty("onscroll");
	public static final Property<String> _onseeked = new HtmlProperty("onseeked");
	public static final Property<String> _onseeking = new HtmlProperty("onseeking");
	public static final Property<String> _onselect = new HtmlProperty("onselect");
	public static final Property<String> _onshow = new HtmlProperty("onshow");
	public static final Property<String> _onstalled = new HtmlProperty("onstalled");
	public static final Property<String> _onsubmit = new HtmlProperty("onsubmit");
	public static final Property<String> _onsuspend = new HtmlProperty("onsuspend");
	public static final Property<String> _ontimeupdate = new HtmlProperty("ontimeupdate");
	public static final Property<String> _onvolumechange = new HtmlProperty("onvolumechange");
	public static final Property<String> _onwaiting = new HtmlProperty("onwaiting");

	// needed for Importer
	public static final Property<String> _data = new HtmlProperty("data").indexed();

	// Edit-mode attributes
	public static final Property<Boolean> _reload = new BooleanProperty("data-structr-reload").category(EDIT_MODE_BINDING_CATEGORY).hint("If active, the page will refresh after a successfull action.");
	public static final Property<Boolean> _confirm = new BooleanProperty("data-structr-confirm").category(EDIT_MODE_BINDING_CATEGORY).hint("If active, a user has to confirm the action.");
	public static final Property<Boolean> _appendId = new BooleanProperty("data-structr-append-id").category(EDIT_MODE_BINDING_CATEGORY).hint("On create, append ID of first created object to the return URI.");
	public static final Property<String> _action = new StringProperty("data-structr-action").category(EDIT_MODE_BINDING_CATEGORY).hint("The action of the dynamic form (e.g create:&lt;Type&gt; | delete:&lt;Type&gt; | edit | login | logout)");
	public static final Property<String> _attributes = new StringProperty("data-structr-attributes").category(EDIT_MODE_BINDING_CATEGORY).hint("The names of the properties that should be included in the request. (for create, edit/save, login or registration actions)");
	public static final Property<String> _attr = new StringProperty("data-structr-attr").category(EDIT_MODE_BINDING_CATEGORY).hint("If this is set, the input field is rendered in auto-edit mode");
	public static final Property<String> _fieldName = new StringProperty("data-structr-name").category(EDIT_MODE_BINDING_CATEGORY).hint("The name of the property (for create/save actions with custom form)");
	public static final Property<String> _hide = new StringProperty("data-structr-hide").category(EDIT_MODE_BINDING_CATEGORY).hint("Which mode (if any) the element should be hidden from the user (eg. edit | non-edit | edit,non-edit)");
	public static final Property<String> _rawValue = new StringProperty("data-structr-raw-value").category(EDIT_MODE_BINDING_CATEGORY).hint("The unformatted value of the element. Provide this if the value of the element is printed with a format applied (useful for Date or Number fields)");

	public static final Property<String> _placeholder = new StringProperty("data-structr-placeholder").category(EDIT_MODE_BINDING_CATEGORY).hint("used to display option labels (default: name)");
	public static final Property<String> _type = new StringProperty("data-structr-type").category(EDIT_MODE_BINDING_CATEGORY).hint("Type hint for the attribute (e.g. Date, Boolean; default: String)");
	public static final Property<String> _customOptionsQuery = new StringProperty("data-structr-custom-options-query").category(EDIT_MODE_BINDING_CATEGORY).hint("Custom REST query for value options (for collection properties)");
	public static final Property<String> _optionsKey = new StringProperty("data-structr-options-key").category(EDIT_MODE_BINDING_CATEGORY).hint("Key used to display option labels for collection properties (default: name)");
	public static final Property<String> _editClass = new StringProperty("data-structr-edit-class").category(EDIT_MODE_BINDING_CATEGORY).hint("Custom CSS class in edit mode");
	public static final Property<String> _returnURI = new StringProperty("data-structr-return").category(EDIT_MODE_BINDING_CATEGORY).hint("Return URI after successful action");

	// Core attributes
	public static final Property<String> _accesskey = new HtmlProperty("accesskey").indexed();
	public static final Property<String> _class = new HtmlProperty("class").indexed();
	public static final Property<String> _contenteditable = new HtmlProperty("contenteditable");
	public static final Property<String> _contextmenu = new HtmlProperty("contextmenu");
	public static final Property<String> _dir = new HtmlProperty("dir");
	public static final Property<String> _draggable = new HtmlProperty("draggable");
	public static final Property<String> _dropzone = new HtmlProperty("dropzone");
	public static final Property<String> _hidden = new HtmlProperty("hidden");
	public static final Property<String> _id = new HtmlProperty("id");
	public static final Property<String> _lang = new HtmlProperty("lang");
	public static final Property<String> _spellcheck = new HtmlProperty("spellcheck");
	public static final Property<String> _style = new HtmlProperty("style");
	public static final Property<String> _tabindex = new HtmlProperty("tabindex");
	public static final Property<String> _title = new HtmlProperty("title").indexed();
	public static final Property<String> _translate = new HtmlProperty("translate");

	// new properties for Polymer support
	public static final Property<String> _is         = new HtmlProperty("is");
	public static final Property<String> _properties = new HtmlProperty("properties");

	// The role attribute, see http://www.w3.org/TR/role-attribute/
	public static final Property<String> _role = new HtmlProperty("role");

	public static final org.structr.common.View publicView = new org.structr.common.View(DOMElement.class, PropertyView.Public,
		name, tag, pageId, path, parent, children, restQuery, cypherQuery, xpathQuery, functionQuery, partialUpdateKey, dataKey, syncedNodes, sharedComponent, isDOMNode
	);

	public static final org.structr.common.View uiView = new org.structr.common.View(DOMElement.class, PropertyView.Ui, name, tag, pageId, path, parent, children, childrenIds, owner,
		restQuery, cypherQuery, xpathQuery, functionQuery, partialUpdateKey, dataKey, syncedNodes, sharedComponent, sharedComponentConfiguration,
		isDOMNode, renderDetails, hideOnIndex, hideOnDetail, showForLocales, hideForLocales, showConditions, hideConditions,
		_reload, _confirm, _appendId, _action, _attributes, _attr, _fieldName, _hide, _rawValue, _placeholder, _customOptionsQuery, _optionsKey, _returnURI, _editClass, _type, dataStructrIdProperty, _class, _id
	);

	public static final org.structr.common.View htmlView = new org.structr.common.View(DOMElement.class, PropertyView.Html, _accesskey, _class, _contenteditable, _contextmenu, _dir,
		_draggable, _dropzone, _hidden, _id, _lang, _spellcheck, _style, _tabindex, _title, _translate, _onabort, _onblur, _oncanplay,
		_oncanplaythrough, _onchange, _onclick, _oncontextmenu, _ondblclick, _ondrag, _ondragend, _ondragenter, _ondragleave,
		_ondragover, _ondragstart, _ondrop, _ondurationchange, _onemptied, _onended, _onerror, _onfocus, _oninput, _oninvalid,
		_onkeydown, _onkeypress, _onkeyup, _onload, _onloadeddata, _onloadedmetadata, _onloadstart, _onmousedown, _onmousemove,
		_onmouseout, _onmouseover, _onmouseup, _onmousewheel, _onpause, _onplay, _onplaying, _onprogress, _onratechange,
		_onreadystatechange, _onreset, _onscroll, _onseeked, _onseeking, _onselect, _onshow, _onstalled, _onsubmit, _onsuspend,
		_ontimeupdate, _onvolumechange, _onwaiting, _role
	);

	@Override
	public boolean isValid(ErrorBuffer errorBuffer) {

		boolean valid = super.isValid(errorBuffer);

		valid &= nonEmpty(tag, errorBuffer);
		valid &= ValidationHelper.isValidStringMatchingRegex(this, tag, "^[a-z][a-zA-Z0-9\\-]*$", errorBuffer);

		return valid;
	}

	@Override
	public boolean contentEquals(DOMNode otherNode) {

		// two elements can not have the same content
		return false;
	}

	@Override
	public String getContextName() {

		final String _name = getProperty(DOMElement.name);
		if (_name != null) {

			return _name;
		}

		return getProperty(DOMElement.tag);
	}

	@Override
	public void updateFromNode(final DOMNode newNode) throws FrameworkException {

		if (newNode instanceof DOMElement) {

			final PropertyMap properties = new PropertyMap();

			for (Property htmlProp : getHtmlAttributes()) {
				properties.put(htmlProp, newNode.getProperty(htmlProp));
			}

			// copy tag
			properties.put(DOMElement.tag, newNode.getProperty(DOMElement.tag));

			setProperties(securityContext, properties);
		}
	}

	public Property[] getHtmlAttributes() {

		return htmlView.properties();

	}

	public void openingTag(final AsyncBuffer out, final String tag, final EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException {

		final DOMElement _syncedNode = (DOMElement) getProperty(sharedComponent);
		if (_syncedNode != null && EditMode.DEPLOYMENT.equals(editMode)) {

			final String name = _syncedNode.getProperty(AbstractNode.name);

			out.append("<structr:component src=\"");
			out.append(name != null ? name : _syncedNode.getUuid());
			out.append("\"");

			renderSharedComponentConfiguration(out, editMode);

			// include data-* attributes in template
			renderCustomAttributes(out, securityContext, renderContext);

		} else {

			out.append("<").append(tag);

			for (PropertyKey attribute : StructrApp.getConfiguration().getPropertySet(entityType, PropertyView.Html)) {

				String value = null;

				if (EditMode.DEPLOYMENT.equals(editMode)) {

					value = (String)getProperty(attribute);

				} else {

					value = getPropertyWithVariableReplacement(renderContext, attribute);
				}

				if (!(EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode))) {

					value = escapeForHtmlAttributes(value);
				}

				if (value != null) {

					String key = attribute.jsonName().substring(PropertyView.Html.length());

					out.append(" ").append(key).append("=\"").append(value).append("\"");

				}

			}

			// include arbitrary data-* attributes
			renderSharedComponentConfiguration(out, editMode);
			renderCustomAttributes(out, securityContext, renderContext);

			// include special mode attributes
			switch (editMode) {

				case CONTENT:

					if (depth == 0) {

						String pageId = renderContext.getPageId();

						if (pageId != null) {

							out.append(" data-structr-page=\"").append(pageId).append("\"");
						}
					}

					out.append(" data-structr-id=\"").append(getUuid()).append("\"");
					break;

				case RAW:

					out.append(" ").append(DOMElement.dataHashProperty.jsonName()).append("=\"").append(getIdHash()).append("\"");
					break;
	 		}
		}

		out.append(">");
	}

	/**
	 * Render (inner) content.
	 *
	 * @param renderContext
	 * @param depth
	 * @throws FrameworkException
	@Override
	public void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {

		if (isDeleted() || isHidden() || !displayForLocale(renderContext) || !displayForConditions(renderContext)) {
			return;
		}

		// final variables
		final AsyncBuffer out    = renderContext.getBuffer();
		final EditMode editMode  = renderContext.getEditMode(securityContext.getUser(false));
		final boolean isVoid     = isVoidElement();
		final String _tag        = getProperty(DOMElement.tag);

		// non-final variables
		Result localResult                 = renderContext.getResult();
		boolean anyChildNodeCreatesNewLine = false;

		renderStructrAppLib(out, securityContext, renderContext, depth);

		if (depth > 0 && !avoidWhitespace()) {

			out.append(indent(depth, renderContext));

		}

		if (StringUtils.isNotBlank(_tag)) {

			if (EditMode.DEPLOYMENT.equals(editMode)) {

				// Determine if this element's visibility flags differ from
				// the flags of the page and render a <!-- @structr:private -->
				// comment accordingly.
				if (renderDeploymentExportComments(out, false)) {

					// restore indentation
					if (depth > 0 && !avoidWhitespace()) {
						out.append(indent(depth, renderContext));
					}
				}
			}

			openingTag(out, _tag, editMode, renderContext, depth);

			try {

				// in body?
				if (lowercaseBodyName.equals(this.getTagName())) {
					renderContext.setInBody(true);
				}

				// only render children if we are not in a shared component scenario and not in deployment mode
				if (getProperty(sharedComponent) == null || !EditMode.DEPLOYMENT.equals(editMode)) {

					// fetch children
					final List<DOMChildren> rels = getChildRelationships();
					if (rels.isEmpty()) {

						// No child relationships, maybe this node is in sync with another node
						final DOMElement _syncedNode = (DOMElement) getProperty(sharedComponent);
						if (_syncedNode != null) {

							rels.addAll(_syncedNode.getChildRelationships());
						}
					}

					// apply configuration for shared component if present
					final String _sharedComponentConfiguration = getProperty(sharedComponentConfiguration);
					if (StringUtils.isNotBlank(_sharedComponentConfiguration)) {

						Scripting.evaluate(renderContext, this, "${" + _sharedComponentConfiguration + "}", "shared component configuration");
					}

					for (final AbstractRelationship rel : rels) {

						final DOMNode subNode = (DOMNode) rel.getTargetNode();

						if (subNode instanceof DOMElement) {
							anyChildNodeCreatesNewLine = (anyChildNodeCreatesNewLine || !(subNode.avoidWhitespace()));
						}

						subNode.render(renderContext, depth + 1);

					}

				}

			} catch (Throwable t) {

				logger.error("Error while rendering node {}: {}", new java.lang.Object[]{getUuid(), t});

				out.append("Error while rendering node ").append(getUuid()).append(": ").append(t.getMessage());

				logger.warn("", t);

			}

			// render end tag, if needed (= if not singleton tags)
			if (StringUtils.isNotBlank(_tag) && (!isVoid)) {

				// only insert a newline + indentation before the closing tag if any child-element used a newline
				final DOMElement _syncedNode = (DOMElement) getProperty(sharedComponent);
				final boolean isTemplate     = _syncedNode != null && EditMode.DEPLOYMENT.equals(editMode);

				if (anyChildNodeCreatesNewLine || isTemplate) {

					out.append(indent(depth, renderContext));
				}

				if (_syncedNode != null && EditMode.DEPLOYMENT.equals(editMode)) {

					out.append("</structr:component>");

				} else if (isTemplate) {

					out.append("</structr:template>");

				} else {

					out.append("</").append(_tag).append(">");
				}
			}

		}

		// Set result for this level again, if there was any
		if (localResult != null) {
			renderContext.setResult(localResult);
		}
	}

	public boolean isVoidElement() {
		return false;
	}

	public String getOffsetAttributeName(String name, int offset) {

		int namePosition = -1;
		int index = 0;

		List<String> keys = Iterables.toList(this.getNode().getPropertyKeys());
		Collections.sort(keys);

		List<String> names = new ArrayList<>(10);

		for (String key : keys) {

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

		List<String> names = new ArrayList<>(10);

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

		synchronized (htmlProperties) {

			htmlProperty = htmlProperties.get(name);
		}

		if (htmlProperty == null) {

			// try to find native html property defined in
			// DOMElement or one of its subclasses
			PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(entityType, name, false);

			if (key != null && key instanceof HtmlProperty) {

				htmlProperty = (HtmlProperty) key;

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

	// ----- private methods -----

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
	public void setAttribute(final String name, final String value) throws DOMException {

		try {
			HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(securityContext, DOMElement.this, value);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	@Override
	public void removeAttribute(final String name) throws DOMException {

		try {
			HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(securityContext, DOMElement.this, null);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	@Override
	public Attr getAttributeNode(String name) {

		HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
		String value = htmlProperty.getProperty(securityContext, this, true);

		if (value != null) {

			boolean explicitlySpecified = true;
			boolean isId = false;

			if (value.equals(htmlProperty.defaultValue())) {
				explicitlySpecified = false;
			}

			return new DOMAttribute((Page) getOwnerDocument(), this, name, value, explicitlySpecified, isId);
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
			((DOMAttribute) attr).setParent(this);
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

		DOMNodeList results = new DOMNodeList();

		collectNodesByPredicate(this, results, new TagPredicate(tagName), 0, false);

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
	public void setIdAttribute(final String idString, boolean isId) throws DOMException {

		checkWriteAccess();

		try {
			setProperties(securityContext, new PropertyMap(DOMElement._id, idString));

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
			return setAttributeNode((Attr) node);
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

	// ----- interface DOMImportable -----
	@Override
	public Node doImport(final Page newPage) throws DOMException {

		DOMElement newElement = (DOMElement) newPage.createElement(getTagName());

		// copy attributes
		for (String _name : getHtmlAttributeNames()) {

			Attr attr = getAttributeNode(_name);
			if (attr.getSpecified()) {

				newElement.setAttribute(attr.getName(), attr.getValue());
			}
		}

		return newElement;
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (super.onModification(securityContext, errorBuffer, modificationQueue)) {

			final PropertyMap map = new PropertyMap();

			for (Sync rel : getOutgoingRelationships(Sync.class)) {

				final DOMElement syncedNode = (DOMElement) rel.getTargetNode();

				map.clear();

				// sync HTML properties only
				for (Property htmlProp : syncedNode.getHtmlAttributes()) {
					map.put(htmlProp, getProperty(htmlProp));
				}

				map.put(name, getProperty(name));

				syncedNode.setProperties(securityContext, map);
			}

			final Sync rel = getIncomingRelationship(Sync.class);
			if (rel != null) {

				final DOMElement otherNode = (DOMElement) rel.getSourceNode();
				if (otherNode != null) {

					map.clear();

					// sync both ways
					for (Property htmlProp : otherNode.getHtmlAttributes()) {
						map.put(htmlProp, getProperty(htmlProp));
					}

					map.put(name, getProperty(name));

					otherNode.setProperties(securityContext, map);
				}
			}

			return true;
		}

		return false;
	}

	/**
	 * Render script tags with jQuery and structr-app.js to current tag.
	 *
	 * Make sure it happens only once per page.
	 *
	 * @param out
	private void renderStructrAppLib(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext, final int depth) throws FrameworkException {

		EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));

		if (!(EditMode.DEPLOYMENT.equals(editMode) || EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode)) && !renderContext.appLibRendered() && getProperty(new StringProperty(STRUCTR_ACTION_PROPERTY)) != null) {

			out
				.append("<!--")
				.append(indent(depth, renderContext))
				.append("--><script>if (!window.jQuery) { document.write('<script src=\"/structr/js/lib/jquery-1.11.1.min.js\"><\\/script>'); }</script><!--")
				.append(indent(depth, renderContext))
				.append("--><script>if (!window.jQuery.ui) { document.write('<script src=\"/structr/js/lib/jquery-ui-1.11.0.custom.min.js\"><\\/script>'); }</script><!--")
				.append(indent(depth, renderContext))
				.append("--><script>if (!window.jQuery.ui.timepicker) { document.write('<script src=\"/structr/js/lib/jquery-ui-timepicker-addon.min.js\"><\\/script>'); }</script><!--")
				.append(indent(depth, renderContext))
				.append("--><script>if (!window.StructrApp) { document.write('<script src=\"/structr/js/structr-app.js\"><\\/script>'); }</script><!--")
				.append(indent(depth, renderContext))
				.append("--><script>if (!window.moment) { document.write('<script src=\"/structr/js/lib/moment.min.js\"><\\/script>'); }</script><!--")
				.append(indent(depth, renderContext))
				.append("--><link rel=\"stylesheet\" type=\"text/css\" href=\"/structr/css/lib/jquery-ui-1.10.3.custom.min.css\">");

			renderContext.setAppLibRendered(true);

		}

	}

	/**
	 * This method concatenates the pre-defined HTML attributes and the
	 * optional custom data-* attributes.
	 *
	 * @param propertyView
	 * @return property key
	@Override
	public Set<PropertyKey> getPropertyKeys(String propertyView) {


		final Set<PropertyKey> allProperties = new LinkedHashSet<>();
		final Set<PropertyKey> htmlAttrs     = super.getPropertyKeys(propertyView);

		for (final PropertyKey attr : htmlAttrs) {

			allProperties.add(attr);
		}

		allProperties.addAll(getDataPropertyKeys());

		return allProperties;
	}

	@Override
	public boolean isSynced() {
		return hasIncomingRelationships(Sync.class) || hasOutgoingRelationships(Sync.class);
	}

	// ----- interface Syncable -----
	@Override
	public List<GraphObject> getSyncData() throws FrameworkException {

		final List<GraphObject> data = super.getSyncData();

		data.add(getProperty(DOMElement.sharedComponent));
		data.add(getIncomingRelationship(Sync.class));

		if (isSynced()) {

			// add parent
			data.add(getProperty(ownerDocument));
			data.add(getOutgoingRelationship(PageLink.class));
		}

		return data;
	}
	*/
}
