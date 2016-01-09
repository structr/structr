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
package org.structr.web.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.MailHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.app.App;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;
import org.structr.rest.service.HttpService;
import org.structr.web.entity.User;
import org.structr.web.servlet.HtmlServlet;

//~--- classes ----------------------------------------------------------------

/**
 * A resource to reset a user's password
 *
 *
 */
public class ResetPasswordResource extends Resource {

	private static final Logger logger = Logger.getLogger(ResetPasswordResource.class.getName());

	private enum TemplateKey {
		RESET_PASSWORD_SENDER_NAME,
		RESET_PASSWORD_SENDER_ADDRESS,
		RESET_PASSWORD_SUBJECT,
		RESET_PASSWORD_TEXT_BODY,
		RESET_PASSWORD_HTML_BODY,
		RESET_PASSWORD_BASE_URL,
		RESET_PASSWORD_TARGET_PAGE,
		RESET_PASSWORD_ERROR_PAGE,
		RESET_PASSWORD_PAGE,
		RESET_PASSWORD_CONFIRM_KEY_KEY,
		RESET_PASSWORD_TARGET_PAGE_KEY,
		RESET_PASSWORD_ERROR_PAGE_KEY
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

				// We only handle existing users but we don't want to disclose if this e-mail address exists,
				// so we're failing silently here
				return new RestMethodResult(HttpServletResponse.SC_OK);
			}

			if (user != null) {

				if (!sendResetPasswordLink(user, propertySet)) {

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

	private boolean sendResetPasswordLink(final Principal user, final Map<String, Object> propertySetFromUserPOST) {

		Map<String, String> replacementMap = new HashMap();

		// Populate the replacement map with all POSTed values
		// WARNING! This is unchecked user input!!
		populateReplacementMap(replacementMap, propertySetFromUserPOST);

		final String userEmail = user.getProperty(User.eMail);
		final String appHost   = Services.getInstance().getConfigurationValue(HttpService.APPLICATION_HOST);
		final String httpPort  = Services.getInstance().getConfigurationValue(HttpService.APPLICATION_HTTP_PORT);

		replacementMap.put(toPlaceholder(User.eMail.jsonName()), userEmail);
		replacementMap.put(toPlaceholder("link"),
			getTemplateText(TemplateKey.RESET_PASSWORD_BASE_URL, "http://" + appHost + ":" + httpPort)
			      + getTemplateText(TemplateKey.RESET_PASSWORD_PAGE, HtmlServlet.RESET_PASSWORD_PAGE)
			+ "?" + getTemplateText(TemplateKey.RESET_PASSWORD_CONFIRM_KEY_KEY, HtmlServlet.CONFIRM_KEY_KEY) + "=" + confKey
			+ "&" + getTemplateText(TemplateKey.RESET_PASSWORD_TARGET_PAGE_KEY, HtmlServlet.TARGET_PAGE_KEY) + "=" + getTemplateText(TemplateKey.RESET_PASSWORD_TARGET_PAGE, HtmlServlet.RESET_PASSWORD_PAGE)
		);

		String textMailTemplate = getTemplateText(TemplateKey.RESET_PASSWORD_TEXT_BODY, "Go to ${link} to reset your password.");
		String htmlMailTemplate = getTemplateText(TemplateKey.RESET_PASSWORD_HTML_BODY, "<div>Click <a href='${link}'>here</a> to reset your password.</div>");
		String textMailContent  = MailHelper.replacePlaceHoldersInTemplate(textMailTemplate, replacementMap);
		String htmlMailContent  = MailHelper.replacePlaceHoldersInTemplate(htmlMailTemplate, replacementMap);

		try {

			MailHelper.sendHtmlMail(
				getTemplateText(TemplateKey.RESET_PASSWORD_SENDER_ADDRESS, "structr-mail-daemon@localhost"),
				getTemplateText(TemplateKey.RESET_PASSWORD_SENDER_NAME, "Structr Mail Daemon"),
				userEmail, "", null, null, null,
				getTemplateText(TemplateKey.RESET_PASSWORD_SUBJECT, "Request to reset your Structr password"),
				htmlMailContent, textMailContent);

		} catch (Exception e) {

			logger.log(Level.SEVERE, "Unable to send reset password e-mail", e);
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

			Logger.getLogger(ResetPasswordResource.class.getName()).log(Level.WARNING, "Could not get mail template for key " + key, ex);

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

	//~--- get methods ----------------------------------------------------

	@Override
	public Class getEntityClass() {

		return null;

	}

	@Override
	public String getUriPart() {

		return "reset-password";

	}

	@Override
	public String getResourceSignature() {

		return "_resetPassword";

	}

	@Override
	public boolean isCollectionResource() {

		return false;

	}

}
