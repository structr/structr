/**
 * Copyright (C) 2010-2020 Structr GmbH
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.Predicate;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.service.LicenseManager;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import static org.structr.web.entity.dom.DOMNode.escapeForHtmlAttributes;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public interface DOMElement extends DOMNode, Element, NamedNodeMap, NonIndexed {

	static final String GET_HTML_ATTRIBUTES_CALL = "return (Property[]) org.apache.commons.lang3.ArrayUtils.addAll(super.getHtmlAttributes(), _html_View.properties());";
	static final String STRUCTR_ACTION_PROPERTY  = "data-structr-action";
	static final String lowercaseBodyName        = "body";

	static final int HtmlPrefixLength            = PropertyView.Html.length();

	static class Impl { static {

		final JsonSchema schema   = SchemaService.getDynamicSchema();
		final JsonObjectType type = schema.addType("DOMElement");

		//type.setIsAbstract();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/DOMElement"));
		type.setExtends(URI.create("#/definitions/DOMNode"));

		type.addStringProperty("tag",              PropertyView.Public, PropertyView.Ui).setIndexed(true).setCategory(PAGE_CATEGORY);
		type.addStringProperty("path",             PropertyView.Public, PropertyView.Ui).setIndexed(true);
		type.addStringProperty("partialUpdateKey", PropertyView.Public, PropertyView.Ui).setIndexed(true);

		type.addStringProperty("_html_onabort", PropertyView.Html);
		type.addStringProperty("_html_onblur", PropertyView.Html);
		type.addStringProperty("_html_oncanplay", PropertyView.Html);
		type.addStringProperty("_html_oncanplaythrough", PropertyView.Html);
		type.addStringProperty("_html_onchange", PropertyView.Html);
		type.addStringProperty("_html_onclick", PropertyView.Html);
		type.addStringProperty("_html_oncontextmenu", PropertyView.Html);
		type.addStringProperty("_html_ondblclick", PropertyView.Html);
		type.addStringProperty("_html_ondrag", PropertyView.Html);
		type.addStringProperty("_html_ondragend", PropertyView.Html);
		type.addStringProperty("_html_ondragenter", PropertyView.Html);
		type.addStringProperty("_html_ondragleave", PropertyView.Html);
		type.addStringProperty("_html_ondragover", PropertyView.Html);
		type.addStringProperty("_html_ondragstart", PropertyView.Html);
		type.addStringProperty("_html_ondrop", PropertyView.Html);
		type.addStringProperty("_html_ondurationchange", PropertyView.Html);
		type.addStringProperty("_html_onemptied", PropertyView.Html);
		type.addStringProperty("_html_onended", PropertyView.Html);
		type.addStringProperty("_html_onerror", PropertyView.Html);
		type.addStringProperty("_html_onfocus", PropertyView.Html);
		type.addStringProperty("_html_oninput", PropertyView.Html);
		type.addStringProperty("_html_oninvalid", PropertyView.Html);
		type.addStringProperty("_html_onkeydown", PropertyView.Html);
		type.addStringProperty("_html_onkeypress", PropertyView.Html);
		type.addStringProperty("_html_onkeyup", PropertyView.Html);
		type.addStringProperty("_html_onload", PropertyView.Html);
		type.addStringProperty("_html_onloadeddata", PropertyView.Html);
		type.addStringProperty("_html_onloadedmetadata", PropertyView.Html);
		type.addStringProperty("_html_onloadstart", PropertyView.Html);
		type.addStringProperty("_html_onmousedown", PropertyView.Html);
		type.addStringProperty("_html_onmousemove", PropertyView.Html);
		type.addStringProperty("_html_onmouseout", PropertyView.Html);
		type.addStringProperty("_html_onmouseover", PropertyView.Html);
		type.addStringProperty("_html_onmouseup", PropertyView.Html);
		type.addStringProperty("_html_onmousewheel", PropertyView.Html);
		type.addStringProperty("_html_onpause", PropertyView.Html);
		type.addStringProperty("_html_onplay", PropertyView.Html);
		type.addStringProperty("_html_onplaying", PropertyView.Html);
		type.addStringProperty("_html_onprogress", PropertyView.Html);
		type.addStringProperty("_html_onratechange", PropertyView.Html);
		type.addStringProperty("_html_onreadystatechange", PropertyView.Html);
		type.addStringProperty("_html_onreset", PropertyView.Html);
		type.addStringProperty("_html_onscroll", PropertyView.Html);
		type.addStringProperty("_html_onseeked", PropertyView.Html);
		type.addStringProperty("_html_onseeking", PropertyView.Html);
		type.addStringProperty("_html_onselect", PropertyView.Html);
		type.addStringProperty("_html_onshow", PropertyView.Html);
		type.addStringProperty("_html_onstalled", PropertyView.Html);
		type.addStringProperty("_html_onsubmit", PropertyView.Html);
		type.addStringProperty("_html_onsuspend", PropertyView.Html);
		type.addStringProperty("_html_ontimeupdate", PropertyView.Html);
		type.addStringProperty("_html_onvolumechange", PropertyView.Html);
		type.addStringProperty("_html_onwaiting", PropertyView.Html);
		type.addStringProperty("_html_data", PropertyView.Html);

		// data-structr-* attibutes
		type.addBooleanProperty("data-structr-reload",              PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("If active, the page will refresh after a successfull action.");
		type.addBooleanProperty("data-structr-confirm",             PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("If active, a user has to confirm the action.");
		type.addBooleanProperty("data-structr-append-id",           PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("On create, append ID of first created object to the return URI.");
		type.addStringProperty("data-structr-action",               PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("The action of the dynamic form (e.g create:&lt;Type&gt; | delete:&lt;Type&gt; | edit | registration | login | logout)");
		type.addStringProperty("data-structr-attributes",           PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("The names of the properties that should be included in the request. (for create, edit/save, login or registration actions)");
		type.addStringProperty("data-structr-attr",                 PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("If this is set, the input field is rendered in auto-edit mode");
		type.addStringProperty("data-structr-name",                 PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("The name of the property (for create/save actions with custom form)");
		type.addStringProperty("data-structr-hide",                 PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Which mode (if any) the element should be hidden from the user (eg. edit | non-edit | edit,non-edit)");
		type.addStringProperty("data-structr-raw-value",            PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("The unformatted value of the element. Provide this if the value of the element is printed with a format applied (useful for Date or Number fields)");
		type.addStringProperty("data-structr-placeholder",          PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("used to display option labels (default: name)");
		type.addStringProperty("data-structr-type",                 PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Type hint for the attribute (e.g. Date, Boolean; default: String)");
		type.addStringProperty("data-structr-custom-options-query", PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Custom REST query for value options (for collection properties)");
		type.addStringProperty("data-structr-options-key",          PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Key used to display option labels for collection properties (default: name)");
		type.addStringProperty("data-structr-edit-class",           PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Custom CSS class in edit mode");
		type.addStringProperty("data-structr-return",               PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Return URI after successful action");
		type.addStringProperty("data-structr-format",               PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Custom format for Date or Enum properties. (Example: Date: dd.MM.yyyy - Enum: Value1,Value2,Value3");
		type.addBooleanProperty("data-structr-insert",              PropertyView.Ui);
		type.addBooleanProperty("data-structr-from-widget",         PropertyView.Ui);

		// Core attributes
		type.addStringProperty("_html_accesskey", PropertyView.Html);
		type.addStringProperty("_html_class", PropertyView.Html, PropertyView.Ui);
		type.addStringProperty("_html_contenteditable", PropertyView.Html);
		type.addStringProperty("_html_contextmenu", PropertyView.Html);
		type.addStringProperty("_html_dir", PropertyView.Html);
		type.addStringProperty("_html_draggable", PropertyView.Html);
		type.addStringProperty("_html_dropzone", PropertyView.Html);
		type.addStringProperty("_html_hidden", PropertyView.Html);
		type.addStringProperty("_html_id", PropertyView.Html, PropertyView.Ui);
		type.addStringProperty("_html_lang", PropertyView.Html);
		type.addStringProperty("_html_spellcheck", PropertyView.Html);
		type.addStringProperty("_html_style", PropertyView.Html);
		type.addStringProperty("_html_tabindex", PropertyView.Html);
		type.addStringProperty("_html_title", PropertyView.Html);
		type.addStringProperty("_html_translate", PropertyView.Html);

		// new properties for Polymer support
		type.addStringProperty("_html_is", PropertyView.Html);
		type.addStringProperty("_html_properties", PropertyView.Html);
		type.addBooleanProperty("fromWidget");

		// The role attribute, see http://www.w3.org/TR/role-attribute/
		type.addStringProperty("_html_role", PropertyView.Html);

		type.addPropertyGetter("tag", String.class);

		type.overrideMethod("updateFromNode",         false, DOMElement.class.getName() + ".updateFromNode(this, arg0);");
		type.overrideMethod("getHtmlAttributes",      false, "return _html_View.properties();");
		type.overrideMethod("getHtmlAttributeNames",  false, "return " + DOMElement.class.getName() + ".getHtmlAttributeNames(this);");
		type.overrideMethod("getOffsetAttributeName", false, "return " + DOMElement.class.getName() + ".getOffsetAttributeName(this, arg0, arg1);");
		type.overrideMethod("getLocalName",           false, "return null;");
		type.overrideMethod("getAttributes",          false, "return this;");
		type.overrideMethod("contentEquals",          false, "return false;");
		type.overrideMethod("hasAttributes",          false, "return getLength() > 0;");
		type.overrideMethod("getLength",              false, "return getHtmlAttributeNames().size();");
		type.overrideMethod("getTagName",             false, "return getTag();");
		type.overrideMethod("getNodeName",            false, "return getTagName();");
		type.overrideMethod("setNodeValue",           false, "");
		type.overrideMethod("getNodeValue",           false, "return null;");
		type.overrideMethod("getNodeType",            false, "return ELEMENT_NODE;");
		type.overrideMethod("getPropertyKeys",        false, "final Set<PropertyKey> allProperties = new LinkedHashSet<>(); final Set<PropertyKey> htmlAttrs = super.getPropertyKeys(arg0); for (final PropertyKey attr : htmlAttrs) { allProperties.add(attr); } allProperties.addAll(getDataPropertyKeys()); return allProperties;");

		type.overrideMethod("openingTag",             false, DOMElement.class.getName() + ".openingTag(this, arg0, arg1, arg2, arg3, arg4);");
		type.overrideMethod("renderContent",          false, DOMElement.class.getName() + ".renderContent(this, arg0, arg1);");
		type.overrideMethod("renderStructrAppLib",    false, DOMElement.class.getName() + ".renderStructrAppLib(this, arg0, arg1, arg2, arg3);");
		type.overrideMethod("setIdAttribute",         false, DOMElement.class.getName() + ".setIdAttribute(this, arg0, arg1);");
		type.overrideMethod("setAttribute",           false, DOMElement.class.getName() + ".setAttribute(this, arg0, arg1);");
		type.overrideMethod("removeAttribute",        false, DOMElement.class.getName() + ".removeAttribute(this, arg0);");
		type.overrideMethod("doImport",               false, "return "+ DOMElement.class.getName() + ".doImport(this, arg0);");
		type.overrideMethod("getAttribute",           false, "return " + DOMElement.class.getName() + ".getAttribute(this, arg0);");
		type.overrideMethod("getAttributeNode",       false, "return " + DOMElement.class.getName() + ".getAttributeNode(this, arg0);");
		type.overrideMethod("setAttributeNode",       false, "return " + DOMElement.class.getName() + ".setAttributeNode(this, arg0);");
		type.overrideMethod("removeAttributeNode",    false, "return " + DOMElement.class.getName() + ".removeAttributeNode(this, arg0);");
		type.overrideMethod("getElementsByTagName",   false, "return " + DOMElement.class.getName() + ".getElementsByTagName(this, arg0);");
		type.overrideMethod("setIdAttributeNS",       false, "throw new UnsupportedOperationException(\"Namespaces not supported.\");");
		type.overrideMethod("setIdAttributeNode",     false, "throw new UnsupportedOperationException(\"Attribute nodes not supported in HTML5.\");");
		type.overrideMethod("hasAttribute",           false, "return getAttribute(arg0) != null;");
		type.overrideMethod("hasAttributeNS",         false, "return false;");
		type.overrideMethod("getAttributeNS",         false, "");
		type.overrideMethod("setAttributeNS",         false, "");
		type.overrideMethod("removeAttributeNS",      false, "");
		type.overrideMethod("getAttributeNodeNS",     false, "return null;");
		type.overrideMethod("setAttributeNodeNS",     false, "return null;");
		type.overrideMethod("getElementsByTagNameNS", false, "return null;");

		// NamedNodeMap
		type.overrideMethod("getNamedItemNS",         false, "return null;");
		type.overrideMethod("setNamedItemNS",         false, "return null;");
		type.overrideMethod("removeNamedItemNS",      false, "return null;");
		type.overrideMethod("getNamedItem",           false, "return getAttributeNode(arg0);");
		type.overrideMethod("setNamedItem",           false, "return " + DOMElement.class.getName() + ".setNamedItem(this, arg0);");
		type.overrideMethod("removeNamedItem",        false, "return " + DOMElement.class.getName() + ".removeNamedItem(this, arg0);");
		type.overrideMethod("getContextName",         false, "return " + DOMElement.class.getName() + ".getContextName(this);");
		type.overrideMethod("item",                   false, "return " + DOMElement.class.getName() + ".item(this, arg0);");

		// CMISInfo
		type.overrideMethod("getSchemaTypeInfo",      false, "return null;");

		// view configuration
		type.addViewProperty(PropertyView.Public, "isDOMNode");
		type.addViewProperty(PropertyView.Public, "pageId");
		type.addViewProperty(PropertyView.Public, "parent");
		type.addViewProperty(PropertyView.Public, "sharedComponentId");
		type.addViewProperty(PropertyView.Public, "syncedNodesIds");
		type.addViewProperty(PropertyView.Public, "name");
		type.addViewProperty(PropertyView.Public, "children");
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
		type.addViewProperty(PropertyView.Ui, "data-structr-id");
		type.addViewProperty(PropertyView.Ui, "renderDetails");
		type.addViewProperty(PropertyView.Ui, "children");
		type.addViewProperty(PropertyView.Ui, "childrenIds");
		type.addViewProperty(PropertyView.Ui, "showForLocales");
		type.addViewProperty(PropertyView.Ui, "hideForLocales");
		type.addViewProperty(PropertyView.Ui, "showConditions");
		type.addViewProperty(PropertyView.Ui, "hideConditions");
		type.addViewProperty(PropertyView.Ui, "dataKey");
		type.addViewProperty(PropertyView.Ui, "cypherQuery");
		type.addViewProperty(PropertyView.Ui, "xpathQuery");
		type.addViewProperty(PropertyView.Ui, "restQuery");
		type.addViewProperty(PropertyView.Ui, "functionQuery");

		final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
		if (licenseManager == null || licenseManager.isModuleLicensed("api-builder")) {

			type.addViewProperty(PropertyView.Public, "flow");
			type.addViewProperty(PropertyView.Ui, "flow");
		}

	}}

	String getTag();
	String getOffsetAttributeName(final String name, final int offset);

	void openingTag(final AsyncBuffer out, final String tag, final EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException;
	void renderStructrAppLib(final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext, final int depth) throws FrameworkException;

	Property[] getHtmlAttributes();
	List<String> getHtmlAttributeNames();

	@Override
	public Set<PropertyKey> getPropertyKeys(String propertyView);

	// ----- static methods -----
	public static String getOffsetAttributeName(final DOMElement elem, final String name, final int offset) {

		int namePosition = -1;
		int index = 0;

		List<String> keys = Iterables.toList(elem.getNode().getPropertyKeys());
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

	public static void updateFromNode(final DOMElement node, final DOMNode newNode) throws FrameworkException {

		if (newNode instanceof DOMElement) {

			final PropertyKey<String> tagKey = StructrApp.key(DOMElement.class, "tag");
			final PropertyMap properties     = new PropertyMap();

			for (Property htmlProp : node.getHtmlAttributes()) {
				properties.put(htmlProp, newNode.getProperty(htmlProp));
			}

			// copy tag
			properties.put(tagKey, newNode.getProperty(tagKey));

			node.setProperties(node.getSecurityContext(), properties);
		}
	}

	public static String getContextName(DOMElement thisNode) {

		final String _name = thisNode.getProperty(DOMElement.name);
		if (_name != null) {

			return _name;
		}

		return thisNode.getTag();
	}

	static void renderContent(final DOMElement thisElement, final RenderContext renderContext, final int depth) throws FrameworkException {

		if (!thisElement.shouldBeRendered(renderContext)) {
			return;
		}

		// final variables
		final SecurityContext securityContext = renderContext.getSecurityContext();
		final AsyncBuffer out                 = renderContext.getBuffer();
		final EditMode editMode               = renderContext.getEditMode(securityContext.getUser(false));
		final boolean isVoid                  = thisElement.isVoidElement();
		final String _tag                     = thisElement.getTag();

		// non-final variables
		boolean anyChildNodeCreatesNewLine = false;

		thisElement.renderStructrAppLib(out, securityContext, renderContext, depth);

		if (depth > 0 && !thisElement.avoidWhitespace()) {

			out.append(DOMNode.indent(depth, renderContext));

		}

		if (StringUtils.isNotBlank(_tag)) {

			if (EditMode.DEPLOYMENT.equals(editMode)) {

				// Determine if this element's visibility flags differ from
				// the flags of the page and render a <!-- @structr:private -->
				// comment accordingly.
				if (DOMNode.renderDeploymentExportComments(thisElement, out, false)) {

					// restore indentation
					if (depth > 0 && !thisElement.avoidWhitespace()) {
						out.append(DOMNode.indent(depth, renderContext));
					}
				}
			}

			thisElement.openingTag(out, _tag, editMode, renderContext, depth);

			try {

				// in body?
				if (lowercaseBodyName.equals(thisElement.getTagName())) {
					renderContext.setInBody(true);
				}

				// only render children if we are not in a shared component scenario and not in deployment mode
				if (thisElement.getSharedComponent() == null || !EditMode.DEPLOYMENT.equals(editMode)) {

					// fetch children
					final List<RelationshipInterface> rels = thisElement.getChildRelationships();
					if (rels.isEmpty()) {

						// No child relationships, maybe this node is in sync with another node
						final DOMElement _syncedNode = (DOMElement) thisElement.getSharedComponent();
						if (_syncedNode != null) {

							rels.addAll(_syncedNode.getChildRelationships());
						}
					}

					// apply configuration for shared component if present
					final String _sharedComponentConfiguration = thisElement.getProperty(StructrApp.key(DOMElement.class, "sharedComponentConfiguration"));
					if (StringUtils.isNotBlank(_sharedComponentConfiguration)) {

						Scripting.evaluate(renderContext, thisElement, "${" + _sharedComponentConfiguration.trim() + "}", "shared component configuration");
					}

					for (final RelationshipInterface rel : rels) {

						final DOMNode subNode = (DOMNode) rel.getTargetNode();

						if (subNode instanceof DOMElement) {
							anyChildNodeCreatesNewLine = (anyChildNodeCreatesNewLine || !(subNode.avoidWhitespace()));
						}

						subNode.render(renderContext, depth + 1);

					}

				}

			} catch (Throwable t) {

				out.append("Error while rendering node ").append(thisElement.getUuid()).append(": ").append(t.getMessage());

				logger.warn("", t);
			}

			// render end tag, if needed (= if not singleton tags)
			if (StringUtils.isNotBlank(_tag) && (!isVoid) || (isVoid && thisElement.getSharedComponent() != null && EditMode.DEPLOYMENT.equals(editMode))) {

				// only insert a newline + indentation before the closing tag if any child-element used a newline
				final DOMElement _syncedNode = (DOMElement) thisElement.getSharedComponent();
				final boolean isTemplate     = _syncedNode != null && EditMode.DEPLOYMENT.equals(editMode);

				if (anyChildNodeCreatesNewLine || isTemplate) {

					out.append(DOMNode.indent(depth, renderContext));
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
	}

	static void renderStructrAppLib(final DOMElement thisElement, final AsyncBuffer out, final SecurityContext securityContext, final RenderContext renderContext, final int depth) throws FrameworkException {

		EditMode editMode = renderContext.getEditMode(securityContext.getUser(false));

		if (!(EditMode.DEPLOYMENT.equals(editMode) || EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode)) && !renderContext.appLibRendered() && thisElement.getProperty(new StringProperty(STRUCTR_ACTION_PROPERTY)) != null) {

			out
				.append("<!--")
				.append(DOMNode.indent(depth, renderContext))
				.append("--><script>if (!window.jQuery) { document.write('<script src=\"/structr/js/lib/jquery-3.3.1.min.js\"><\\/script>'); }</script><!--")
				.append(DOMNode.indent(depth, renderContext))
				.append("--><script>if (!window.jQuery.ui) { document.write('<script src=\"/structr/js/lib/jquery-ui-1.11.0.custom.min.js\"><\\/script>'); }</script><!--")
				.append(DOMNode.indent(depth, renderContext))
				.append("--><script>if (!window.jQuery.ui.timepicker) { document.write('<script src=\"/structr/js/lib/jquery-ui-timepicker-addon.min.js\"><\\/script>'); }</script><!--")
				.append(DOMNode.indent(depth, renderContext))
				.append("--><script>if (!window.StructrApp) { document.write('<script src=\"/structr/js/structr-app.js\"><\\/script>'); }</script><!--")
				.append(DOMNode.indent(depth, renderContext))
				.append("--><script>if (!window.moment) { document.write('<script src=\"/structr/js/lib/moment.min.js\"><\\/script>'); }</script><!--")
				.append(DOMNode.indent(depth, renderContext))
				.append("--><link rel=\"stylesheet\" type=\"text/css\" href=\"/structr/css/lib/jquery-ui-1.10.3.custom.min.css\">");

			renderContext.setAppLibRendered(true);

		}

	}

	static void openingTag(final DOMElement thisElement, final AsyncBuffer out, final String tag, final EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException {

		final DOMElement _syncedNode = (DOMElement) thisElement.getSharedComponent();

		if (_syncedNode != null && EditMode.DEPLOYMENT.equals(editMode)) {

			out.append("<structr:component src=\"");

			final String _name = _syncedNode.getProperty(AbstractNode.name);
			out.append(_name != null ? _name.concat("-").concat(_syncedNode.getUuid()) : _syncedNode.getUuid());

			out.append("\"");

			thisElement.renderSharedComponentConfiguration(out, editMode);

			// include data-* attributes in template
			thisElement.renderCustomAttributes(out, renderContext.getSecurityContext(), renderContext);

		} else {

			out.append("<").append(tag);

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Class type = thisElement.getEntityType();

			final List<PropertyKey> htmlAttributes = new ArrayList<>();
			thisElement.getNode().getPropertyKeys().forEach((key) -> {
				if (key.startsWith(PropertyView.Html)) {
					htmlAttributes.add(config.getPropertyKeyForJSONName(type, key));
				}
			});

			if (EditMode.DEPLOYMENT.equals(editMode)) {
				Collections.sort(htmlAttributes);
			}

			for (PropertyKey attribute : htmlAttributes) {

				String value = null;

				if (EditMode.DEPLOYMENT.equals(editMode)) {

					value = (String)thisElement.getProperty(attribute);

				} else {

					value = thisElement.getPropertyWithVariableReplacement(renderContext, attribute);
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
			thisElement.renderSharedComponentConfiguration(out, editMode);
			thisElement.renderCustomAttributes(out, renderContext.getSecurityContext(), renderContext);

			// include special mode attributes
			switch (editMode) {

				case SHAPES:
				case SHAPES_MINIATURES:

					final boolean isInsertable = thisElement.getProperty(StructrApp.key(DOMElement.class, "data-structr-insert"));
					final boolean isFromWidget = thisElement.getProperty(StructrApp.key(DOMElement.class, "data-structr-from-widget"));

					if (isInsertable || isFromWidget) {
						out.append(" data-structr-id=\"").append(thisElement.getUuid()).append("\"");
					}
					break;

				case CONTENT:

					if (depth == 0) {

						String pageId = renderContext.getPageId();

						if (pageId != null) {

							out.append(" data-structr-page=\"").append(pageId).append("\"");
						}
					}

					out.append(" data-structr-id=\"").append(thisElement.getUuid()).append("\"");
					break;

				case RAW:

					out.append(" ").append("data-structr-hash").append("=\"").append(thisElement.getIdHash()).append("\"");
					break;
	 		}
		}

		out.append(">");
	}

	public static Node doImport(final DOMElement thisNode, final Page newPage) throws DOMException {

		DOMElement newElement = (DOMElement) newPage.createElement(thisNode.getTag());

		// copy attributes
		for (String _name : thisNode.getHtmlAttributeNames()) {

			Attr attr = thisNode.getAttributeNode(_name);
			if (attr.getSpecified()) {

				newElement.setAttribute(attr.getName(), attr.getValue());
			}
		}

		return newElement;
	}

	public static List<String> getHtmlAttributeNames(final DOMElement thisNode) {

		List<String> names = new ArrayList<>(10);

		for (String key : thisNode.getNode().getPropertyKeys()) {

			// use html properties only
			if (key.startsWith(PropertyView.Html)) {

				names.add(key.substring(HtmlPrefixLength));
			}
		}

		return names;
	}

	public static void setIdAttribute(final DOMElement thisElement, final String idString, boolean isId) throws DOMException {

		thisElement.checkWriteAccess();

		try {
			thisElement.setProperty(StructrApp.key(DOMElement.class, "_html_id"), idString);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}
	}

	public static String getAttribute(final DOMElement thisElement, final String name) {

		HtmlProperty htmlProperty = DOMElement.findOrCreateAttributeKey(thisElement, name);

		return htmlProperty.getProperty(thisElement.getSecurityContext(), thisElement, true);
	}

	public static void setAttribute(final DOMElement thisElement, final String name, final String value) throws DOMException {

		try {
			HtmlProperty htmlProperty = DOMElement.findOrCreateAttributeKey(thisElement, name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(thisElement.getSecurityContext(), thisElement, value);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	public static void removeAttribute(final DOMElement thisElement, final String name) throws DOMException {

		try {
			HtmlProperty htmlProperty = DOMElement.findOrCreateAttributeKey(thisElement, name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(thisElement.getSecurityContext(), thisElement, null);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	public static Attr getAttributeNode(final DOMElement thisElement, final String name) {

		HtmlProperty htmlProperty = DOMElement.findOrCreateAttributeKey(thisElement, name);
		final String value        = htmlProperty.getProperty(thisElement.getSecurityContext(), thisElement, true);

		if (value != null) {

			boolean explicitlySpecified = true;
			boolean isId = false;

			if (value.equals(htmlProperty.defaultValue())) {
				explicitlySpecified = false;
			}

			return new DOMAttribute((Page) thisElement.getOwnerDocument(), thisElement, name, value, explicitlySpecified, isId);
		}

		return null;
	}

	public static Attr setAttributeNode(final DOMElement thisElement, final Attr attr) throws DOMException {

		// save existing attribute node
		final Attr attribute = thisElement.getAttributeNode(attr.getName());

		// set value
		thisElement.setAttribute(attr.getName(), attr.getValue());

		// set parent of attribute node
		if (attr instanceof DOMAttribute) {
			((DOMAttribute) attr).setParent(thisElement);
		}

		return attribute;
	}

	public static Attr removeAttributeNode(final DOMElement thisElement, final Attr attr) throws DOMException {

		// save existing attribute node
		final Attr attribute = thisElement.getAttributeNode(attr.getName());

		// set value
		thisElement.setAttribute(attr.getName(), null);

		return attribute;
	}

	public static NodeList getElementsByTagName(final DOMElement thisElement, final String tagName) {

		DOMNodeList results = new DOMNodeList();

		DOMNode.collectNodesByPredicate(thisElement.getSecurityContext(), thisElement, results, new TagPredicate(tagName), 0, false);

		return results;
	}

	public static HtmlProperty findOrCreateAttributeKey(final DOMElement thisElement, final String name) {

		// try to find native html property defined in DOMElement or one of its subclasses
		final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(thisElement.getEntityType(), name, false);

		if (key != null && key instanceof HtmlProperty) {

			return (HtmlProperty) key;

		} else {

			// create synthetic HtmlProperty
			final HtmlProperty htmlProperty = new HtmlProperty(name);
			htmlProperty.setDeclaringClass(DOMElement.class);

			return htmlProperty;
		}
	}

	public static Node setNamedItem(final DOMElement thisElement, final Node node) throws DOMException {

		if (node instanceof Attr) {
			return thisElement.setAttributeNode((Attr) node);
		}

		return null;
	}

	public static Node removeNamedItem(final DOMElement thisElement, final String name) throws DOMException {

		// save existing attribute node
		Attr attribute = thisElement.getAttributeNode(name);

		// set value to null
		thisElement.setAttribute(name, null);

		return attribute;
	}

	public static Node item(final DOMElement thisElement, final int i) {

		List<String> htmlAttributeNames = thisElement.getHtmlAttributeNames();
		if (i >= 0 && i < htmlAttributeNames.size()) {

			return thisElement.getAttributeNode(htmlAttributeNames.get(i));
		}

		return null;
	}

	public static class TagPredicate implements Predicate<Node> {

		private String tagName = null;

		public TagPredicate(String tagName) {
			this.tagName = tagName;
		}

		@Override
		public boolean accept(Node obj) {

			if (obj instanceof DOMElement) {

				DOMElement elem = (DOMElement)obj;

				if (tagName.equals(elem.getProperty(StructrApp.key(DOMElement.class, "tag")))) {
					return true;
				}
			}

			return false;
		}
	}
}
