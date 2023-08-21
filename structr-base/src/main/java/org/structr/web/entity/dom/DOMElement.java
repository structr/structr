/*
 * Copyright (C) 2010-2023 Structr GmbH
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

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.graph.Cardinality;
import org.structr.api.schema.JsonObjectType;
import org.structr.api.schema.JsonSchema;
import org.structr.api.service.LicenseManager;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.rest.RestMethodResult;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.NonIndexed;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.EventContext;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.function.InsertHtmlFunction;
import org.structr.web.function.RemoveDOMChildFunction;
import org.structr.web.function.ReplaceDOMChildFunction;
import org.structr.web.resource.LoginResource;
import org.structr.web.resource.LogoutResource;
import org.structr.web.resource.RegistrationResource;
import org.structr.web.resource.ResetPasswordResource;
import org.structr.web.servlet.HtmlServlet;
import org.w3c.dom.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import static org.structr.web.entity.dom.DOMNode.escapeForHtmlAttributes;
import org.structr.web.entity.html.TemplateElement;

public interface DOMElement extends DOMNode, Element, NamedNodeMap, NonIndexed {

	static final Set<String> RequestParameterBlacklist = Set.of(HtmlServlet.ENCODED_RENDER_STATE_PARAMETER_NAME);

	static final String GET_HTML_ATTRIBUTES_CALL = "return (Property[]) org.apache.commons.lang3.ArrayUtils.addAll(super.getHtmlAttributes(), _html_View.properties());";
	static final String STRUCTR_ACTION_PROPERTY  = "data-structr-action";
	static final String lowercaseBodyName        = "body";

	static final String DATA_BINDING_PARAMETER_STRUCTRID             = "structrId";
	static final String DATA_BINDING_PARAMETER_STRUCTRIDEXPRESSION   = "structrIdExpression";
	static final String DATA_BINDING_PARAMETER_STRUCTRTARGET         = "structrTarget";
	static final String DATA_BINDING_PARAMETER_STRUCTRMETHOD         = "structrMethod";
	static final String DATA_BINDING_PARAMETER_STRUCTRACTION         = "structrAction";
	static final String DATA_BINDING_PARAMETER_STRUCTREVENT          = "structrEvent";
	static final String DATA_BINDING_PARAMETER_STRUCTREVENTS         = "structrEvents";
	static final String DATA_BINDING_PARAMETER_HTMLEVENT             = "htmlEvent";
	static final String DATA_BINDING_PARAMETER_CHILDID               = "childId";
	static final String DATA_BINDING_PARAMETER_SOURCEOBJECT          = "sourceObject";
	static final String DATA_BINDING_PARAMETER_SOURCEPROPERTY        = "sourceProperty";
	static final String DATA_BINDING_PARAMETER_DATA_TYPE             = "structrDataType";
	static final String DATA_BINDING_PARAMETER_SUCCESS_NOTIFICATIONS = "structrSuccessNotifications";
	static final String DATA_BINDING_PARAMETER_FAILURE_NOTIFICATIONS = "structrFailureNotifications";
	static final String DATA_BINDING_PARAMETER_SUCCESS_TARGET        = "structrSuccessTarget";
	static final String DATA_BINDING_PARAMETER_FAILURE_TARGET        = "structrFailureTarget";
	static final String DATA_BINDING_PARAMETER_SUCCESS_NOTIFICATIONS_CUSTOM_DIALOG_ELEMENT = "structrSuccessNotificationsCustomDialogElement";


	static final int HtmlPrefixLength            = PropertyView.Html.length();
	static final Gson gson                       = new GsonBuilder().create();

	static class Impl { static {

		final JsonSchema schema            = SchemaService.getDynamicSchema();
		final JsonObjectType type          = schema.addType("DOMElement");
		final JsonObjectType actionMapping = schema.addType("ActionMapping");

		//type.setIsAbstract();
		type.setImplements(URI.create("https://structr.org/v1.1/definitions/DOMElement"));
		type.setExtends(URI.create("#/definitions/DOMNode"));
		type.setCategory("html");

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

		// data-structr-* attributes
		type.addBooleanProperty("data-structr-reload",              PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("If active, the page will refresh after a successful action.");
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

		// simple interactive elements
		type.addStringProperty("data-structr-target",                PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Event target, usually something like ${dataKey.id} for custom, update and delete events, or the entity type for create events.");
		type.addStringProperty("data-structr-options",               PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Configuration options for simple interactive elements, reserved for future use.");
		type.addStringProperty("data-structr-reload-target",         PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("CSS selector that specifies which partials to reload.");
		type.addStringProperty("data-structr-tree-children",         PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Toggles automatic visibility for tree child items when the 'toggle-tree-item' event is mapped. This field must contain the data key on which the tree is based, e.g. 'item'.");
		type.addBooleanProperty("data-structr-manual-reload-target", PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Identifies this element as a manual reload target, this is necessary when using repeaters as reload targets.");
		type.addStringProperty("eventMapping",                       PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("A mapping between the desired Javascript event (click, drop, dragOver, ...) and the server-side event that should be triggered: (create | update | delete | <method name>).");
		type.addPropertyGetter("eventMapping", String.class);
		type.relate(type, "RELOADS",   Cardinality.ManyToMany, "reloadSources",     "reloadTargets");

		// old relationships between action element (typically Button) and inputs (typically Input or Select elements)
		//type.relate(type, "INPUTS",   Cardinality.OneToMany,   "actionElement",     "inputs");
		//type.addViewProperty(PropertyView.Ui, "actionElement");
		//type.addViewProperty(PropertyView.Ui, "inputs");

		// new event action mapping, moved to ActionMapping node
		type.addViewProperty(PropertyView.Ui, "triggeredActions");

		// attributes for lazy rendering
		type.addStringProperty("data-structr-rendering-mode",       PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Rendering mode, possible values are empty (default for eager rendering), 'load' to render when the DOM document has finished loading, 'delayed' like 'load' but with a fixed delay, 'visible' to render when the element comes into view and 'periodic' to render the element with periodic updates with a given interval");
		type.addStringProperty("data-structr-delay-or-interval",    PropertyView.Ui).setCategory(EDIT_MODE_BINDING_CATEGORY).setHint("Delay or interval in milliseconds for 'delayed' or 'periodic' rendering mode");

		// Core attributes
		type.addStringProperty("_html_accesskey", PropertyView.Html);
		type.addStringProperty("_html_class", PropertyView.Html, PropertyView.Ui);
		type.addStringProperty("_html_contenteditable", PropertyView.Html);
		type.addStringProperty("_html_contextmenu", PropertyView.Html);
		type.addStringProperty("_html_dir", PropertyView.Html);
		type.addStringProperty("_html_draggable", PropertyView.Html);
		type.addStringProperty("_html_dropzone", PropertyView.Html);
		type.addStringProperty("_html_hidden", PropertyView.Html);
		type.addStringProperty("_html_id", PropertyView.Html, PropertyView.Ui).setIndexed(true);
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

		type.overrideMethod("onCreation",             true,  DOMElement.class.getName() + ".onCreation(this, arg0, arg1);");
		type.overrideMethod("onModification",         true,  DOMElement.class.getName() + ".onModification(this, arg0, arg1, arg2);");

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
		type.overrideMethod("getCssClass",            false, "return getProperty(_html_classProperty);");

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

		// W3C Element
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
	String getEventMapping();

	@Override
	Set<PropertyKey> getPropertyKeys(String propertyView);

	static void onCreation(final DOMElement thisElement, final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {
		DOMElement.updateReloadTargets(thisElement);
	}

	static void onModification(final DOMElement thisElement, final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {
		DOMElement.updateReloadTargets(thisElement);
	}

	@Export
	default Object event(final SecurityContext securityContext, final java.util.Map<String, java.lang.Object> parameters) throws FrameworkException {

		final ActionContext actionContext = new ActionContext(securityContext);
		final EventContext  eventContext  = new EventContext();
		final String        event         = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_HTMLEVENT);
		final String        action;

		if (event == null) {
			throw new FrameworkException(422, "Cannot execute action without event name (htmlEvent property).");
		}

		ActionMapping triggeredAction;

		final List<ActionMapping> triggeredActions = (List<ActionMapping>) Iterables.toList((Iterable<? extends ActionMapping>) StructrApp.getInstance().get(DOMElement.class, this.getUuid()).getProperty(StructrApp.key(DOMElement.class, "triggeredActions")));
		if (triggeredActions != null && !triggeredActions.isEmpty()) {
			triggeredAction = triggeredActions.get(0);
			action = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "action"));
		} else {
			throw new FrameworkException(422, "Cannot execute action without action defined on this DOMElement: " + this);
		}

		// parse event mapping property on this node and determine event type
//		final String eventMapping = getProperty(StructrApp.key(DOMElement.class, "eventMapping"));
//		if (eventMapping != null) {
//
//			final Map<String, Object> mapping = getMappedEvents();
//			if (mapping != null) {
//
//				event = (String)mapping.get(event);
//			}
//		}

		// first thing to do: remove ID from parameters since it is only used to identify this element as event target
		parameters.remove("id");

		// store event context in object
		actionContext.setConstant("eventContext", eventContext);

		switch (action) {

			case "create":
				return handleCreateAction(actionContext, parameters, eventContext);

			case "update":
				handleUpdateAction(actionContext, parameters, eventContext);
				break;

			case "delete":
				handleDeleteAction(actionContext, parameters, eventContext);
				break;

			case "append-child":
				handleAppendChildAction(actionContext, parameters, eventContext);
				break;

			case "remove-child":
				handleRemoveChildAction(actionContext, parameters, eventContext);
				break;

			case "insert-html":
				return handleInsertHtmlAction(actionContext, parameters, eventContext);

			case "replace-html":
				return handleReplaceHtmlAction(actionContext, parameters, eventContext);

			case "open-tree-item":
			case "close-tree-item":
			case "toggle-tree-item":
				handleTreeAction(actionContext, parameters, eventContext, event);
				break;

			case "sign-in":
				return handleSignInAction(actionContext, parameters, eventContext);

			case "sign-out":
				return handleSignOutAction(actionContext, parameters, eventContext);

			case "sign-up":
				return handleSignUpAction(actionContext, parameters, eventContext);

			case "reset-password":
				return handleResetPasswordAction(actionContext, parameters, eventContext);

			case "method":
			default:
				// execute custom method (and return the result directly)
				final String method = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRMETHOD);
				return handleCustomAction(actionContext, parameters, eventContext, method);
		}

		return eventContext;
	}

	private Object handleSignInAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		removeInternalDataBindingKeys(parameters);

		final LoginResource loginResource = new LoginResource();
		loginResource.setSecurityContext(actionContext.getSecurityContext());

		final Map<String, Object> properties = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			properties.put(key, value);
		}

		final RestMethodResult result = loginResource.doPost(properties);

		return result;
	}

	private Object handleSignOutAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		removeInternalDataBindingKeys(parameters);

		final LogoutResource logoutResource = new LogoutResource();
		logoutResource.setSecurityContext(actionContext.getSecurityContext());

		final Map<String, Object> properties = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			properties.put(key, value);
		}

		final RestMethodResult result = logoutResource.doPost(properties);

		return result;
	}

	private Object handleSignUpAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final Map<String, Object> properties = new LinkedHashMap<>();

		removeInternalDataBindingKeys(parameters);

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			if (value != null) properties.put(key, value);
		}

		final RegistrationResource registrationResource = new RegistrationResource();
		registrationResource.setSecurityContext(actionContext.getSecurityContext());
		final RestMethodResult result = registrationResource.doPost(properties);

		return result;
	}

	private Object handleResetPasswordAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final Map<String, Object> properties = new LinkedHashMap<>();

		removeInternalDataBindingKeys(parameters);

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			if (value != null) properties.put(key, value);
		}

		final ResetPasswordResource resetPasswordResource = new ResetPasswordResource();
		resetPasswordResource.setSecurityContext(actionContext.getSecurityContext());
		final RestMethodResult result = resetPasswordResource.doPost(properties);

		return result;
	}

	private void handleTreeAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext, final String action) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();

		if (parameters.containsKey(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET)) {

			final String key = getTreeItemSessionIdentifier((String)parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET));

			switch (action) {

				case "open-tree-item":
					setSessionAttribute(securityContext, key, true);
					break;

				case "close-tree-item":
					removeSessionAttribute(securityContext, key);
					break;

				case "toggle-tree-item":

					if (Boolean.TRUE.equals(getSessionAttribute(securityContext, key))) {

						removeSessionAttribute(securityContext, key);

					} else {

						setSessionAttribute(securityContext, key, true);
					}
					break;
			}


		} else {

			throw new FrameworkException(422, "Cannot execute update action without target UUID (data-structr-target attribute).");
		}
	}

	private GraphObject handleCreateAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();

		// create new object of type?
		final String targetType = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET);
		if (targetType == null) {

			throw new FrameworkException(422, "Cannot execute create action without target type (data-structr-target attribute).");
		}

		// resolve target type
		Class type = StructrApp.getConfiguration().getNodeEntityClass(targetType);
		if (type == null) {

			type = StructrApp.getConfiguration().getRelationshipEntityClass(targetType);
		}

		if (type == null) {

			throw new FrameworkException(422, "Cannot execute create action with target type " + targetType + ", type does not exist.");
		}

		removeInternalDataBindingKeys(parameters);

		// convert input
		final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, type, parameters);

		// create entity
		return StructrApp.getInstance(securityContext).create(type, properties);
	}

	private void handleUpdateAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute update action without target UUID (data-structr-target attribute).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : DOMElement.resolveDataTargets(actionContext, this, dataTarget)) {

			// convert input
			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, target.getEntityType(), parameters);

			// update properties
			target.setProperties(securityContext, properties);

		}
	}

	private void handleDeleteAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final String dataTarget               = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute delete action without target UUID (data-structr-target attribute).");
		}

		for (final GraphObject target : DOMElement.resolveDataTargets(actionContext, this, dataTarget)) {

			if (target.isNode()) {

				app.delete((AbstractNode)target);

			} else {

				app.delete((AbstractRelationship)target);
			}
		}
	}

	private Object handleCustomAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext, final String methodName) throws FrameworkException {

		// Support old and new parameters
		final String idExpression  = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRIDEXPRESSION);
		final String structrTarget = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET);
		final String dataTarget    = structrTarget != null ? structrTarget : idExpression;

		// Empty dataTarget means no database object and no type, so it can only be a global (schema) method
		if (StringUtils.isNotBlank(methodName) && dataTarget == null) {

			removeInternalDataBindingKeys(parameters);

			return Actions.callWithSecurityContext(methodName, actionContext.getSecurityContext(), parameters);

			// call global schema method
			//return invokeMethod(actionContext.getSecurityContext(), action, parameters, false, new EvaluationHints());
			//throw new FrameworkException(422, "Cannot execute action without target (data-structr-target attribute).");

		}

		if (Settings.isValidUuid(dataTarget)) {

			final List<GraphObject> targets = DOMElement.resolveDataTargets(actionContext, this, dataTarget);
			final Logger logger             = LoggerFactory.getLogger(getClass());

			if (targets.size() > 1) {
				logger.warn("Custom action has multiple targets, this is not supported yet. Returning only the result of the first target.");
			}

			removeInternalDataBindingKeys(parameters);

			for (final GraphObject target : targets) {

				// try to execute event method
				return target.invokeMethod(actionContext.getSecurityContext(), methodName, parameters, false, new EvaluationHints());
			}

		} else {

			// add support for static methods
			final Class staticClass = StructrApp.getConfiguration().getNodeEntityClass(dataTarget);
			if (staticClass != null) {

				final Map<String, Method> methods = StructrApp.getConfiguration().getExportedMethodsForType(staticClass);
				final Method method               = methods.get(methodName);

				if (method != null) {

					if (Modifier.isStatic(methods.get(methodName).getModifiers())) {

						return AbstractNode.invokeMethod(actionContext.getSecurityContext(), method, null, parameters, new EvaluationHints());

					} else {

						throw new FrameworkException(422, "Cannot execute static method " + dataTarget + "." + methodName + ": method is not static.");
					}

				} else {

					throw new FrameworkException(422, "Cannot execute static method " + dataTarget + "." + methodName + ": method not found.");
				}

			} else {

				throw new FrameworkException(422, "Cannot execute static method " + dataTarget + "." + methodName + ": type not found.");
			}
		}

		return null;
	}

	private Object handleAppendChildAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String)parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without target UUID (data-structr-target attribute).");
		}

		// fetch child ID
		final String childId = (String)parameters.get(DOMElement.DATA_BINDING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without child UUID (data-child-id attribute).");
		}

		// load child node
		final DOMNode child = StructrApp.getInstance(securityContext).get(DOMNode.class, childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without child (object with ID not found or not a DOMNode).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : DOMElement.resolveDataTargets(actionContext, this, dataTarget)) {

			if (target instanceof DOMElement) {

				final DOMElement domTarget = (DOMElement)target;

				domTarget.appendChild(child);

			} else {

				throw new FrameworkException(422, "Cannot execute append-child action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Object handleRemoveChildAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String)parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without target UUID (data-structr-target attribute).");
		}

		// fetch child ID
		final String childId = (String)parameters.get(DOMElement.DATA_BINDING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without child UUID (data-child-id attribute).");
		}

		// load child node
		final DOMNode child = StructrApp.getInstance(securityContext).get(DOMNode.class, childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without child (object with ID not found or not a DOMNode).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : DOMElement.resolveDataTargets(actionContext, this, dataTarget)) {

			if (target instanceof DOMElement) {

				final DOMElement parent = (DOMElement)target;

				RemoveDOMChildFunction.apply(actionContext.getSecurityContext(), parent, child);

			} else {

				throw new FrameworkException(422, "Cannot execute remove-child action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Object handleInsertHtmlAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String)parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET);
		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without target UUID (data-structr-target attribute).");
		}

		final String sourceObjectId = (String)parameters.get(DOMElement.DATA_BINDING_PARAMETER_SOURCEOBJECT);
		if (sourceObjectId == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without html source object UUID (data-source-object).");
		}

		final String sourceProperty = (String)parameters.get(DOMElement.DATA_BINDING_PARAMETER_SOURCEPROPERTY);
		if (sourceProperty == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without html source property name (data-source-property).");
		}

		final GraphObject sourceObject = StructrApp.getInstance(securityContext).get(NodeInterface.class, sourceObjectId);
		if (sourceObject == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without html source property name (data-source-property).");
		}

		final String htmlSource = sourceObject.getProperty(sourceProperty);
		if (StringUtils.isBlank(htmlSource)) {

			throw new FrameworkException(422, "Cannot execute insert-html action without empty html source.");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : DOMElement.resolveDataTargets(actionContext, this, dataTarget)) {

			if (target instanceof DOMElement) {

				final DOMElement parent = (DOMElement) target;

				return InsertHtmlFunction.apply(securityContext, parent, htmlSource);

			} else {

				throw new FrameworkException(422, "Cannot execute insert-html action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Object handleReplaceHtmlAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET);
		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without target UUID (data-structr-target attribute).");
		}

		// fetch child ID
		final String childId = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without child UUID (data-child-id attribute).");
		}

		// load child node
		final DOMNode child = StructrApp.getInstance(securityContext).get(DOMNode.class, childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without child (object with ID not found or not a DOMNode).");
		}

		final String sourceObjectId = (String) parameters.get(DOMElement.DATA_BINDING_PARAMETER_SOURCEOBJECT);
		if (sourceObjectId == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without html source object UUID (data-source-object).");
		}

		final String sourceProperty = (String)parameters.get(DOMElement.DATA_BINDING_PARAMETER_SOURCEPROPERTY);
		if (sourceProperty == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without html source property name (data-source-property).");
		}

		final GraphObject sourceObject = StructrApp.getInstance(securityContext).get(NodeInterface.class, sourceObjectId);
		if (sourceObject == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without html source property name (data-source-property).");
		}

		final String htmlSource = sourceObject.getProperty(sourceProperty);
		if (StringUtils.isBlank(htmlSource)) {

			throw new FrameworkException(422, "Cannot execute replace-html action without empty html source.");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : DOMElement.resolveDataTargets(actionContext, this, dataTarget)) {

			if (target instanceof DOMElement) {

				final DOMElement parent = (DOMElement) target;

				return ReplaceDOMChildFunction.apply(securityContext, parent, child, htmlSource);

			} else {

				throw new FrameworkException(422, "Cannot execute replace-html action on " + target.getClass().getSimpleName() + " (must be a DOMElement).");
			}
		}

		return null;
	}

	private Map<String, Object> getMappedEvents() {

		final String mapping = getEventMapping();
		if (mapping != null) {

			return gson.fromJson(mapping, Map.class);
		}

		return null;
	}

	private void removeInternalDataBindingKeys(final java.util.Map<String, java.lang.Object> parameters) {

		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_STRUCTRID);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_STRUCTRIDEXPRESSION);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_STRUCTRTARGET);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_STRUCTRMETHOD);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_STRUCTRACTION);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_STRUCTREVENT);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_STRUCTREVENTS);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_HTMLEVENT);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_CHILDID);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_SOURCEOBJECT);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_SOURCEPROPERTY);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_DATA_TYPE);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_SUCCESS_NOTIFICATIONS);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_FAILURE_NOTIFICATIONS);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_SUCCESS_TARGET);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_FAILURE_TARGET);
		parameters.remove(DOMElement.DATA_BINDING_PARAMETER_SUCCESS_NOTIFICATIONS_CUSTOM_DIALOG_ELEMENT);

	}







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

		// TODO: remove this..
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

				boolean lazyRendering = false;
				final String renderingMode = thisElement.getProperty(StructrApp.key(DOMElement.class, "data-structr-rendering-mode"));

				// lazy rendering can only work if this node is not requested as a partial
				if (renderContext.getPage() != null && renderingMode != null) {
					lazyRendering = true;
				}

				// disable lazy rendering in deployment mode
				if (EditMode.DEPLOYMENT.equals(editMode)) {
					lazyRendering = false;
				}

				// only render children if we are not in a shared component scenario, not in deployment mode and it's not rendered lazily
				if (!lazyRendering && (thisElement.getSharedComponent() == null || !EditMode.DEPLOYMENT.equals(editMode))) {

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

						Scripting.evaluate(renderContext, thisElement, "${" + _sharedComponentConfiguration.trim() + "}", "sharedComponentConfiguration", thisElement.getUuid());
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

				final Logger logger = LoggerFactory.getLogger(DOMElement.class);
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

			// Send deprecation warning
			TransactionCommand.simpleBroadcastDeprecationWarning(
					"EDIT_MODE_BINDING",
					"Edit Mode Binding is deprecated",
					"Element " + thisElement.getUuid() + " uses deprecated frontend edit-mode bindings. This feature is deprecated and will be removed in a coming version. Migration is needed to ensure future functioning.",
					thisElement.getUuid()
			);
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
			final Class type  = thisElement.getEntityType();
			final String uuid = thisElement.getUuid();

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

			// new: managed attributes (like selected
			thisElement.renderManagedAttributes(out, renderContext.getSecurityContext(), renderContext);

			// include special mode attributes
			switch (editMode) {

				case SHAPES:
				case SHAPES_MINIATURES:

					final boolean isInsertable = thisElement.getProperty(StructrApp.key(DOMElement.class, "data-structr-insert"));
					final boolean isFromWidget = thisElement.getProperty(StructrApp.key(DOMElement.class, "data-structr-from-widget"));

					if (isInsertable || isFromWidget) {
						out.append(" data-structr-id=\"").append(uuid).append("\"");
					}
					break;

				case CONTENT:

					if (depth == 0) {

						String pageId = renderContext.getPageId();

						if (pageId != null) {

							out.append(" data-structr-page=\"").append(pageId).append("\"");
						}
					}

					out.append(" data-structr-id=\"").append(uuid).append("\"");
					break;

				case RAW:

					out.append(" ").append("data-structr-hash").append("=\"").append(thisElement.getIdHash()).append("\"");
					break;

				case WIDGET:
				case DEPLOYMENT:

					final String eventMapping = (String) thisElement.getProperty(StructrApp.key(DOMElement.class, "eventMapping"));
					if (eventMapping != null) {

						out.append(" ").append("data-structr-meta-event-mapping").append("=\"").append(StringEscapeUtils.escapeHtml(eventMapping)).append("\"");
					}
					break;

				case NONE:

					// Get actions in superuser context
					final List<ActionMapping> triggeredActions = (List<ActionMapping>) Iterables.toList((Iterable<? extends ActionMapping>)	StructrApp.getInstance().get(DOMElement.class, thisElement.getUuid()).getProperty(StructrApp.key(DOMElement.class, "triggeredActions")));
					if (triggeredActions != null && !triggeredActions.isEmpty()) {

						final ActionMapping triggeredAction = triggeredActions.get(0);

						// Support for legacy action mapping (simple interactive elements)
						// ensure backwards compatibility with frontend.js before switch to new persistence model
						if (StringUtils.isNotBlank(uuid)) {
							out.append(" data-structr-id=\"").append(uuid).append("\"");
						}
						String eventsString = null;
						final Map<String, Object> mapping = thisElement.getMappedEvents();
						if (mapping != null) {
							eventsString = StringUtils.join(mapping.keySet(), ",");
						}

						// append all stored action mapping keys as data-structr-<key> attributes
						for (final String key : new String[] { "event", "action", "method", "dataType", "idExpression" }) {
							final String value = triggeredAction.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ActionMapping.class, key));
							if (StringUtils.isNotBlank(value)) {
								final String keyHyphenated = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, key);
								out.append(" data-structr-" + keyHyphenated + "=\"").append(value).append("\"");
							}
							if (key.equals("event")) {
								eventsString = (String) value;
							}
						}

						if (eventsString != null) {
							out.append(" data-structr-events=\"").append(eventsString).append("\"");
						}

						// Possible values for success notifications are none, system-alert, inline-text-message, custom-dialog-element
						final String successNotifications = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "successNotifications"));
						if (StringUtils.isNotBlank(successNotifications)) {

							out.append(" data-structr-success-notifications=\"").append(successNotifications).append("\"");

							switch (successNotifications) {

								case ("custom-dialog-linked"):
									out.append(" data-structr-success-notifications-custom-dialog-element=\"").append(generateDataAttributesForIdList(renderContext, triggeredAction, "successNotificationElements")).append("\"");
									break;
								default:

							}
						}

						final String successNotificationsPartial = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "successNotificationsPartial"));
						if (StringUtils.isNotBlank(successNotificationsPartial)) {
							out.append(" data-structr-success-notifications-partial=\"").append(successNotificationsPartial).append("\"");
						}

						// Possible values for failure notifications are none, system-alert, inline-text-message, custom-dialog-element
						final String failureNotifications = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "failureNotifications"));
						if (StringUtils.isNotBlank(failureNotifications)) {
							out.append(" data-structr-failure-notifications=\"").append(failureNotifications).append("\"");
						}

						if (StringUtils.isNotBlank(failureNotifications)) {

							switch (failureNotifications) {

								case ("custom-dialog-linked"):
									out.append(" data-structr-failure-notifications-custom-dialog-element=\"").append(generateDataAttributesForIdList(renderContext, triggeredAction, "failureNotificationElements")).append("\"");
									break;
								default:

							}
						}
						final String failureNotificationsPartial = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "failureNotificationsPartial"));
						if (StringUtils.isNotBlank(failureNotificationsPartial)) {
							out.append(" data-structr-failure-notifications-partial=\"").append(failureNotificationsPartial).append("\"");
						}

						// Possible values for the success behaviour are nothing, full-page-reload, partial-refresh, navigate-to-url, fire-event
						final String successBehaviour = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "successBehaviour"));
						final String successPartial   = triggeredAction.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ActionMapping.class, "successPartial"));
						final String successURL       = triggeredAction.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ActionMapping.class, "successURL"));
						final String successEvent     = triggeredAction.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ActionMapping.class, "successEvent"));

						String successTargetString = null;

						if (StringUtils.isNotBlank(successBehaviour)) {

							switch (successBehaviour) {
								case "partial-refresh":
									successTargetString = successPartial;
									break;
								case "partial-refresh-linked":
									successTargetString = generateDataAttributesForIdList(renderContext, triggeredAction, "successTargets");
									break;
								case "navigate-to-url":
									successTargetString = "url:" + successURL;
									break;
								case "fire-event":
									successTargetString = "event:" + successEvent;
									break;
								case "full-page-reload":
									successTargetString = "url:";
									break;
								case "sign-out":
									successTargetString = "sign-out";
									break;
								case "none":
								default:
									successTargetString = null;
							}
						}

						final String idExpression = triggeredAction.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ActionMapping.class, "idExpression"));
						if (StringUtils.isNotBlank(idExpression)) {
							out.append(" data-structr-target=\"").append(idExpression).append("\"");
						}

						final String action = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "action"));
						if ("create".equals(action)) {
							final String dataType = triggeredAction.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ActionMapping.class, "dataType"));
							if (StringUtils.isNotBlank(dataType)) {
								out.append(" data-structr-target=\"").append(dataType).append("\"");
							}
						}

						if (StringUtils.isNotBlank(successTargetString)) {
							out.append(" data-structr-reload-target=\"").append(successTargetString).append("\""); // Legacy, deprecated
							out.append(" data-structr-success-target=\"").append(successTargetString).append("\"");
						}


						// Possible values for the failure behaviour are nothing, full-page-reload, partial-refresh, navigate-to-url, fire-event
						final String failureBehaviour = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "failureBehaviour"));
						final String failurePartial   = triggeredAction.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ActionMapping.class, "failurePartial"));
						final String failureURL       = triggeredAction.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ActionMapping.class, "failureURL"));
						final String failureEvent     = triggeredAction.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ActionMapping.class, "failureEvent"));

						String failureTargetString = null;

						if (StringUtils.isNotBlank(failureBehaviour)) {

							switch (failureBehaviour) {
								case "partial-refresh":
									failureTargetString = failurePartial;
									break;
								case "partial-refresh-linked":
									failureTargetString = generateDataAttributesForIdList(renderContext, triggeredAction, "failureTargets");
									break;
								case "navigate-to-url":
									failureTargetString = "url:" + failureURL;
									break;
								case "fire-event":
									failureTargetString = "event:" + failureEvent;
									break;
								case "full-page-reload":
									failureTargetString = "url:";
									break;
								case "sign-out":
									failureTargetString = "sign-out";
									break;
								case "none":
								default:
									failureTargetString = null;
							}
						}

						if (StringUtils.isNotBlank(failureTargetString)) {
							out.append(" data-structr-failure-target=\"").append(failureTargetString).append("\"");
						}
						
						
//						{ // TODO: Migrate tree handling to new action mapping
//							// toggle-tree-item
//							if (mapping.containsValue("toggle-tree-item")) {
//
//								final String targetValue = thisElement.getPropertyWithVariableReplacement(renderContext, targetKey);
//								final String key = thisElement.getTreeItemSessionIdentifier(targetValue);
//								final boolean open = thisElement.getSessionAttribute(renderContext.getSecurityContext(), key) != null;
//
//								out.append(" data-tree-item-state=\"").append(open ? "open" : "closed").append("\"");
//							}
//						}


						// TODO: Add support for multiple triggered actions.
						//  At the moment, backend and frontend code only support one triggered action,
						// even though the data model has a ManyToMany rel between triggerElements and triggeredActions
						for (final ParameterMapping parameterMapping : (Iterable<? extends ParameterMapping>) triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "parameterMappings"))) {

							final String parameterType = parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "parameterType"));
							final String parameterName = parameterMapping.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ParameterMapping.class, "parameterName"));

							if (parameterType == null || parameterName == null) {
								// Ignore incomplete parameter mapping
								continue;
							}

							final String nameAttributeHyphenated = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, parameterName);

							switch (parameterType) {

								case "user-input":
									final DOMElement element   = parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "inputElement"));

									if (element != null) {

										final String elementCssId = element.getPropertyWithVariableReplacement(renderContext, StructrApp.key(DOMElement.class, "_html_id"));

										if (elementCssId != null) {

											out.append(" data-").append(nameAttributeHyphenated).append("=\"css(#").append(elementCssId).append(")\"");

										} else {

											out.append(" data-").append(nameAttributeHyphenated).append("=\"id(").append(element.getUuid()).append(")\"");
										}

									}
									break;

								case "constant-value":
									final String constantValue = parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "constantValue"));
									// Could be 'json(...)' or a simple value
									out.append(" data-").append(nameAttributeHyphenated).append("=\"").append(escapeForHtmlAttributes(constantValue)).append("\"");
									break;

								case "script-expression":
									final String scriptExpression = parameterMapping.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ParameterMapping.class, "scriptExpression"));
									out.append(" data-").append(nameAttributeHyphenated).append("=\"").append(escapeForHtmlAttributes(scriptExpression)).append("\"");
									break;

								case "page-param":
									// Name of the request parameter for pager 'page'
									final String value = renderContext.getRequestParameter(parameterName);
									// special handling for pagination (migrated code)
									switch (action) {

										case "prev-page":
										case "previous-page":
											out.append(" data-structr-target=\"").append(parameterName).append("\"");
											final int prev = DOMElement.intOrOne(value);
											out.append(" data-").append(parameterName).append("=\"").append(String.valueOf(Math.max(1, prev - 1))).append("\"");
											break;

										case "next-page":
											out.append(" data-structr-target=\"").append(parameterName).append("\"");
											final int next = DOMElement.intOrOne(value);
											out.append(" data-").append(parameterName).append("=\"").append(String.valueOf(next + 1)).append("\"");
											break;

										case "first-page":
											out.append(" data-structr-target=\"").append(parameterName).append("\"");
											out.append(" data-").append(parameterName).append("=\"1\"");
											break;

										case "last-page":
											// should we really count all objects?
											out.append(" data-structr-target=\"").append(parameterName).append("\"");
											out.append(" data-").append(parameterName).append("=\"1000\"");
											break;

										default:
											break;
									}

									break;
								case "pagesize-param":
									// TODO: Implement additional parameter for page size
									// Name of the request parameter for pager 'pageSize'
									break;

								default:

							}
						}

					}

					// make repeater data object ID available
					final GraphObject repeaterDataObject = renderContext.getDataObject();
					if (repeaterDataObject != null) {

						out.append(" data-repeater-data-object-id=\"").append(repeaterDataObject.getUuid()).append("\"");
					}

					final DOMElement thisElementWithSuperuserContext = StructrApp.getInstance().get(DOMElement.class, thisElement.getUuid());

					if (isTargetElement(thisElementWithSuperuserContext)) {

						out.append(" data-structr-id=\"").append(uuid).append("\"");

						// make current object ID available in reload targets
						final GraphObject current = renderContext.getDetailsDataObject();
						if (current != null) {

							out.append(" data-current-object-id=\"").append(current.getUuid()).append("\"");
						}

						// realization: all dynamic parameters must be stored on the reload target!
						final HttpServletRequest request = renderContext.getRequest();
						if (request != null) {

							final Map<String, String[]> parameters = request.getParameterMap();

							for (final Entry<String, String[]> entry : parameters.entrySet()) {

								final String key      = entry.getKey();
								final String[] values = entry.getValue();

								if (values.length > 0 && !RequestParameterBlacklist.contains(key)) {

									out.append(" data-request-").append(DOMElement.toHtmlAttributeName(key)).append("=\"").append(values[0]).append("\"");
								}
							}
						}

						final String encodedRenderState = renderContext.getEncodedRenderState();
						if (encodedRenderState != null) {

							out.append(" data-structr-render-state=\"").append(encodedRenderState).append("\"");
						}

					}

					if (thisElement.getProperty(StructrApp.key(DOMElement.class, "data-structr-rendering-mode")) != null) {

						out.append(" data-structr-id=\"").append(uuid).append("\"");
						out.append(" data-structr-delay-or-interval=\"").append(thisElement.getProperty(StructrApp.key(DOMElement.class, "data-structr-delay-or-interval"))).append("\"");

					}

					if (renderContext.isTemplateRoot(uuid)) {

						// render template ID into output so it can be re-used
						out.append(" data-structr-template-id=\"").append(renderContext.getTemplateId()).append("\"");

					}

					final PropertyKey<Iterable<ParameterMapping>> parameterMappingsKey     = StructrApp.key(DOMElement.class, "parameterMappings");
					final Iterable<? extends ParameterMapping>	parameterMappings = thisElementWithSuperuserContext.getProperty(parameterMappingsKey);

					final boolean isParameterElement = parameterMappings.iterator().hasNext();

					if (thisElementWithSuperuserContext instanceof TemplateElement || isParameterElement) {

						// render ID into output so it can be re-used
						out.append(" data-structr-id=\"").append(uuid).append("\"");
					}

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

	public static List<GraphObject> resolveDataTargets(final ActionContext actionContext, final DOMElement thisElement, final String dataTarget) throws FrameworkException {

		final App app                   = StructrApp.getInstance(thisElement.getSecurityContext());
		final List<GraphObject> targets = new LinkedList<>();

		if (dataTarget.length() >= 32) {

			// list of UUIDs or single UUID, below code should handle both
			for (final String part : dataTarget.split(",")) {

				final String cleaned = part.trim();
				if (StringUtils.isNotBlank(cleaned) && cleaned.length() == 32) {

					final NodeInterface node = app.getNodeById(cleaned);
					if (node != null) {

						targets.add(node);

					} else {

						final RelationshipInterface rel = app.getRelationshipById(cleaned);
						if (rel != null) {

							targets.add(rel);
						}
					}
				}
			}

		} else {

			// evaluate single keyword
			final Object result = thisElement.evaluate(actionContext, dataTarget, null, new EvaluationHints(), 1, 1);
			if (result != null) {

				if (result instanceof Iterable) {

					for (final Object o : (Iterable)result) {

						if (o instanceof GraphObject) {
							targets.add((GraphObject)o);
						}
					}

				} else if (result instanceof GraphObject) {

					targets.add((GraphObject)result);
				}
			}

		}

		return targets;
	}

	public static void updateReloadTargets(final DOMElement thisElement) throws FrameworkException {

		try {

			final PropertyKey<Iterable<DOMElement>> reloadSourcesKey     = StructrApp.key(DOMElement.class, "reloadSources");
			final PropertyKey<Iterable<DOMElement>> reloadTargetsKey     = StructrApp.key(DOMElement.class, "reloadTargets");
			final PropertyKey<String> reloadTargetKey                    = StructrApp.key(DOMElement.class, "data-structr-reload-target");
			final List<DOMElement> actualReloadSources                   = new LinkedList<>();
			final List<DOMElement> actualReloadTargets                   = new LinkedList<>();
			final org.jsoup.nodes.Element matchElement                   = thisElement.getMatchElement();
			final String reloadTargets                                   = thisElement.getProperty(reloadTargetKey);
			final Page page                                              = thisElement.getOwnerDocument();

			if (page != null) {

				for (final DOMNode possibleReloadTargetNode : page.getElements()) {

					if (possibleReloadTargetNode instanceof DOMElement) {

						final DOMElement possibleTarget       = (DOMElement)possibleReloadTargetNode;
						final org.jsoup.nodes.Element element = possibleTarget.getMatchElement();
						final String otherReloadTarget        = possibleTarget.getProperty(reloadTargetKey);

						if (reloadTargets != null && element != null) {

							for (final String part : reloadTargets.split(",")) {

								final String targetSelector = part.trim();

								try {

									if (StringUtils.isNotBlank(targetSelector) && element.select(targetSelector).first() != null) {

										actualReloadTargets.add(possibleTarget);
									}

								} catch (Throwable t) {}
							}
						}

						if (otherReloadTarget != null && matchElement != null) {

							for (final String part : otherReloadTarget.split(",")) {

								final String targetSelector = part.trim();

								try {

									if (StringUtils.isNotBlank(targetSelector) && matchElement.select(targetSelector).first() != null) {

										actualReloadSources.add(possibleTarget);
									}

								} catch (Throwable t) {}
							}
						}
					}
				}
			}

			// update reload targets with list from above
			thisElement.setProperty(reloadSourcesKey, actualReloadSources);
			thisElement.setProperty(reloadTargetsKey, actualReloadTargets);

		} catch (Throwable t) {

			t.printStackTrace();
		}
	}

	private org.jsoup.nodes.Element getMatchElement() {

		final PropertyKey<String> classKey    = StructrApp.key(DOMElement.class, "_html_class");
		final PropertyKey<String> idKey       = StructrApp.key(DOMElement.class, "_html_id");
		final String tag                      = getTag();

		if (StringUtils.isNotBlank(tag)) {

			final org.jsoup.nodes.Element element = new org.jsoup.nodes.Element(tag);
			final String classes                  = getProperty(classKey);

			if (classes != null) {

				for (final String css : classes.split(" ")) {

					if (StringUtils.isNotBlank(css)) {

						element.addClass(css.trim());
					}
				}
			}

			final String name = getProperty(AbstractNode.name);
			if (name != null) {
				element.attr("name", name);
			}

			final String htmlId = getProperty(idKey);
			if (htmlId != null) {

				element.attr("id", htmlId);
			}

			return element;
		}

		return null;
	}

	public static boolean isTargetElement(final DOMElement thisElement) {

		final PropertyKey<Boolean> isManualReloadTargetKey          = StructrApp.key(DOMElement.class, "data-structr-manual-reload-target");
		final boolean isManualReloadTarget                          = thisElement.getProperty(isManualReloadTargetKey);

		final PropertyKey<Iterable<DOMElement>> reloadSourcesKey    = StructrApp.key(DOMElement.class, "reloadSources");
		final List<DOMElement> reloadSources                        = Iterables.toList(thisElement.getProperty(reloadSourcesKey));

		final PropertyKey<Iterable<DOMNode>> reloadingActionsKey = StructrApp.key(DOMNode.class, "reloadingActions");
		final List<DOMNode> reloadingActions                     = Iterables.toList(thisElement.getProperty(reloadingActionsKey));

		final PropertyKey<Iterable<DOMNode>> failureActionsKey = StructrApp.key(DOMNode.class, "failureActions");
		final List<DOMNode> failureActions                     = Iterables.toList(thisElement.getProperty(failureActionsKey));

		final PropertyKey<Iterable<DOMNode>> successNotificationActionsKey = StructrApp.key(DOMNode.class, "successNotificationActions");
		final List<DOMNode> successNotificationActions                     = Iterables.toList(thisElement.getProperty(successNotificationActionsKey));

		final PropertyKey<Iterable<DOMNode>> failureNotificationActionsKey = StructrApp.key(DOMNode.class, "failureNotificationActions");
		final List<DOMNode> failureNotificationActions                     = Iterables.toList(thisElement.getProperty(failureNotificationActionsKey));

		return isManualReloadTarget || !reloadSources.isEmpty() || !reloadingActions.isEmpty() || !failureActions.isEmpty() || !successNotificationActions.isEmpty() || !failureNotificationActions.isEmpty();
	}

	private static int intOrOne(final String source) {

		if (source != null) {

			try {

				return Integer.valueOf(source);

			} catch (Throwable t) {
			}
		}

		return 1;
	}

	private static String toHtmlAttributeName(final String camelCaseName) {

		final StringBuilder buf = new StringBuilder();

		camelCaseName.chars().forEach(c -> {

			if (Character.isUpperCase(c)) {

				buf.append("-");
				c = Character.toLowerCase(c);

			}

			buf.append(Character.toString(c));
		});

		return buf.toString();
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

	private static String generateDataAttributesForIdList(final RenderContext renderContext, final ActionMapping triggeredAction, final String propertyKey) {

		String resultString = "";

		final List<DOMNode> nodeList = (List<DOMNode>) Iterables.toList((Iterable<? extends DOMNode >) triggeredAction.getProperty(StructrApp.key(ActionMapping.class, propertyKey)));
		if (!nodeList.isEmpty()) {
			int i=1;
			for (final DOMNode node : nodeList) {

				// Create CSS selector for data-structr-id
				String selector = "[data-structr-id='" + node.getUuid() + "']";
				final String key = node.getDataKey();
				if (key != null) {
					selector += "[data-repeater-data-object-id='" + renderContext.getDataNode(key).getUuid() + "']";
				}
				resultString += selector + (i < nodeList.size() ? "," : "");
				i++;
			}

		}

		return resultString;
	}
}
