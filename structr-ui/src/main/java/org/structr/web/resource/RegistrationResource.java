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
package org.structr.web.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.MailHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeFactory;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.rest.RestMethodResult;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;
import org.structr.web.entity.User;
import org.structr.web.servlet.HtmlServlet;

//~--- classes ----------------------------------------------------------------

/**
 * A resource to register new users
 *
 *
 */
public class RegistrationResource extends Resource {

	private static final Logger logger = LoggerFactory.getLogger(RegistrationResource.class.getName());

	private enum TemplateKey {
		SENDER_NAME,
		SENDER_ADDRESS,
		SUBJECT,
		TEXT_BODY,
		HTML_BODY,
		BASE_URL,
		TARGET_PAGE,
		ERROR_PAGE,
		CONFIRM_REGISTRATION_PAGE,
		CONFIRM_KEY_KEY,
		TARGET_PAGE_KEY,
		ERROR_PAGE_KEY
	}

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		return (getUriPart().equals(part));

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page) throws FrameworkException {
		throw new NotAllowedException("GET not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException("PUT not allowed on " + getResourceSignature());
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		boolean existingUser = false;

		if (propertySet.containsKey(User.eMail.jsonName())) {

			final Principal user;

			final String emailString  = (String) propertySet.get(User.eMail.jsonName());

			if (StringUtils.isEmpty(emailString)) {
				return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);
			}

			final String localeString = (String) propertySet.get(MailTemplate.locale.jsonName());
			final String confKey      = UUID.randomUUID().toString();

			final Result result = StructrApp.getInstance().nodeQuery(User.class).and(User.eMail, emailString).getResult();
			if (!result.isEmpty()) {

				user = (Principal) result.get(0);

				// For existing users, update confirmation key
				user.setProperties(securityContext, new PropertyMap(User.confirmationKey, confKey));

				existingUser = true;


			} else {

				final Authenticator auth = securityContext.getAuthenticator();
				user = createUser(securityContext, User.eMail, emailString, propertySet, Settings.RestUserAutocreate.getValue(), auth.getUserClass(), confKey);
			}

			if (user != null) {

				if (!sendInvitationLink(user, propertySet, confKey, localeString)) {

					// return 400 Bad request
					return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);

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

				// return 400 Bad request
				return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);

			}


		} else {

			// return 400 Bad request
			return new RestMethodResult(HttpServletResponse.SC_BAD_REQUEST);

		}

	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new NotAllowedException("OPTIONS not allowed on " + getResourceSignature());
	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		return null;

	}

	private boolean sendInvitationLink(final Principal user, final Map<String, Object> propertySetFromUserPOST, final String confKey, final String localeString) {

		Map<String, String> replacementMap = new HashMap();

		// Populate the replacement map with all POSTed values
		// WARNING! This is unchecked user input!!
		populateReplacementMap(replacementMap, propertySetFromUserPOST);

		final String userEmail = user.getProperty(User.eMail);
		final String appHost   = Settings.ApplicationHost.getValue();
		final Integer httpPort = Settings.HttpPort.getValue();

		replacementMap.put(toPlaceholder(User.eMail.jsonName()), userEmail);
		replacementMap.put(toPlaceholder("link"),
			getTemplateText(TemplateKey.BASE_URL, "http://" + appHost + ":" + httpPort, localeString)
			      + getTemplateText(TemplateKey.CONFIRM_REGISTRATION_PAGE, HtmlServlet.CONFIRM_REGISTRATION_PAGE, localeString)
			+ "?" + getTemplateText(TemplateKey.CONFIRM_KEY_KEY, HtmlServlet.CONFIRM_KEY_KEY, localeString) + "=" + confKey
			+ "&" + getTemplateText(TemplateKey.TARGET_PAGE_KEY, HtmlServlet.TARGET_PAGE_KEY, localeString) + "=" + getTemplateText(TemplateKey.TARGET_PAGE, "register_thanks", localeString)
			+ "&" + getTemplateText(TemplateKey.ERROR_PAGE_KEY, HtmlServlet.ERROR_PAGE_KEY, localeString)   + "=" + getTemplateText(TemplateKey.ERROR_PAGE, "register_error", localeString)
		);

		String textMailTemplate = getTemplateText(TemplateKey.TEXT_BODY, "Go to ${link} to finalize registration.", localeString);
		String htmlMailTemplate = getTemplateText(TemplateKey.HTML_BODY, "<div>Click <a href='${link}'>here</a> to finalize registration.</div>", localeString);
		String textMailContent  = MailHelper.replacePlaceHoldersInTemplate(textMailTemplate, replacementMap);
		String htmlMailContent  = MailHelper.replacePlaceHoldersInTemplate(htmlMailTemplate, replacementMap);

		try {

			MailHelper.sendHtmlMail(
				getTemplateText(TemplateKey.SENDER_ADDRESS, "structr-mail-daemon@localhost", localeString),
				getTemplateText(TemplateKey.SENDER_NAME, "Structr Mail Daemon", localeString),
				userEmail, "", null, null, null,
				getTemplateText(TemplateKey.SUBJECT, "Welcome to Structr, please finalize registration", localeString),
				htmlMailContent, textMailContent);

		} catch (Exception e) {

			logger.error("Unable to send registration e-mail", e);
			return false;
		}

		return true;

	}

	private String getTemplateText(final TemplateKey key, final String defaultValue, final String localeString) {

		try {

			final Query<MailTemplate> query = StructrApp.getInstance().nodeQuery(MailTemplate.class).andName(key.name());

			if (localeString != null) {
				query.and(MailTemplate.locale, localeString);
			}

			MailTemplate template = query.getFirst();
			if (template != null) {

				final String text = template.getProperty(MailTemplate.text);
				return text != null ? text : defaultValue;

			} else {

				return defaultValue;

			}

		} catch (FrameworkException ex) {

			LoggerFactory.getLogger(RegistrationResource.class.getName()).warn("Could not get mail template for key " + key, ex);

		}

		return null;

	}

	private static void populateReplacementMap(final Map<String, String> replacementMap, final Map<String, Object> props) {

		for (Entry<String, Object> entry : props.entrySet()) {

			replacementMap.put(toPlaceholder(entry.getKey()), entry.getValue().toString());

		}

	}

	private static String toPlaceholder(final String key) {

		return "${".concat(key).concat("}");

	}

	/**
	 * Create a new user.
	 *
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @return user
	 */
	public Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final String confKey) {

		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, confKey);

	}

	/**
	 * Create a new user.
	 *
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param propertySet
	 * @return user
	 */
	public Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final String confKey) {

		return createUser(securityContext, credentialKey, credentialValue, propertySet, false, confKey);

	}

	/**
	 * Create a new user.
	 *
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param autoCreate
	 * @return user
	 */
	public Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final boolean autoCreate, final String confKey) {

		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, autoCreate, confKey);

	}

	/**
	 * Create a new user.
	 *
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * Do not auto-create a new user.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param autoCreate
	 * @param userClass
	 * @return user
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final boolean autoCreate, final Class userClass, final String confKey) {

		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, autoCreate, userClass, confKey);

	}

	/**
	 * Create a new user.
	 *
	 * If a {@link Person} is found, convert that object to a {@link User} object.
	 * If autoCreate is true, auto-create a new user, even if no matching person is found.
	 *
	 * @param securityContext
	 * @param credentialKey
	 * @param credentialValue
	 * @param propertySet
	 * @param autoCreate
	 * @return user
	 */
	public Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final boolean autoCreate, final String confKey) {

		return createUser(securityContext, credentialKey, credentialValue, propertySet, autoCreate, User.class, confKey);

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
	 * @param userClass
	 * @return user
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final boolean autoCreate, final Class userClass, final String confKey) {

		Principal user = null;

		try {

			// First, search for a person with that e-mail address
			user = AuthHelper.getPrincipalForCredential(credentialKey, credentialValue);

			if (user != null) {

				user = new NodeFactory<Principal>(securityContext).instantiate(user.getNode());

				// convert to user
				user.unlockSystemPropertiesOnce();

				final PropertyMap changedProperties = new PropertyMap();
				changedProperties.put(AbstractNode.type, User.class.getSimpleName());
				changedProperties.put(User.confirmationKey, confKey);
				user.setProperties(securityContext, changedProperties);

			} else if (autoCreate) {

				final App app = StructrApp.getInstance(securityContext);

				// Clear properties set by us from the user-defined props
				propertySet.remove(credentialKey.jsonName());
				propertySet.remove(User.confirmationKey.jsonName());

				PropertyMap props = PropertyMap.inputTypeToJavaType(securityContext, Principal.class, propertySet);

				// Remove any property which is not included in configuration
				// eMail is mandatory and necessary
				final String customAttributesString = User.eMail.jsonName() + "," + Settings.RegistrationCustomAttributes.getValue();
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
				props.put(User.confirmationKey, confKey);

//				// Remove security-relevant properties
//				props.remove(Principal.isAdmin);
//				props.remove(Principal.ownedNodes);
//				props.remove(Principal.salt);
//				props.remove(Principal.sessionIds);

				user = (Principal) app.create(userClass, props);

			} else {

				logger.info("User self-registration is not configured yet, cannot create new user.");
			}

		} catch (FrameworkException ex) {

			logger.error("", ex);

		}

		return user;

	}


	//~--- get methods ----------------------------------------------------

	@Override
	public Class getEntityClass() {

		return null;

	}

	@Override
	public String getUriPart() {

		return "registration";

	}

	@Override
	public String getResourceSignature() {

		return "_registration";

	}

	@Override
	public boolean isCollectionResource() {

		return false;

	}

}
