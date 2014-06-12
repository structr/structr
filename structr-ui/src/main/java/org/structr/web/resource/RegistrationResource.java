/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.resource;

import java.util.Collections;
import org.structr.common.MailHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.auth.AuthHelper;
import org.structr.core.auth.Authenticator;
import org.structr.core.entity.Person;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyMap;
import org.structr.rest.service.HttpService;
import org.structr.web.entity.User;
import org.structr.core.entity.MailTemplate;
import org.structr.web.servlet.HtmlServlet;

//~--- classes ----------------------------------------------------------------

/**
 * A resource to register new users
 *
 * @author Axel Morgner
 */
public class RegistrationResource extends Resource {

	private static final Logger logger = Logger.getLogger(RegistrationResource.class.getName());

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

	private static String localeString;
	private static String confKey;

	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		return (getUriPart().equals(part));

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {

		throw new NotAllowedException();

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

			localeString = (String) propertySet.get(MailTemplate.locale.jsonName());
			confKey = UUID.randomUUID().toString();

			Result result = StructrApp.getInstance().nodeQuery(User.class).and(User.eMail, emailString).getResult();
			if (!result.isEmpty()) {

				final App app = StructrApp.getInstance(securityContext);
				user = (Principal) result.get(0);

				// For existing users, update confirmation key
				user.setProperty(User.confirmationKey, confKey);

				existingUser = true;


			} else {

				Authenticator auth = securityContext.getAuthenticator();
				user = createUser(securityContext, User.eMail, emailString, propertySet, auth.getUserAutoCreate(), auth.getUserClass());
			}

			if (user != null) {

				if (!sendInvitationLink(user, propertySet)) {

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

		throw new NotAllowedException();

	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		return null;

	}

	private boolean sendInvitationLink(final Principal user, final Map<String, Object> propertySetFromUserPOST) {

		Map<String, String> replacementMap = new HashMap();

		// Populate the replacement map with all POSTed values
		// WARNING! This is unchecked user input!!
		populateReplacementMap(replacementMap, propertySetFromUserPOST);

		final String userEmail = user.getProperty(User.eMail);
		final String appHost   = Services.getInstance().getConfigurationValue(HttpService.APPLICATION_HOST);
		final String httpPort  = Services.getInstance().getConfigurationValue(HttpService.APPLICATION_HTTP_PORT);

		replacementMap.put(toPlaceholder(User.eMail.jsonName()), userEmail);
		replacementMap.put(toPlaceholder("link"),
			getTemplateText(TemplateKey.BASE_URL, "http://" + appHost + ":" + httpPort)
			      + getTemplateText(TemplateKey.CONFIRM_REGISTRATION_PAGE, HtmlServlet.CONFIRM_REGISTRATION_PAGE)
			+ "?" + getTemplateText(TemplateKey.CONFIRM_KEY_KEY, HtmlServlet.CONFIRM_KEY_KEY) + "=" + confKey
			+ "&" + getTemplateText(TemplateKey.TARGET_PAGE_KEY, HtmlServlet.TARGET_PAGE_KEY) + "=" + getTemplateText(TemplateKey.TARGET_PAGE, "register_thanks")
			+ "&" + getTemplateText(TemplateKey.ERROR_PAGE_KEY, HtmlServlet.ERROR_PAGE_KEY)   + "=" + getTemplateText(TemplateKey.ERROR_PAGE, "register_error"));

		String textMailTemplate = getTemplateText(TemplateKey.TEXT_BODY, "Go to ${link} to finalize registration.");
		String htmlMailTemplate = getTemplateText(TemplateKey.HTML_BODY, "<div>Click <a href='${link}'>here</a> to finalize registration.</div>");
		String textMailContent  = MailHelper.replacePlaceHoldersInTemplate(textMailTemplate, replacementMap);
		String htmlMailContent  = MailHelper.replacePlaceHoldersInTemplate(htmlMailTemplate, replacementMap);

		try {

			MailHelper.sendHtmlMail(
				getTemplateText(TemplateKey.SENDER_ADDRESS, "structr-mail-daemon@localhost"),
				getTemplateText(TemplateKey.SENDER_NAME, "Structr Mail Daemon"),
				userEmail, "", null, null, null,
				getTemplateText(TemplateKey.SUBJECT, "Welcome to Structr, please finalize registration"),
				htmlMailContent, textMailContent);

		} catch (Exception e) {

			logger.log(Level.SEVERE, "Unable to send registration e-mail", e);
			return false;
		}

		return true;

	}

	private String getTemplateText(final TemplateKey key, final String defaultValue) {

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

			Logger.getLogger(RegistrationResource.class.getName()).log(Level.WARNING, "Could not get mail template for key " + key, ex);

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
	 * @return
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue) {

		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP);

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
	 * @return
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet) {

		return createUser(securityContext, credentialKey, credentialValue, propertySet, false);

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
	 * @return
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final boolean autoCreate) {

		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, autoCreate);

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
	 * @return
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final boolean autoCreate, final Class userClass) {

		return createUser(securityContext, credentialKey, credentialValue, Collections.EMPTY_MAP, autoCreate, userClass);

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
	 * @return
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final boolean autoCreate) {

		return createUser(securityContext, credentialKey, credentialValue, propertySet, autoCreate, User.class);

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
	 * @param userClass
	 * @return
	 */
	public static Principal createUser(final SecurityContext securityContext, final PropertyKey credentialKey, final String credentialValue, final Map<String, Object> propertySet, final boolean autoCreate, final Class userClass) {

		Principal user = null;

		try {

			// First, search for a person with that e-mail address
			user = AuthHelper.getPrincipalForCredential(credentialKey, credentialValue);

			if (user != null) {

				// convert to user
				user.unlockReadOnlyPropertiesOnce();
				user.setProperty(AbstractNode.type, User.class.getSimpleName());

				user = new User();

				user.init(securityContext, user.getNode());


				user.setProperty(User.confirmationKey, confKey);

			} else if (autoCreate) {

				// Clear properties set by us from the user-defined props
				propertySet.remove(credentialKey.jsonName());
				propertySet.remove(User.name.jsonName());
				propertySet.remove(User.confirmationKey.jsonName());

				PropertyMap props = PropertyMap.inputTypeToJavaType(securityContext, propertySet);

				props.put(credentialKey, credentialValue);
				props.put(User.name, credentialValue);
				props.put(User.confirmationKey, confKey);

				user = (Principal) StructrApp.getInstance(securityContext).create(userClass, props);

			}

		} catch (FrameworkException ex) {

			logger.log(Level.SEVERE, null, ex);

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
