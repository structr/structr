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
package org.structr.web.resource;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.MailHelper;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeFactory;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.GraphObjectTraitDefinition;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.core.traits.definitions.PersonTraitDefinition;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;
import org.structr.core.traits.definitions.UserTraitDefinition;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.servlet.AbstractDataServlet;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.User;
import org.structr.web.servlet.HtmlServlet;

import java.util.*;

public class RegistrationResourceHandler extends RESTCallHandler {

	private static final Logger logger = LoggerFactory.getLogger(RegistrationResourceHandler.class.getName());

	private enum TemplateKey {
		CONFIRM_REGISTRATION_SENDER_NAME,
		CONFIRM_REGISTRATION_SENDER_ADDRESS,
		CONFIRM_REGISTRATION_SUBJECT,
		CONFIRM_REGISTRATION_TEXT_BODY,
		CONFIRM_REGISTRATION_HTML_BODY,
		CONFIRM_REGISTRATION_BASE_URL,
		CONFIRM_REGISTRATION_SUCCESS_PATH,
		CONFIRM_REGISTRATION_FAILURE_PATH,
		CONFIRM_REGISTRATION_PAGE,
		CONFIRM_REGISTRATION_CONFIRMATION_KEY_KEY,
		CONFIRM_REGISTRATION_TARGET_PATH_KEY,
		CONFIRM_REGISTRATION_ERROR_PAGE_KEY
	}

	public RegistrationResourceHandler(final RESTCall call) {
		super(call);
	}

	@Override
	public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

		boolean existingUser                  = false;
		RestMethodResult returnedMethodResult = null;

		if (propertySet.containsKey(PrincipalTraitDefinition.EMAIL_PROPERTY)) {

			final PropertyKey<String> confKeyKey  = Traits.of(StructrTraits.USER).key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY);
			final PropertyKey<String> eMailKey    = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);
			final PropertyKey<String> passwordKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.PASSWORD_PROPERTY);
			final String emailString              = (String) propertySet.get(eMailKey.jsonName());
			final String passwordString           = (String) propertySet.get(passwordKey.jsonName());

			if (StringUtils.isEmpty(emailString)) {
				throw new FrameworkException(422, "No e-mail address given.");
			}

			final String localeString = (String) propertySet.get("locale");
			final String confKey      = AuthHelper.getConfirmationKey();

			final SecurityContext ctx = SecurityContext.getSuperUserInstance();
			final App app             = StructrApp.getInstance(ctx);

			if (Settings.CallbacksOnLogin.getValue() == false) {
				ctx.disableInnerCallbacks();
			}

			Principal user = null;

			try (final Tx tx = app.tx(true, true, true)) {

				final NodeInterface userNode = app.nodeQuery(StructrTraits.USER).and(eMailKey, emailString).getFirst();
				if (userNode != null) {

					// For existing users, update confirmation key
					user.setProperty(confKeyKey, confKey);

					existingUser = true;

				} else {

					final Authenticator auth = securityContext.getAuthenticator();
					user = createUser(ctx, eMailKey, emailString, propertySet, Settings.RestUserAutocreate.getValue(), auth.getUserClass(), confKey);
					if (user != null) user.setPassword(passwordString);
				}

				tx.success();

			} catch (final FrameworkException ex) {

				logger.info("Unable to create user with e-mail {}: {}", emailString, ex.getMessage());
				returnedMethodResult = new RestMethodResult(422, ex.getMessage());
				returnedMethodResult.addHeader("reason", ex.getMessage());

				return returnedMethodResult;
			}

			if (user != null) {

				try (final Tx tx = app.tx(true, true, true)) {

					sendInvitationLink(securityContext, user, propertySet, confKey, localeString);

					tx.success();

				} catch (final FrameworkException | EmailException ex) {

					logger.info("Unable to create user with e-mail {}", emailString, ex);
					returnedMethodResult = new RestMethodResult(422, ex.getMessage());

					return returnedMethodResult;
				}

				// If we have just updated the confirmation key for an existing user,
				// return 200 to distinguish from new users
				if (existingUser) {

					// return 200 OK
					return new RestMethodResult(HttpServletResponse.SC_OK);

				} else {

					// return 201 Created
					return new RestMethodResult(HttpServletResponse.SC_CREATED);
				}

			} else {

				logger.info("Unable to create user with e-mail {}", emailString);
				returnedMethodResult = new RestMethodResult(422);

				return returnedMethodResult;
			}

		} else {

			logger.info("No e-mail address given.");
			returnedMethodResult = new RestMethodResult(422, "No e-mail address given.");

			return returnedMethodResult;
		}
	}

	private void sendInvitationLink(final SecurityContext securityContext, final Principal user, final Map<String, Object> propertySetFromUserPOST, final String confKey, final String localeString) throws FrameworkException, EmailException {

		final PropertyKey<String> eMailKey = Traits.of(StructrTraits.USER).key(PrincipalTraitDefinition.EMAIL_PROPERTY);
		final String userEmail             = user.getProperty(eMailKey);
		final ActionContext ctx            = new ActionContext(SecurityContext.getInstance(user, AccessMode.Frontend));

		// Populate the replacement map with all POSTed values
		// WARNING! This is unchecked user input!!
		propertySetFromUserPOST.entrySet().forEach(entry -> ctx.setConstant(entry.getKey(), entry.getValue().toString()));

		String successPath = getTemplateText(TemplateKey.CONFIRM_REGISTRATION_SUCCESS_PATH, AbstractDataServlet.prefixLocation("register_thanks"), localeString);
		String failurePath  = getTemplateText(TemplateKey.CONFIRM_REGISTRATION_FAILURE_PATH, AbstractDataServlet.prefixLocation("register_error"), localeString);

//		// Allow overriding success path from frontend action
//		if (propertySetFromUserPOST.containsKey(DOMElement.DATA_BINDING_PARAMETER_SUCCESS_TARGET)) {
//			successPath = getTemplateText(TemplateKey.CONFIRM_REGISTRATION_SUCCESS_PATH, (String) propertySetFromUserPOST.get(DOMElement.DATA_BINDING_PARAMETER_SUCCESS_TARGET), localeString);
//		}
//
//		// Allow overriding failure path from frontend action
//		if (propertySetFromUserPOST.containsKey(DOMElement.DATA_BINDING_PARAMETER_FAILURE_TARGET)) {
//			failurePath = getTemplateText(TemplateKey.CONFIRM_REGISTRATION_FAILURE_PATH, (String) propertySetFromUserPOST.get(DOMElement.DATA_BINDING_PARAMETER_FAILURE_TARGET), localeString);
//		}

		ctx.setConstant(PrincipalTraitDefinition.EMAIL_PROPERTY, userEmail);
		ctx.setConstant("link",
				getTemplateText(TemplateKey.CONFIRM_REGISTRATION_BASE_URL, ActionContext.getBaseUrl(securityContext.getRequest()), localeString)
				+ getTemplateText(TemplateKey.CONFIRM_REGISTRATION_PAGE, HtmlServlet.CONFIRM_REGISTRATION_PAGE, localeString)
				+ "?" + getTemplateText(TemplateKey.CONFIRM_REGISTRATION_CONFIRMATION_KEY_KEY, HtmlServlet.CONFIRMATION_KEY_KEY, localeString) + "=" + confKey
				+ "&" + getTemplateText(TemplateKey.CONFIRM_REGISTRATION_TARGET_PATH_KEY, HtmlServlet.TARGET_PATH_KEY, localeString)           + "=" + successPath
				+ "&" + getTemplateText(TemplateKey.CONFIRM_REGISTRATION_ERROR_PAGE_KEY, HtmlServlet.ERROR_PAGE_KEY, localeString)             + "=" + failurePath
		);

		final String textMailContent = replaceVariablesInTemplate(TemplateKey.CONFIRM_REGISTRATION_TEXT_BODY,"Go to ${link} to finalize registration.", localeString, ctx);
		final String htmlMailContent = replaceVariablesInTemplate(TemplateKey.CONFIRM_REGISTRATION_HTML_BODY,"<div>Click <a href='${link}'>here</a> to finalize registration.</div>", localeString, ctx);

		MailHelper.sendHtmlMail(
			getTemplateText(TemplateKey.CONFIRM_REGISTRATION_SENDER_ADDRESS, "structr-mail-daemon@localhost", localeString),
			getTemplateText(TemplateKey.CONFIRM_REGISTRATION_SENDER_NAME, "Structr Mail Daemon", localeString),
			userEmail, "", null, null, null,
			getTemplateText(TemplateKey.CONFIRM_REGISTRATION_SUBJECT, "Welcome to Structr, please finalize registration", localeString),
			htmlMailContent, textMailContent);

	}

	private String getTemplateText(final TemplateKey key, final String defaultValue, final String localeString) {

		try {

			final Query<NodeInterface> query = StructrApp.getInstance().nodeQuery(StructrTraits.MAIL_TEMPLATE).andName(key.name());

			if (localeString != null) {
				query.and(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.LOCALE_PROPERTY), localeString);
			}

			NodeInterface template = query.getFirst();
			if (template != null) {

				final String text = template.as(MailTemplate.class).getText();
				return text != null ? text : defaultValue;

			} else {

				return defaultValue;
			}

		} catch (FrameworkException ex) {

			logger.warn("Could not get mail template for key " + key, ex);
		}

		return null;
	}

	private String replaceVariablesInTemplate(final TemplateKey key, final String defaultValue, final String localeString, final ActionContext ctx) throws FrameworkException {

		try {

			final String templateText = getTemplateText(key, defaultValue, localeString);

			return Scripting.replaceVariables(ctx, null, templateText, "sendInvitationLink()");

		} catch (FrameworkException fxe) {

			logger.warn("Unable to send confirmation e-mail, scripting error in {} template. {}", key.name(), fxe);

			throw new FrameworkException(503, "Unable to send confirmation e-mail");
		}
	}

	/**
	 * Create a new user.
	 *
	 * If a {@link PersonTraitDefinition} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param confKey
	 * @return user
	 */
	public Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final String confKey) throws FrameworkException {

		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, confKey);
	}

	/**
	 * Create a new user.
	 *
	 * If a {@link PersonTraitDefinition} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param propertySet
	 * @param confKey
	 * @return user
	 */
	public Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final String confKey) throws FrameworkException {

		return createUser(securityContext, credentialKey, credentialValue, propertySet, false, confKey);
	}

	/**
	 * Create a new user.
	 *
	 * If a {@link PersonTraitDefinition} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param autoCreate
	 * @param confKey
	 * @return user
	 */
	public Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final boolean autoCreate, final String confKey) throws FrameworkException {

		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, autoCreate, confKey);
	}

	/**
	 * Create a new user.
	 *
	 * If a {@link PersonTraitDefinition} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param autoCreate
	 * @param userType
	 * @param confKey
	 * @return user
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final boolean autoCreate, final String userType, final String confKey) throws FrameworkException {

		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, autoCreate, userType, confKey);
	}

	/**
	 * Create a new user.
	 *
	 * If a {@link PersonTraitDefinition} is found, convert that object to a {@link User} object.
	 * If autoCreate is true, auto-create a new user, even if no matching person is found.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param propertySet
	 * @param autoCreate
	 * @param confKey
	 * @return user
	 */
	public Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final boolean autoCreate, final String confKey) throws FrameworkException {

		return createUser(securityContext, credentialKey, credentialValue, propertySet, autoCreate, StructrTraits.USER, confKey);
	}

	/**
	 * Create a new user.
	 *
	 * If a {@link Principal} is found, convert that object to a {@link Principal} object.
	 * If autoCreate is true, auto-create a new user, even if no matching person is found.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param propertySet
	 * @param autoCreate
	 * @param confKey
	 * @return user
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final boolean autoCreate, final String userClass, final String confKey) throws FrameworkException {

		final PropertyKey<String> confirmationKeyKey = Traits.of(StructrTraits.USER).key(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY);
		Principal user = null;

		// First, search for a person with that e-mail address
		user = AuthHelper.getPrincipalForCredential(credentialKey, credentialValue);

		if (user != null) {

			user = new NodeFactory(securityContext).instantiate(user.getNode()).as(Principal.class);

			// convert to user
			user.unlockSystemPropertiesOnce();

			final PropertyMap changedProperties = new PropertyMap();
			changedProperties.put(Traits.of(StructrTraits.GRAPH_OBJECT).key(GraphObjectTraitDefinition.TYPE_PROPERTY), StructrTraits.USER);
			changedProperties.put(confirmationKeyKey, confKey);
			user.setProperties(securityContext, changedProperties);

		} else if (autoCreate == true) {

			final App app = StructrApp.getInstance(securityContext);

			// Clear properties set by us from the user-defined props
			propertySet.remove(credentialKey.jsonName());
			propertySet.remove(UserTraitDefinition.CONFIRMATION_KEY_PROPERTY);

			PropertyMap props = PropertyMap.inputTypeToJavaType(securityContext, StructrTraits.PRINCIPAL, propertySet);

			// Remove any property which is not included in configuration
			// eMail is mandatory and necessary
			final String customAttributesString = PrincipalTraitDefinition.EMAIL_PROPERTY + "," + Settings.RegistrationCustomAttributes.getValue();
			final List<String> customAttributes = Arrays.asList(customAttributesString.split("[ ,]+"));

			final Set<PropertyKey> propsToRemove = new HashSet<>();
			for (final PropertyKey key : props.keySet()) {
				if (!customAttributes.contains(key.jsonName())) {
					propsToRemove.add(key);
				}
			}

			for (final PropertyKey propToRemove : propsToRemove) {
				props.remove(propToRemove);
			}

			props.put(credentialKey, credentialValue);
			props.put(confirmationKeyKey, confKey);

			user = app.create(userClass, props).as(Principal.class);

		} else {

			throw new FrameworkException(503, "User auto-creation is disabled, unable to register user");
		}

		return user;
	}

	@Override
	public String getTypeName(final SecurityContext securityContext) {
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean createPostTransaction() {
		return false;
	}

	@Override
	public Set<String> getAllowedHttpMethodsForOptionsCall() {
		return Set.of("OPTIONS", "POST");
	}
}
