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

import com.google.common.base.CaseFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.api.AbstractMethod;
import org.structr.core.api.Arguments;
import org.structr.core.api.Methods;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractRelationship;
import org.structr.core.entity.PrincipalInterface;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.property.*;
import org.structr.core.script.Scripting;
import org.structr.rest.api.RESTCall;
import org.structr.rest.servlet.AbstractDataServlet;
import org.structr.schema.ConfigurationProvider;
import org.structr.schema.NonIndexed;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Actions;
import org.structr.schema.action.EvaluationHints;
import org.structr.web.common.AsyncBuffer;
import org.structr.web.common.EventContext;
import org.structr.web.common.HtmlProperty;
import org.structr.web.common.RenderContext;
import org.structr.web.common.RenderContext.EditMode;
import org.structr.web.entity.User;
import org.structr.web.entity.dom.relationship.DOMElementINPUT_ELEMENTParameterMapping;
import org.structr.web.entity.dom.relationship.DOMElementRELOADSDOMElement;
import org.structr.web.entity.dom.relationship.DOMElementTRIGGERED_BYActionMapping;
import org.structr.web.entity.dom.relationship.DOMNodeCONTAINSDOMNode;
import org.structr.web.entity.event.ActionMapping;
import org.structr.web.entity.event.ParameterMapping;
import org.structr.web.entity.html.TemplateElement;
import org.structr.web.function.InsertHtmlFunction;
import org.structr.web.function.RemoveDOMChildFunction;
import org.structr.web.function.ReplaceDOMChildFunction;
import org.structr.web.resource.LoginResourceHandler;
import org.structr.web.resource.LogoutResourceHandler;
import org.structr.web.resource.RegistrationResourceHandler;
import org.structr.web.resource.ResetPasswordResourceHandler;
import org.structr.web.servlet.HtmlServlet;
import org.w3c.dom.*;

import java.util.*;
import java.util.Map.Entry;

public class DOMElement extends DOMNode implements Element, NamedNodeMap, NonIndexed {

	public static final String GET_HTML_ATTRIBUTES_CALL = "return (Property[]) org.apache.commons.lang3.ArrayUtils.addAll(super.getHtmlAttributes(), _html_View.properties());";

	public static final Set<String> RequestParameterBlacklist = Set.of(HtmlServlet.ENCODED_RENDER_STATE_PARAMETER_NAME);

	public static final String lowercaseBodyName        = "body";

	public static final String EVENT_ACTION_MAPPING_PARAMETER_HTMLEVENT                                   = "htmlEvent";
	public static final String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION                         = "structrIdExpression";
	public static final String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET                               = "structrTarget";
	public static final String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRDATATYPE                             = "structrDataType";
	public static final String EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD                               = "structrMethod";
	public static final String EVENT_ACTION_MAPPING_PARAMETER_CHILDID                                     = "childId";
	public static final String EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT                                = "sourceObject";
	public static final String EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY                              = "sourceProperty";

	public static final int HtmlPrefixLength            = PropertyView.Html.length();
	public static final Gson gson                       = new GsonBuilder().create();

	public static final Property<Iterable<DOMElement>> reloadSourcesProperty           = new StartNodes<>("reloadSources", DOMElementRELOADSDOMElement.class).category(EVENT_ACTION_MAPPING_CATEGORY).partOfBuiltInSchema();
	public static final Property<Iterable<DOMElement>> reloadTargetsProperty           = new EndNodes<>("reloadTargets", DOMElementRELOADSDOMElement.class).category(EVENT_ACTION_MAPPING_CATEGORY).partOfBuiltInSchema();
	public static final Property<Iterable<ActionMapping>> triggeredActionsProperty     = new EndNodes<>("triggeredActions", DOMElementTRIGGERED_BYActionMapping.class).category(EVENT_ACTION_MAPPING_CATEGORY).partOfBuiltInSchema();
	public static final Property<Iterable<ParameterMapping>> parameterMappingsProperty = new EndNodes<>("parameterMappings", DOMElementINPUT_ELEMENTParameterMapping.class).category(EVENT_ACTION_MAPPING_CATEGORY).partOfBuiltInSchema();

	public static final Property<String> tagProperty              = new StringProperty("tag").indexed().category(PAGE_CATEGORY).partOfBuiltInSchema();
	public static final Property<String> pathProperty             = new StringProperty("path").indexed().partOfBuiltInSchema();
	public static final Property<String> partialUpdateKeyProperty = new StringProperty("partialUpdateKey").indexed().partOfBuiltInSchema();

	public static final Property<Boolean> hasSharedComponent         = new BooleanProperty("hasSharedComponent").indexed().partOfBuiltInSchema();
	public static final Property<Boolean> manualReloadTargetProperty = new BooleanProperty("data-structr-manual-reload-target").category(EVENT_ACTION_MAPPING_CATEGORY).hint("Identifies this element as a manual reload target, this is necessary when using repeaters as reload targets.").partOfBuiltInSchema();
	public static final Property<Boolean> dataFromWidgetProperty     = new BooleanProperty("data-structr-from-widget").partOfBuiltInSchema();

	public static final Property<String> eventMappingProperty       = new StringProperty("eventMapping").category(EVENT_ACTION_MAPPING_CATEGORY).hint("A mapping between the desired Javascript event (click, drop, dragOver, ...) and the server-side event that should be triggered: (create | update | delete | <method name>).").partOfBuiltInSchema();
	// probably useless ATM because EAM does not support trees yet
	public static final Property<String> dataTreeChildrenProperty   = new StringProperty("data-structr-tree-children").category(EVENT_ACTION_MAPPING_CATEGORY).hint("Toggles automatic visibility for tree child items when the 'toggle-tree-item' event is mapped. This field must contain the data key on which the tree is based, e.g. 'item'.").partOfBuiltInSchema();
	public static final Property<String> dataReloadTargetProperty   = new StringProperty("data-structr-reload-target").category(EVENT_ACTION_MAPPING_CATEGORY).hint("CSS selector that specifies which partials to reload.").partOfBuiltInSchema();
	public static final Property<String> renderingModeProperty      = new StringProperty("data-structr-rendering-mode").category(EVENT_ACTION_MAPPING_CATEGORY).hint("Rendering mode, possible values are empty (default for eager rendering), 'load' to render when the DOM document has finished loading, 'delayed' like 'load' but with a fixed delay, 'visible' to render when the element comes into view and 'periodic' to render the element with periodic updates with a given interval").partOfBuiltInSchema();
	public static final Property<String> delayOrIntervalProperty    = new StringProperty("data-structr-delay-or-interval").category(EVENT_ACTION_MAPPING_CATEGORY).hint("Delay or interval in milliseconds for 'delayed' or 'periodic' rendering mode").partOfBuiltInSchema();
	public static final Property<String> onAbortProperty            = new StringProperty("_html_onabort").partOfBuiltInSchema();
	public static final Property<String> onBlurProperty             = new StringProperty("_html_onblur").partOfBuiltInSchema();
	public static final Property<String> onCanPlayProperty          = new StringProperty("_html_oncanplay").partOfBuiltInSchema();
	public static final Property<String> onCanPlayThroughProperty   = new StringProperty("_html_oncanplaythrough").partOfBuiltInSchema();
	public static final Property<String> onChangeProperty           = new StringProperty("_html_onchange").partOfBuiltInSchema();
	public static final Property<String> onClickProperty            = new StringProperty("_html_onclick").partOfBuiltInSchema();
	public static final Property<String> onContextMenuProperty      = new StringProperty("_html_oncontextmenu").partOfBuiltInSchema();
	public static final Property<String> onDblClickProperty         = new StringProperty("_html_ondblclick").partOfBuiltInSchema();
	public static final Property<String> onDragProperty             = new StringProperty("_html_ondrag").partOfBuiltInSchema();
	public static final Property<String> onDragEndProperty          = new StringProperty("_html_ondragend").partOfBuiltInSchema();
	public static final Property<String> onDragEnterProperty        = new StringProperty("_html_ondragenter").partOfBuiltInSchema();
	public static final Property<String> onDragLeaveProperty        = new StringProperty("_html_ondragleave").partOfBuiltInSchema();
	public static final Property<String> onDragOverProperty         = new StringProperty("_html_ondragover").partOfBuiltInSchema();
	public static final Property<String> onDragStartProperty        = new StringProperty("_html_ondragstart").partOfBuiltInSchema();
	public static final Property<String> onDropProperty             = new StringProperty("_html_ondrop").partOfBuiltInSchema();
	public static final Property<String> onDurationChangeProperty   = new StringProperty("_html_ondurationchange").partOfBuiltInSchema();
	public static final Property<String> onEmptiedProperty          = new StringProperty("_html_onemptied").partOfBuiltInSchema();
	public static final Property<String> onEndedProperty            = new StringProperty("_html_onended").partOfBuiltInSchema();
	public static final Property<String> onErrorProperty            = new StringProperty("_html_onerror").partOfBuiltInSchema();
	public static final Property<String> onFocusProperty            = new StringProperty("_html_onfocus").partOfBuiltInSchema();
	public static final Property<String> onInputProperty            = new StringProperty("_html_oninput").partOfBuiltInSchema();
	public static final Property<String> onInvalidProperty          = new StringProperty("_html_oninvalid").partOfBuiltInSchema();
	public static final Property<String> onKeyDownProperty          = new StringProperty("_html_onkeydown").partOfBuiltInSchema();
	public static final Property<String> onKeyPressProperty         = new StringProperty("_html_onkeypress").partOfBuiltInSchema();
	public static final Property<String> onKeyUpProperty            = new StringProperty("_html_onkeyup").partOfBuiltInSchema();
	public static final Property<String> onLoadProperty             = new StringProperty("_html_onload").partOfBuiltInSchema();
	public static final Property<String> onLoadedDataProperty       = new StringProperty("_html_onloadeddata").partOfBuiltInSchema();
	public static final Property<String> onLoadedMetadataProperty   = new StringProperty("_html_onloadedmetadata").partOfBuiltInSchema();
	public static final Property<String> onLoadStartProperty        = new StringProperty("_html_onloadstart").partOfBuiltInSchema();
	public static final Property<String> onMouseDownProperty        = new StringProperty("_html_onmousedown").partOfBuiltInSchema();
	public static final Property<String> onMouseMoveProperty        = new StringProperty("_html_onmousemove").partOfBuiltInSchema();
	public static final Property<String> onMouseOutProperty         = new StringProperty("_html_onmouseout").partOfBuiltInSchema();
	public static final Property<String> onMouseOverProperty        = new StringProperty("_html_onmouseover").partOfBuiltInSchema();
	public static final Property<String> onMouseUpProperty          = new StringProperty("_html_onmouseup").partOfBuiltInSchema();
	public static final Property<String> onMouseWheelProperty       = new StringProperty("_html_onmousewheel").partOfBuiltInSchema();
	public static final Property<String> onPauseProperty            = new StringProperty("_html_onpause").partOfBuiltInSchema();
	public static final Property<String> onPlayProperty             = new StringProperty("_html_onplay").partOfBuiltInSchema();
	public static final Property<String> onPlayingProperty          = new StringProperty("_html_onplaying").partOfBuiltInSchema();
	public static final Property<String> onProgressProperty         = new StringProperty("_html_onprogress").partOfBuiltInSchema();
	public static final Property<String> onRateChangeProperty       = new StringProperty("_html_onratechange").partOfBuiltInSchema();
	public static final Property<String> onReadyStateChangeProperty = new StringProperty("_html_onreadystatechange").partOfBuiltInSchema();
	public static final Property<String> onResetProperty            = new StringProperty("_html_onreset").partOfBuiltInSchema();
	public static final Property<String> onScrollProperty           = new StringProperty("_html_onscroll").partOfBuiltInSchema();
	public static final Property<String> onSeekedProperty           = new StringProperty("_html_onseeked").partOfBuiltInSchema();
	public static final Property<String> onSeekingProperty          = new StringProperty("_html_onseeking").partOfBuiltInSchema();
	public static final Property<String> onSelectProperty           = new StringProperty("_html_onselect").partOfBuiltInSchema();
	public static final Property<String> onShowProperty             = new StringProperty("_html_onshow").partOfBuiltInSchema();
	public static final Property<String> onStalledProperty          = new StringProperty("_html_onstalled").partOfBuiltInSchema();
	public static final Property<String> onSubmitProperty           = new StringProperty("_html_onsubmit").partOfBuiltInSchema();
	public static final Property<String> onSuspendProperty          = new StringProperty("_html_onsuspend").partOfBuiltInSchema();
	public static final Property<String> onTimeUpdateProperty       = new StringProperty("_html_ontimeupdate").partOfBuiltInSchema();
	public static final Property<String> onVolumechangeProperty     = new StringProperty("_html_onvolumechange").partOfBuiltInSchema();
	public static final Property<String> onWaitingProperty          = new StringProperty("_html_onwaiting").partOfBuiltInSchema();
	public static final Property<String> htmlDataProperty           = new StringProperty("_html_data").partOfBuiltInSchema();

	// Core attributes
	public static final Property<String> htmlAcceskeyProperty        = new StringProperty("_html_accesskey").partOfBuiltInSchema();
	public static final Property<String> htmlClassProperty           = new StringProperty("_html_class").partOfBuiltInSchema();
	public static final Property<String> htmlContentEditableProperty = new StringProperty("_html_contenteditable").partOfBuiltInSchema();
	public static final Property<String> htmlContextMenuProperty     = new StringProperty("_html_contextmenu").partOfBuiltInSchema();
	public static final Property<String> htmlDirProperty             = new StringProperty("_html_dir").partOfBuiltInSchema();
	public static final Property<String> htmlDraggableProperty       = new StringProperty("_html_draggable").partOfBuiltInSchema();
	public static final Property<String> htmlDropzoneProperty        = new StringProperty("_html_dropzone").partOfBuiltInSchema();
	public static final Property<String> htmlHiddenProperty          = new StringProperty("_html_hidden").partOfBuiltInSchema();
	public static final Property<String> htmlIdProperty              = new StringProperty("_html_id").indexed().partOfBuiltInSchema();
	public static final Property<String> htmlLangProperty            = new StringProperty("_html_lang").partOfBuiltInSchema();
	public static final Property<String> htmlSpellcheckProperty      = new StringProperty("_html_spellcheck").partOfBuiltInSchema();
	public static final Property<String> htmlStyleProperty           = new StringProperty("_html_style").partOfBuiltInSchema();
	public static final Property<String> htmlTabindexProperty        = new StringProperty("_html_tabindex").partOfBuiltInSchema();
	public static final Property<String> htmlTitleProperty           = new StringProperty("_html_title").partOfBuiltInSchema();
	public static final Property<String> htmlTranslateProperty       = new StringProperty("_html_translate").partOfBuiltInSchema();

	// new properties for Polymer support
	public static final Property<String> htmlIsProperty         = new StringProperty("_html_is").partOfBuiltInSchema();
	public static final Property<String> htmlPropertiesProperty = new StringProperty("_html_properties").partOfBuiltInSchema();

	// The role attribute, see http://www.w3.org/TR/role-attribute/
	public static final Property<String> htmlRoleProperty            = new StringProperty("_html_role").partOfBuiltInSchema();

	public static final View defaultView = new View(DOMElement.class, PropertyView.Public,
		tagProperty, pathProperty, partialUpdateKeyProperty, isDOMNodeProperty, pageIdProperty, parentProperty, sharedComponentIdProperty, syncedNodesIdsProperty,
		name, childrenProperty, dataKeyProperty, cypherQueryProperty, restQueryProperty, functionQueryProperty
	);

	public static final View uiView = new View(DOMElement.class, PropertyView.Ui,
		tagProperty, pathProperty, partialUpdateKeyProperty, htmlClassProperty, htmlIdProperty, sharedComponentConfigurationProperty,
		isDOMNodeProperty, pageIdProperty, parentProperty, sharedComponentIdProperty, syncedNodesIdsProperty, dataStructrIdProperty, childrenProperty,
		childrenIdsProperty, showForLocalesProperty, hideForLocalesProperty, showConditionsProperty, hideConditionsProperty, dataKeyProperty, cypherQueryProperty,
		restQueryProperty, functionQueryProperty, renderingModeProperty, delayOrIntervalProperty, dataFromWidgetProperty, dataTreeChildrenProperty,
		dataReloadTargetProperty, eventMappingProperty, triggeredActionsProperty, reloadingActionsProperty, failureActionsProperty, successNotificationActionsProperty,
		failureNotificationActionsProperty, manualReloadTargetProperty
	);

	public static final View htmlView = new View(DOMElement.class, PropertyView.Html,
		onAbortProperty, onBlurProperty, onCanPlayProperty, onCanPlayThroughProperty, onChangeProperty, onClickProperty, onContextMenuProperty, onDblClickProperty,
		onDragProperty, onDragEndProperty, onDragEnterProperty, onDragLeaveProperty, onDragOverProperty, onDragStartProperty, onDropProperty, onDurationChangeProperty,
		onEmptiedProperty, onEndedProperty, onErrorProperty, onFocusProperty, onInputProperty, onInvalidProperty, onKeyDownProperty, onKeyPressProperty, onKeyUpProperty,
		onLoadProperty, onLoadedDataProperty, onLoadedMetadataProperty, onLoadStartProperty, onMouseDownProperty, onMouseMoveProperty, onMouseOutProperty,
		onMouseOverProperty, onMouseUpProperty, onMouseWheelProperty, onPauseProperty, onPlayProperty, onPlayingProperty, onProgressProperty, onRateChangeProperty,
		onReadyStateChangeProperty, onResetProperty, onScrollProperty, onSeekedProperty, onSeekingProperty, onSelectProperty, onShowProperty, onStalledProperty,
		onSubmitProperty, onSuspendProperty, onTimeUpdateProperty, onVolumechangeProperty, onWaitingProperty, htmlDataProperty,

		htmlAcceskeyProperty, htmlClassProperty, htmlContentEditableProperty, htmlContextMenuProperty, htmlDirProperty, htmlDraggableProperty, htmlDropzoneProperty,
		htmlHiddenProperty, htmlIdProperty, htmlLangProperty, htmlSpellcheckProperty, htmlStyleProperty, htmlTabindexProperty, htmlTitleProperty, htmlTranslateProperty,

		htmlRoleProperty, htmlIsProperty, htmlPropertiesProperty
	);

	/*

		final LicenseManager licenseManager = Services.getInstance().getLicenseManager();
		if (licenseManager == null || licenseManager.isModuleLicensed("api-builder")) {

			type.addViewProperty(PropertyView.Public, "flow");
			type.addViewProperty(PropertyView.Ui, "flow");
		}
	*/

	@Override
	public void onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		super.onCreation(securityContext, errorBuffer);

		updateReloadTargets();
	}

	@Override
	public void onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		super.onModification(securityContext, errorBuffer, modificationQueue);

		updateReloadTargets();
	}

	// ----- public methods -----
	public String getTag() {
		return getProperty(tagProperty);
	}

	public String getEventMapping() {
		return getProperty(eventMappingProperty);
	}

	public String getCssClass() {
		return getProperty(htmlClassProperty);
	}

	public Property[] getHtmlAttributes() {
		return htmlView.properties();
	}

	@Override
	public Set<PropertyKey> getPropertyKeys(final String propertyView) {

		final Set<PropertyKey> allProperties = new LinkedHashSet<>();
		final Set<PropertyKey> htmlAttrs     = super.getPropertyKeys(propertyView);

		for (final PropertyKey attr : htmlAttrs) {
			allProperties.add(attr);
		}

		allProperties.addAll(getDataPropertyKeys());

		return allProperties;
	}

	@Export
	public Object event(final SecurityContext securityContext, final java.util.Map<String, java.lang.Object> parameters) throws FrameworkException {

		final ActionContext actionContext = new ActionContext(securityContext);
		final EventContext  eventContext  = new EventContext();
		final String        event         = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_HTMLEVENT);
		final String        action;

		if (event == null) {
			throw new FrameworkException(422, "Cannot execute action without event name (htmlEvent property).");
		}

		ActionMapping triggeredAction;

		final List<ActionMapping> triggeredActions = (List<ActionMapping>) Iterables.toList((Iterable<? extends ActionMapping>) StructrApp.getInstance().get(DOMElement.class, this.getUuid()).getProperty(triggeredActionsProperty));
		if (triggeredActions != null && !triggeredActions.isEmpty()) {

			triggeredAction = triggeredActions.get(0);
			action = triggeredAction.getProperty(ActionMapping.actionProperty);

		} else {

			throw new FrameworkException(422, "Cannot execute action without action defined on this DOMElement: " + this);
		}

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

//			case "open-tree-item":
//			case "close-tree-item":
//			case "toggle-tree-item":
//				handleTreeAction(actionContext, parameters, eventContext, event);
//				break;

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
				final String method = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD);
				return handleCustomAction(actionContext, parameters, eventContext, method);
		}

		return eventContext;
	}

	private Object handleSignInAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		removeInternalDataBindingKeys(parameters);

		final PrincipalInterface currentUser              = actionContext.getSecurityContext().getUser(false);
		final LoginResourceHandler loginResource = new LoginResourceHandler(new RESTCall("/login", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, User.class)));
		final Map<String, Object> properties     = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			properties.put(key, value);
		}

		return loginResource.doPost(actionContext.getSecurityContext(), properties);
	}

	private Object handleSignOutAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		removeInternalDataBindingKeys(parameters);

		final PrincipalInterface currentUser                = actionContext.getSecurityContext().getUser(false);
		final LogoutResourceHandler logoutResource = new LogoutResourceHandler(new RESTCall("/logout", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, User.class)));
		final Map<String, Object> properties       = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			properties.put(key, value);
		}

		return logoutResource.doPost(actionContext.getSecurityContext(), properties);
	}

	private Object handleSignUpAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final PrincipalInterface currentUser          = actionContext.getSecurityContext().getUser(false);
		final Map<String, Object> properties = new LinkedHashMap<>();

		removeInternalDataBindingKeys(parameters);

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			if (value != null) properties.put(key, value);
		}

		final RegistrationResourceHandler registrationResource = new RegistrationResourceHandler(new RESTCall("/registration", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, User.class)));

		return registrationResource.doPost(actionContext.getSecurityContext(), properties);
	}

	private Object handleResetPasswordAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final PrincipalInterface currentUser          = actionContext.getSecurityContext().getUser(false);
		final Map<String, Object> properties = new LinkedHashMap<>();

		removeInternalDataBindingKeys(parameters);

		for (final Entry<String, Object> entry : parameters.entrySet()) {

			final String key   = entry.getKey();
			final String value = (String) entry.getValue();

			if (value != null) properties.put(key, value);
		}

		final ResetPasswordResourceHandler resetPasswordResource = new ResetPasswordResourceHandler(new RESTCall("/reset-password", PropertyView.Public, true, AbstractDataServlet.getTypeOrDefault(currentUser, User.class)));

		return resetPasswordResource.doPost(actionContext.getSecurityContext(), properties);
	}

//	private void handleTreeAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext, final String action) throws FrameworkException {
//
//		final SecurityContext securityContext = actionContext.getSecurityContext();
//
//		if (parameters.containsKey(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET)) {
//
//			final String key = getTreeItemSessionIdentifier((String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET));
//
//			switch (action) {
//
//				case "open-tree-item":
//					setSessionAttribute(securityContext, key, true);
//					break;
//
//				case "close-tree-item":
//					removeSessionAttribute(securityContext, key);
//					break;
//
//				case "toggle-tree-item":
//
//					if (Boolean.TRUE.equals(getSessionAttribute(securityContext, key))) {
//
//						removeSessionAttribute(securityContext, key);
//
//					} else {
//
//						setSessionAttribute(securityContext, key, true);
//					}
//					break;
//			}
//
//
//		} else {
//
//			throw new FrameworkException(422, "Cannot execute update action without target UUID (data-structr-target attribute).");
//		}
//	}

	private GraphObject handleCreateAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();

		// create new object of type?
		final String targetType = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
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
		final String dataTarget               = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute update action without target UUID (data-structr-target attribute).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(actionContext, dataTarget)) {

			// convert input
			final PropertyMap properties = PropertyMap.inputTypeToJavaType(securityContext, target.getEntityType(), parameters);

			// update properties
			target.setProperties(securityContext, properties);

		}
	}

	private void handleDeleteAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final App app                         = StructrApp.getInstance(securityContext);
		final String dataTarget               = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute delete action without target UUID (data-structr-target attribute).");
		}

		for (final GraphObject target : resolveDataTargets(actionContext, dataTarget)) {

			if (target.isNode()) {

				app.delete((AbstractNode)target);

			} else {

				app.delete((AbstractRelationship)target);
			}
		}
	}

	private Object handleCustomAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext, final String methodName) throws FrameworkException {

		// Support old and new parameters
		final String idExpression  = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION);
		final String structrTarget = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		final String dataTarget    = structrTarget != null ? structrTarget : idExpression;

		// Empty dataTarget means no database object and no type, so it can only be a global (schema) method
		if (StringUtils.isNotBlank(methodName) && dataTarget == null) {

			removeInternalDataBindingKeys(parameters);

			return Actions.callWithSecurityContext(methodName, actionContext.getSecurityContext(), parameters);
		}

		if (Settings.isValidUuid(dataTarget)) {

			final List<GraphObject> targets = resolveDataTargets(actionContext, dataTarget);
			final Logger logger             = LoggerFactory.getLogger(getClass());

			if (targets.size() > 1) {
				logger.warn("Custom action has multiple targets, this is not supported yet. Returning only the result of the first target.");
			}

			removeInternalDataBindingKeys(parameters);

			for (final GraphObject target : targets) {

				final AbstractMethod method = Methods.resolveMethod(target.getClass(), methodName);
				if (method != null) {

					return method.execute(actionContext.getSecurityContext(), target, Arguments.fromMap(parameters), new EvaluationHints());

				} else {

					throw new FrameworkException(422, "Cannot execute method " + target.getClass().getSimpleName() + "." + methodName + ": method not found.");
				}
			}

		} else {

			// add support for static methods
			final Class staticClass = StructrApp.getConfiguration().getNodeEntityClass(dataTarget);
			if (staticClass != null) {

				final AbstractMethod method = Methods.resolveMethod(staticClass, methodName);
				if (method != null) {

					return method.execute(actionContext.getSecurityContext(), null, Arguments.fromMap(parameters), new EvaluationHints());

				} else {

					throw new FrameworkException(422, "Cannot execute static  method " + methodName + ": method not found.");
				}

			} else {

				throw new FrameworkException(422, "Cannot execute static method " + dataTarget + "." + methodName + ": type not found.");
			}
		}

		return null;
	}

	private Object handleAppendChildAction(final ActionContext actionContext, final java.util.Map<String, java.lang.Object> parameters, final EventContext eventContext) throws FrameworkException {

		final SecurityContext securityContext = actionContext.getSecurityContext();
		final String dataTarget               = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without target UUID (data-structr-target attribute).");
		}

		// fetch child ID
		final String childId = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without child UUID (data-child-id attribute).");
		}

		// load child node
		final DOMNode child = StructrApp.getInstance(securityContext).get(DOMNode.class, childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute append-child action without child (object with ID not found or not a DOMNode).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(actionContext, dataTarget)) {

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
		final String dataTarget               = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);

		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without target UUID (data-structr-target attribute).");
		}

		// fetch child ID
		final String childId = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without child UUID (data-child-id attribute).");
		}

		// load child node
		final DOMNode child = StructrApp.getInstance(securityContext).get(DOMNode.class, childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute remove-child action without child (object with ID not found or not a DOMNode).");
		}

		removeInternalDataBindingKeys(parameters);

		for (final GraphObject target : resolveDataTargets(actionContext, dataTarget)) {

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
		final String dataTarget               = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without target UUID (data-structr-target attribute).");
		}

		final String sourceObjectId = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT);
		if (sourceObjectId == null) {

			throw new FrameworkException(422, "Cannot execute insert-html action without html source object UUID (data-source-object).");
		}

		final String sourceProperty = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY);
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

		for (final GraphObject target : resolveDataTargets(actionContext, dataTarget)) {

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
		final String dataTarget               = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		if (dataTarget == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without target UUID (data-structr-target attribute).");
		}

		// fetch child ID
		final String childId = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		if (childId == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without child UUID (data-child-id attribute).");
		}

		// load child node
		final DOMNode child = StructrApp.getInstance(securityContext).get(DOMNode.class, childId);
		if (child == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without child (object with ID not found or not a DOMNode).");
		}

		final String sourceObjectId = (String) parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT);
		if (sourceObjectId == null) {

			throw new FrameworkException(422, "Cannot execute replace-html action without html source object UUID (data-source-object).");
		}

		final String sourceProperty = (String)parameters.get(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY);
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

		for (final GraphObject target : resolveDataTargets(actionContext, dataTarget)) {

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

		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRIDEXPRESSION);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRTARGET);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRDATATYPE);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_STRUCTRMETHOD);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_CHILDID);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEOBJECT);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_SOURCEPROPERTY);
		parameters.remove(DOMElement.EVENT_ACTION_MAPPING_PARAMETER_HTMLEVENT);
	}

	public String getOffsetAttributeName(final String name, final int offset) {

		int namePosition = -1;
		int index = 0;

		List<String> keys = Iterables.toList(getNode().getPropertyKeys());
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

	public void updateFromNode(final DOMNode newNode) throws FrameworkException {

		if (newNode instanceof DOMElement) {

			final PropertyMap properties = new PropertyMap();

			for (Property htmlProp : getHtmlAttributes()) {
				properties.put(htmlProp, newNode.getProperty(htmlProp));
			}

			// copy tag
			properties.put(DOMElement.tagProperty, newNode.getProperty(DOMElement.tagProperty));

			setProperties(getSecurityContext(), properties);
		}
	}

	public String getContextName() {

		final String _name = this.getProperty(DOMElement.name);
		if (_name != null) {

			return _name;
		}

		return getTag();
	}

	public void renderContent(final RenderContext renderContext, final int depth) throws FrameworkException {

		if (!shouldBeRendered(renderContext)) {
			return;
		}

		// final variables
		final SecurityContext securityContext = renderContext.getSecurityContext();
		final AsyncBuffer out                 = renderContext.getBuffer();
		final EditMode editMode               = renderContext.getEditMode(securityContext.getUser(false));
		final boolean hasSharedComponent      = getProperty(DOMElement.hasSharedComponent);
		final DOMElement synced               = hasSharedComponent ? (DOMElement)getSharedComponent() : null;
		final boolean isVoid                  = isVoidElement();
		final String _tag                     = getTag();

		// non-final variables
		boolean anyChildNodeCreatesNewLine = false;

		if (depth > 0 && !avoidWhitespace()) {

			out.append(DOMNode.indent(depth, renderContext));

		}

		if (StringUtils.isNotBlank(_tag)) {

			if (EditMode.DEPLOYMENT.equals(editMode)) {

				// Determine if this element's visibility flags differ from
				// the flags of the page and render a <!-- @structr:private -->
				// comment accordingly.
				if (renderDeploymentExportComments(out, false)) {

					// restore indentation
					if (depth > 0 && !avoidWhitespace()) {
						out.append(DOMNode.indent(depth, renderContext));
					}
				}
			}

			openingTag(out, _tag, editMode, renderContext, depth);

			try {

				// in body?
				if (lowercaseBodyName.equals(getTagName())) {
					renderContext.setInBody(true);
				}

				final String renderingMode = getProperty(DOMElement.renderingModeProperty);
				boolean lazyRendering      = false;

				// lazy rendering can only work if this node is not requested as a partial
				if (renderContext.getPage() != null && renderingMode != null) {
					lazyRendering = true;
				}

				// disable lazy rendering in deployment mode
				if (EditMode.DEPLOYMENT.equals(editMode)) {
					lazyRendering = false;
				}

				// only render children if we are not in a shared component scenario, not in deployment mode and it's not rendered lazily
				if (!lazyRendering && (synced == null || !EditMode.DEPLOYMENT.equals(editMode))) {

					// fetch children
					final List<DOMNodeCONTAINSDOMNode> rels = getChildRelationships();
					if (rels.isEmpty()) {

						// No child relationships, maybe this node is in sync with another node
						if (synced != null) {

							DOMNode.prefetchDOMNodes(synced.getUuid());

							rels.addAll(synced.getChildRelationships());
						}
					}

					// apply configuration for shared component if present
					final String _sharedComponentConfiguration = getProperty(DOMElement.sharedComponentConfigurationProperty);
					if (StringUtils.isNotBlank(_sharedComponentConfiguration)) {

						Scripting.evaluate(renderContext,  this, "${" + _sharedComponentConfiguration.trim() + "}", "sharedComponentConfiguration", getUuid());
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

				out.append("Error while rendering node ").append(getUuid()).append(": ").append(t.getMessage());

				final Logger logger = LoggerFactory.getLogger(DOMElement.class);
				logger.warn("", t);
			}

			// render end tag, if needed (= if not singleton tags)
			if (StringUtils.isNotBlank(_tag) && (!isVoid) || (isVoid && synced != null && EditMode.DEPLOYMENT.equals(editMode))) {

				// only insert a newline + indentation before the closing tag if any child-element used a newline
				final boolean isTemplate     = synced != null && EditMode.DEPLOYMENT.equals(editMode);

				if (anyChildNodeCreatesNewLine || isTemplate) {

					out.append(DOMNode.indent(depth, renderContext));
				}

				if (synced != null && EditMode.DEPLOYMENT.equals(editMode)) {

					out.append("</structr:component>");

				} else if (isTemplate) {

					out.append("</structr:template>");

				} else {

					out.append("</").append(_tag).append(">");
				}
			}
		}
	}

	public void openingTag(final AsyncBuffer out, final String tag, final EditMode editMode, final RenderContext renderContext, final int depth) throws FrameworkException {

		final boolean hasSharedComponent         = getProperty(DOMElement.hasSharedComponent);
		final DOMElement _sharedComponentElement = hasSharedComponent ? (DOMElement) getSharedComponent() : null;

		if (_sharedComponentElement != null && EditMode.DEPLOYMENT.equals(editMode)) {

			out.append("<structr:component src=\"");

			final String _name = _sharedComponentElement.getProperty(AbstractNode.name);
			out.append(_name != null ? _name.concat("-").concat(_sharedComponentElement.getUuid()) : _sharedComponentElement.getUuid());

			out.append("\"");

			renderSharedComponentConfiguration(out, editMode);

			// include data-* attributes in template
			renderCustomAttributes(out, renderContext.getSecurityContext(), renderContext);

		} else {

			out.append("<").append(tag);

			final ConfigurationProvider config = StructrApp.getConfiguration();
			final Class type  = getEntityType();
			final String uuid = getUuid();

			final List<PropertyKey> htmlAttributes = new ArrayList<>();


			getNode().getPropertyKeys().forEach((key) -> {
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

					value = (String)getProperty(attribute);

				} else {

					value = getPropertyWithVariableReplacement(renderContext, attribute);
				}

				if (!(EditMode.RAW.equals(editMode) || EditMode.WIDGET.equals(editMode))) {

					value = DOMNode.escapeForHtmlAttributes(value);
				}

				if (value != null) {

					String key = attribute.jsonName().substring(PropertyView.Html.length());

					out.append(" ").append(key).append("=\"").append(value).append("\"");

				}
			}

			// make repeater data object ID available
			final GraphObject repeaterDataObject = renderContext.getDataObject();
			if (repeaterDataObject != null && StringUtils.isNotBlank(getDataKey())) {

				out.append(" data-repeater-data-object-id=\"").append(repeaterDataObject.getUuid()).append("\"");
			}

			// include arbitrary data-* attributes
			renderSharedComponentConfiguration(out, editMode);
			renderCustomAttributes(out, renderContext.getSecurityContext(), renderContext);

			// new: managed attributes (like selected
			renderManagedAttributes(out, renderContext.getSecurityContext(), renderContext);

			// include special mode attributes
			switch (editMode) {

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

					out.append(" ").append("data-structr-hash").append("=\"").append(getIdHash()).append("\"");
					break;

				case WIDGET:
				case DEPLOYMENT:

					final String eventMapping = getEventMapping();
					if (eventMapping != null) {

						out.append(" ").append("data-structr-meta-event-mapping").append("=\"").append(StringEscapeUtils.escapeHtml4(eventMapping)).append("\"");
					}
					break;

				case NONE:

					// Get actions in superuser context
					final DOMElement thisElementWithSuperuserContext = StructrApp.getInstance().get(DOMElement.class, uuid);
					final Iterable<ActionMapping> triggeredActions   = thisElementWithSuperuserContext.getProperty(DOMElement.triggeredActionsProperty);
					final List<ActionMapping> list                   = Iterables.toList(triggeredActions);
					boolean outputStructrId                          = false;

					if (!list.isEmpty()) {

						// all active elements need data-structr-id
						outputStructrId = true;

						// why only the first one?!
						final ActionMapping triggeredAction = list.get(0);
						final String options                = triggeredAction.getProperty(ActionMapping.optionsProperty);

						// support for configuration options
						if (StringUtils.isNotBlank(options)) {
							out.append(" data-structr-options=\"").append(uuid).append("\"");
						}

						String eventsString = null;
						final Map<String, Object> mapping = getMappedEvents();
						if (mapping != null) {
							eventsString = StringUtils.join(mapping.keySet(), ",");
						}

						// append all stored action mapping keys as data-structr-<key> attributes
						for (final Property<String> key : Set.of(ActionMapping.eventProperty, ActionMapping.actionProperty, ActionMapping.methodProperty, ActionMapping.dataTypeProperty, ActionMapping.idExpressionProperty)) {

							final String value = triggeredAction.getPropertyWithVariableReplacement(renderContext, key);
							if (StringUtils.isNotBlank(value)) {

								final String keyHyphenated = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, key.jsonName());
								out.append(" data-structr-" + keyHyphenated + "=\"").append(value).append("\"");
							}

							if (key.equals(ActionMapping.eventProperty)) {

								eventsString = (String) value;
							}
						}

						if (eventsString != null) {
							out.append(" data-structr-events=\"").append(eventsString).append("\"");
						}

						renderDialogAttributes(renderContext, out, triggeredAction);
						renderSuccessNotificationAttributes(renderContext, out, triggeredAction);
						renderFailureNotificationAttributes(renderContext, out, triggeredAction);
						renderSuccessBehaviourAttributes(renderContext, out, triggeredAction);
						renderFailureBehaviourAttributes(renderContext, out, triggeredAction);


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


						// **************************************************************************+
						// parameters
						// **************************************************************************+

						// TODO: Add support for multiple triggered actions.
						//  At the moment, backend and frontend code only support one triggered action,
						// even though the data model has a ManyToMany rel between triggerElements and triggeredActions
						for (final ParameterMapping parameterMapping : triggeredAction.getProperty(ActionMapping.parameterMappings)) {

							final String parameterType = parameterMapping.getProperty(StructrApp.key(ParameterMapping.class, "parameterType"));
							final String parameterName = parameterMapping.getPropertyWithVariableReplacement(renderContext, StructrApp.key(ParameterMapping.class, "parameterName"));

							if (parameterType == null || parameterName == null) {
								// Ignore incomplete parameter mapping
								continue;
							}

							final String nameAttributeHyphenated = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, parameterName);

							switch (parameterType) {

								case "user-input":
									final DOMElement element   = parameterMapping.getProperty(ParameterMapping.inputElement);

									if (element != null) {

										final String elementCssId = element.getPropertyWithVariableReplacement(renderContext, DOMElement.htmlIdProperty);

										if (elementCssId != null) {

											out.append(" data-").append(nameAttributeHyphenated).append("=\"css(#").append(elementCssId).append(")\"");

										} else {

											out.append(" data-").append(nameAttributeHyphenated).append("=\"id(").append(element.getUuid()).append(")\"");
										}

									}
									break;

								case "constant-value":
									final String constantValue = parameterMapping.getProperty(ParameterMapping.constantValue);
									// Could be 'json(...)' or a simple value
									out.append(" data-").append(nameAttributeHyphenated).append("=\"").append(DOMNode.escapeForHtmlAttributes(constantValue)).append("\"");
									break;

								case "script-expression":
									final String scriptExpression = parameterMapping.getPropertyWithVariableReplacement(renderContext, ParameterMapping.scriptExpression);
									out.append(" data-").append(nameAttributeHyphenated).append("=\"").append(DOMNode.escapeForHtmlAttributes(scriptExpression)).append("\"");
									break;

								case "page-param":
									// Name of the request parameter for pager 'page'
									final String action = triggeredAction.getProperty(ActionMapping.actionProperty);
									final String value  = renderContext.getRequestParameter(parameterName);
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

					if (isTargetElement(thisElementWithSuperuserContext)) {

						outputStructrId = true;

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

					if (getProperty(DOMElement.renderingModeProperty) != null) {

						out.append(" data-structr-delay-or-interval=\"").append(getProperty(DOMElement.delayOrIntervalProperty)).append("\"");

						outputStructrId = true;
					}

					if (renderContext.isTemplateRoot(uuid)) {

						// render template ID into output so it can be re-used
						out.append(" data-structr-template-id=\"").append(renderContext.getTemplateId()).append("\"");

					}

					final Iterable<? extends ParameterMapping> parameterMappings = thisElementWithSuperuserContext.getProperty(DOMElement.parameterMappingsProperty);

					outputStructrId |= (thisElementWithSuperuserContext instanceof TemplateElement || parameterMappings.iterator().hasNext());

					// output data-structr-id only once
					if (outputStructrId) {
						out.append(" data-structr-id=\"").append(uuid).append("\"");
					}

					break;
	 		}
		}

		out.append(">");
	}

	public static void renderDialogAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) throws FrameworkException {

		final String dialogType = triggeredAction.getProperty(ActionMapping.dialogTypeProperty);
		if (dialogType != null && !dialogType.equals("none")) {

			final String dialogTitle = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.dialogTitleProperty);
			final String dialogText = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.dialogTextProperty);

			out.append(" data-structr-dialog-type=\"").append(dialogType).append("\"");
			out.append(" data-structr-dialog-title=\"").append(DOMNode.escapeForHtmlAttributes(dialogTitle)).append("\"");
			out.append(" data-structr-dialog-text=\"").append(DOMNode.escapeForHtmlAttributes(dialogText)).append("\"");

		}
	}

	public void renderSuccessNotificationAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) {

		// Possible values for success notifications are none, system-alert, inline-text-message, custom-dialog-element, fire-event
		final String successNotifications = triggeredAction.getProperty(ActionMapping.successNotificationsProperty);
		if (StringUtils.isNotBlank(successNotifications)) {

			out.append(" data-structr-success-notifications=\"").append(successNotifications).append("\"");

			switch (successNotifications) {

				case "custom-dialog-linked":
					out.append(" data-structr-success-notifications-custom-dialog-element=\"").append(generateDataAttributesForIdList(renderContext, triggeredAction, ActionMapping.successNotificationElements)).append("\"");
					break;

				case "fire-event":
					out.append(" data-structr-success-notifications-event=\"").append(triggeredAction.getProperty(ActionMapping.successNotificationsEventProperty)).append("\"");
					break;

				case "inline-text-message":
					final Integer delay = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "successNotificationsDelay"));
					out.append(" data-structr-success-notifications-delay=\"").append(delay.toString()).append("\"");
					break;

				default:
					break;

			}
		}

		final String successNotificationsPartial = triggeredAction.getProperty(ActionMapping.successNotificationsPartialProperty);
		if (StringUtils.isNotBlank(successNotificationsPartial)) {

			out.append(" data-structr-success-notifications-partial=\"").append(successNotificationsPartial).append("\"");
		}
	}

	public void renderFailureNotificationAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) {

		// Possible values for failure notifications are none, system-alert, inline-text-message, custom-dialog-element, fire-event
		final String failureNotifications = triggeredAction.getProperty(ActionMapping.failureNotificationsProperty);
		if (StringUtils.isNotBlank(failureNotifications)) {

			out.append(" data-structr-failure-notifications=\"").append(failureNotifications).append("\"");
		}

		if (StringUtils.isNotBlank(failureNotifications)) {

			switch (failureNotifications) {

				case "custom-dialog-linked":
					out.append(" data-structr-failure-notifications-custom-dialog-element=\"").append(generateDataAttributesForIdList(renderContext, triggeredAction, ActionMapping.failureNotificationElements)).append("\"");
					break;

				case "fire-event":
					out.append(" data-structr-failure-notifications-event=\"").append(triggeredAction.getProperty(ActionMapping.failureNotificationsEventProperty)).append("\"");
					break;

				case "inline-text-message":
					final Integer delay = triggeredAction.getProperty(StructrApp.key(ActionMapping.class, "failureNotificationsDelay"));
					out.append(" data-structr-failure-notifications-delay=\"").append(delay.toString()).append("\"");
					break;

				default:
					break;

			}
		}

		final String failureNotificationsPartial = triggeredAction.getProperty(ActionMapping.failureNotificationsPartialProperty);
		if (StringUtils.isNotBlank(failureNotificationsPartial)) {

			out.append(" data-structr-failure-notifications-partial=\"").append(failureNotificationsPartial).append("\"");
		}
	}

	public void renderSuccessBehaviourAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) throws FrameworkException {

		// Possible values for the success behaviour are nothing, full-page-reload, partial-refresh, navigate-to-url, fire-event
		final String successBehaviour = triggeredAction.getProperty(ActionMapping.successBehaviourProperty);
		final String successPartial   = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.successPartialProperty);
		final String successURL       = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.successURLProperty);
		final String successEvent     = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.successEventProperty);

		String successTargetString = null;

		if (StringUtils.isNotBlank(successBehaviour)) {

			switch (successBehaviour) {
				case "partial-refresh":
					successTargetString = successPartial;
					break;
				case "partial-refresh-linked":
					successTargetString = generateDataAttributesForIdList(renderContext, triggeredAction, ActionMapping.successTargets);
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

		final String idExpression = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.idExpressionProperty);
		if (StringUtils.isNotBlank(idExpression)) {
			out.append(" data-structr-target=\"").append(idExpression).append("\"");
		}

		final String action = triggeredAction.getProperty(ActionMapping.actionProperty);
		if ("create".equals(action)) {

			final String dataType = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.dataTypeProperty);
			if (StringUtils.isNotBlank(dataType)) {
				out.append(" data-structr-target=\"").append(dataType).append("\"");
			}
		}

		if (StringUtils.isNotBlank(successTargetString)) {
			out.append(" data-structr-success-target=\"").append(successTargetString).append("\"");
		}
	}

	public void renderFailureBehaviourAttributes(final RenderContext renderContext, final AsyncBuffer out, final ActionMapping triggeredAction) throws FrameworkException {

		// Possible values for the failure behaviour are nothing, full-page-reload, partial-refresh, navigate-to-url, fire-event
		final String failureBehaviour = triggeredAction.getProperty(ActionMapping.failureBehaviourProperty);
		final String failurePartial   = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.failurePartialProperty);
		final String failureURL       = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.failureURLProperty);
		final String failureEvent     = triggeredAction.getPropertyWithVariableReplacement(renderContext, ActionMapping.failureEventProperty);

		String failureTargetString = null;

		if (StringUtils.isNotBlank(failureBehaviour)) {

			switch (failureBehaviour) {
				case "partial-refresh":
					failureTargetString = failurePartial;
					break;
				case "partial-refresh-linked":
					failureTargetString = generateDataAttributesForIdList(renderContext, triggeredAction, ActionMapping.failureTargets);
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
	}

	public Node doImport(final Page newPage) throws DOMException {

		DOMElement newElement = (DOMElement) newPage.createElement(getTag());

		// copy attributes
		for (String _name : getHtmlAttributeNames()) {

			Attr attr = getAttributeNode(_name);
			if (attr.getSpecified()) {

				newElement.setAttribute(attr.getName(), attr.getValue());
			}
		}

		return newElement;
	}

	public List<String> getHtmlAttributeNames() {

		List<String> names = new ArrayList<>(10);

		for (String key : getNode().getPropertyKeys()) {

			// use html properties only
			if (key.startsWith(PropertyView.Html)) {

				names.add(key.substring(HtmlPrefixLength));
			}
		}

		return names;
	}

	public void setIdAttribute(final String idString, boolean isId) throws DOMException {

		checkWriteAccess();

		try {
			setProperty(DOMElement.htmlIdProperty, idString);

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.toString());
		}
	}

	public String getAttribute(final String name) {

		HtmlProperty htmlProperty = findOrCreateAttributeKey(name);

		return htmlProperty.getProperty(getSecurityContext(), this, true);
	}

	public void setAttribute(final String name, final String value) throws DOMException {

		try {
			HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(getSecurityContext(), this, value);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	public void removeAttribute(final String name) throws DOMException {

		try {
			HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
			if (htmlProperty != null) {

				htmlProperty.setProperty(getSecurityContext(), this, null);
			}

		} catch (FrameworkException fex) {

			throw new DOMException(DOMException.INVALID_STATE_ERR, fex.getMessage());

		}
	}

	public Attr getAttributeNode(final String name) {

		HtmlProperty htmlProperty = findOrCreateAttributeKey(name);
		final String value        = htmlProperty.getProperty(getSecurityContext(), this, true);

		if (value != null) {

			boolean explicitlySpecified = true;
			boolean isId = false;

			if (value.equals(htmlProperty.defaultValue())) {
				explicitlySpecified = false;
			}

			return new DOMAttribute(getOwnerDocument(), this, name, value, explicitlySpecified, isId);
		}

		return null;
	}

	public Attr setAttributeNode(final Attr attr) throws DOMException {

		// save existing attribute node
		final Attr attribute = getAttributeNode(attr.getName());

		// set value
		setAttribute(attr.getName(), attr.getValue());

		// set parent of attribute node
		if (attr instanceof DOMAttribute) {
			((DOMAttribute) attr).setParent(this);
		}

		return attribute;
	}

	public Attr removeAttributeNode(final Attr attr) throws DOMException {

		// save existing attribute node
		final Attr attribute = getAttributeNode(attr.getName());

		// set value
		setAttribute(attr.getName(), null);

		return attribute;
	}

	public NodeList getElementsByTagName(final String tagName) {

		DOMNodeList results = new DOMNodeList();

		DOMNode.collectNodesByPredicate(getSecurityContext(), this, results, new TagPredicate(tagName), 0, false);

		return results;
	}

	public HtmlProperty findOrCreateAttributeKey(final String name) {

		// try to find native html property defined in DOMElement or one of its subclasses
		final PropertyKey key = StructrApp.getConfiguration().getPropertyKeyForJSONName(getEntityType(), name, false);

		if (key != null && key instanceof HtmlProperty) {

			return (HtmlProperty) key;

		} else {

			// create synthetic HtmlProperty
			final HtmlProperty htmlProperty = new HtmlProperty(name);
			htmlProperty.setDeclaringClass(DOMElement.class);

			return htmlProperty;
		}
	}

	public Node setNamedItem(final Node node) throws DOMException {

		if (node instanceof Attr) {
			return setAttributeNode((Attr) node);
		}

		return null;
	}

	public Node removeNamedItem(final String name) throws DOMException {

		// save existing attribute node
		Attr attribute = getAttributeNode(name);

		// set value to null
		setAttribute(name, null);

		return attribute;
	}

	public Node item(final int i) {

		List<String> htmlAttributeNames = getHtmlAttributeNames();
		if (i >= 0 && i < htmlAttributeNames.size()) {

			return getAttributeNode(htmlAttributeNames.get(i));
		}

		return null;
	}

	public List<GraphObject> resolveDataTargets(final ActionContext actionContext, final String dataTarget) throws FrameworkException {

		final App app                   = StructrApp.getInstance(getSecurityContext());
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
			final Object result = evaluate(actionContext, dataTarget, null, new EvaluationHints(), 1, 1);
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

	public void updateReloadTargets() throws FrameworkException {

		try {

			final List<DOMElement> actualReloadSources = new LinkedList<>();
			final List<DOMElement> actualReloadTargets = new LinkedList<>();
			final org.jsoup.nodes.Element matchElement = getMatchElement();
			final String reloadTargets                 = getProperty(DOMElement.dataReloadTargetProperty);
			final Page page                            = getOwnerDocument();

			if (page != null) {

				for (final DOMNode possibleReloadTargetNode : page.getElements()) {

					if (possibleReloadTargetNode instanceof DOMElement) {

						final DOMElement possibleTarget       = (DOMElement)possibleReloadTargetNode;
						final org.jsoup.nodes.Element element = possibleTarget.getMatchElement();
						final String otherReloadTarget        = possibleTarget.getProperty(DOMElement.dataReloadTargetProperty);

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
			setProperty(DOMElement.reloadSourcesProperty, actualReloadSources);
			setProperty(DOMElement.reloadTargetsProperty, actualReloadTargets);

			// update shared component sync flag
			setProperty(DOMElement.hasSharedComponent, getSharedComponent() != null);

		} catch (Throwable t) {

			t.printStackTrace();
		}
	}

	public org.jsoup.nodes.Element getMatchElement() {

		final String tag = getTag();

		if (StringUtils.isNotBlank(tag)) {

			final org.jsoup.nodes.Element element = new org.jsoup.nodes.Element(tag);
			final String classes                  = getProperty(DOMElement.htmlClassProperty);

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

			final String htmlId = getProperty(DOMElement.htmlIdProperty);
			if (htmlId != null) {

				element.attr("id", htmlId);
			}

			return element;
		}

		return null;
	}

	public boolean isTargetElement(final DOMElement thisElement) {

		final boolean isManualReloadTarget                   = thisElement.getProperty(DOMElement.manualReloadTargetProperty);
		final List<DOMElement> reloadSources                 = Iterables.toList(thisElement.getProperty(DOMElement.reloadSourcesProperty));
		final List<ActionMapping> reloadingActions           = Iterables.toList(thisElement.getProperty(DOMElement.reloadingActionsProperty));
		final List<ActionMapping> failureActions             = Iterables.toList(thisElement.getProperty(DOMElement.failureActionsProperty));
		final List<ActionMapping> successNotificationActions = Iterables.toList(thisElement.getProperty(DOMElement.successNotificationActionsProperty));
		final List<ActionMapping> failureNotificationActions = Iterables.toList(thisElement.getProperty(DOMElement.failureNotificationActionsProperty));

		return isManualReloadTarget || !reloadSources.isEmpty() || !reloadingActions.isEmpty() || !failureActions.isEmpty() || !successNotificationActions.isEmpty() || !failureNotificationActions.isEmpty();
	}

	// ----- org.w3c.Node methods -----
	@Override
	public String getLocalName() {
		return null;
	}

	@Override
	public NamedNodeMap getAttributes() {
		return this;
	}

	@Override
	public boolean contentEquals(final Node node) {
		return false;
	}

	@Override
	public boolean hasAttributes() {
		return getLength() > 0;
	}

	@Override
	public int getLength() {
		return getHtmlAttributeNames().size();
	}

	@Override
	public String getTagName() {
		return getTag();
	}

	@Override
	public String getNodeName() {
		return getTagName();
	}

	@Override
	public void setNodeValue(final String value) {
	}

	@Override
	public String getNodeValue() {
		return null;
	}

	@Override
	public short getNodeType() {
		return ELEMENT_NODE;
	}

	// ----- org.w3c.dom.Element -----
	@Override
	public void setIdAttributeNS(final String namespaceURI, final String localName, final boolean isId) throws DOMException {
		throw new UnsupportedOperationException("Namespaces not supported.");
	}

	@Override
	public void setIdAttributeNode(final Attr idAttr, final boolean isId) throws DOMException {
		throw new UnsupportedOperationException("Attribute nodes not supported in HTML5.");
	}

	@Override
	public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
		return null;
	}

	@Override
	public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
	}

	@Override
	public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
	}

	@Override
	public Attr getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
		return null;
	}

	@Override
	public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
		return null;
	}

	@Override
	public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
		return null;
	}

	@Override
	public boolean hasAttribute(String name) {
		return getAttribute(name) != null;
	}

	@Override
	public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
		return false;
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		return null;
	}

	// ----- org.w3c.dom.NamedNodeMap -----
	@Override
	public Node getNamedItem(String name) {
		return getAttributeNode(name);
	}

	@Override
	public Node getNamedItemNS(String namespaceURI, String localName) throws DOMException {
		return null;
	}

	@Override
	public Node setNamedItemNS(Node arg) throws DOMException {
		return null;
	}

	@Override
	public Node removeNamedItemNS(String namespaceURI, String localName) throws DOMException {
		return null;
	}

	// ----- private static methods -----
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

	public class TagPredicate implements Predicate<Node> {

		private String tagName = null;

		public TagPredicate(String tagName) {
			this.tagName = tagName;
		}

		@Override
		public boolean accept(Node obj) {

			if (obj instanceof DOMElement) {

				DOMElement elem = (DOMElement)obj;

				if (tagName.equals(elem.getTag())) {
					return true;
				}
			}

			return false;
		}
	}

	private String generateDataAttributesForIdList(final RenderContext renderContext, final ActionMapping triggeredAction, final Property<Iterable<DOMNode>> key) {

		final List<String> selectors = new LinkedList<>();

		for (final DOMNode node : triggeredAction.getProperty(key)) {

			// Create CSS selector for data-structr-id
			String selector = "[data-structr-id='" + node.getUuid() + "']";

			final String dataKey = node.getDataKey();
			if (dataKey != null) {

				selector += "[data-repeater-data-object-id='" + renderContext.getDataNode(dataKey).getUuid() + "']";
			}

			selectors.add(selector);
		}

		return StringUtils.join(selectors, ",");
	}
}