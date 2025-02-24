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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.MailHelper;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.graph.NodeInterface;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.rest.RestMethodResult;
import org.structr.rest.api.RESTCall;
import org.structr.rest.api.RESTCallHandler;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.servlet.AbstractDataServlet;
import org.structr.schema.action.ActionContext;
import org.structr.web.servlet.HtmlServlet;

import java.util.Map;
import java.util.Set;

/**
 */
public class ResetPasswordResourceHandler extends RESTCallHandler {

	private static final Logger logger = LoggerFactory.getLogger(ResetPasswordResourceHandler.class.getName());

	private enum TemplateKey {
		RESET_PASSWORD_SENDER_NAME,
		RESET_PASSWORD_SENDER_ADDRESS,
		RESET_PASSWORD_SUBJECT,
		RESET_PASSWORD_TEXT_BODY,
		RESET_PASSWORD_HTML_BODY,
		RESET_PASSWORD_BASE_URL,
		RESET_PASSWORD_TARGET_PATH,
		RESET_PASSWORD_ERROR_PAGE,
		RESET_PASSWORD_PAGE,
		RESET_PASSWORD_CONFIRMATION_KEY_KEY,
		RESET_PASSWORD_TARGET_PATH_KEY,
		RESET_PASSWORD_ERROR_PAGE_KEY
	}

	public ResetPasswordResourceHandler(final RESTCall call) {
		super(call);
	}

	@Override
	public RestMethodResult doPost(final SecurityContext securityContext, final Map<String, Object> propertySet) throws FrameworkException {

		if (propertySet.containsKey("eMail")) {

			String emailString  = (String) propertySet.get("eMail");

			if (StringUtils.isEmpty(emailString)) {
				throw new FrameworkException(422, "No e-mail address given.");
			}

			// cleanup user input
			emailString = emailString.trim().toLowerCase();

			final Traits traits                       = Traits.of(StructrTraits.USER);
			final PropertyKey<String> confirmationKey = traits.key("confirmationKey");
			final PropertyKey<String> eMail           = traits.key("eMail");
			final String localeString                 = (String) propertySet.get("locale");
			final String confKey                      = AuthHelper.getConfirmationKey();
			final NodeInterface user                  = StructrApp.getInstance().nodeQuery(StructrTraits.USER).and(eMail, emailString).getFirst();

			if (user != null) {

				// update confirmation key
				user.setProperties(SecurityContext.getSuperUserInstance(), new PropertyMap(confirmationKey, confKey));

				if (!sendResetPasswordLink(securityContext, user.as(Principal.class), propertySet, localeString, confKey)) {

					throw new FrameworkException(503, "Unable to send confirmation e-mail.");
				}

				// return 200 OK
				return new RestMethodResult(HttpServletResponse.SC_OK);

			} else {

				// We only handle existing users but we don't want to disclose if this e-mail address exists,
				// so we're failing silently here
				return new RestMethodResult(HttpServletResponse.SC_OK);
			}

		} else {

			throw new FrameworkException(422, "No e-mail address given.");
		}
	}

	private boolean sendResetPasswordLink(final SecurityContext securityContext, final Principal user, final Map<String, Object> propertySetFromUserPOST, final String localeString, final String confKey) throws FrameworkException {

		final String userEmail  = user.getEMail();
		final ActionContext ctx = new ActionContext(SecurityContext.getInstance(user, AccessMode.Frontend));

		// Populate the replacement map with all POSTed values
		// WARNING! This is unchecked user input!!
		propertySetFromUserPOST.entrySet().forEach(entry -> ctx.setConstant(entry.getKey(), entry.getValue().toString()));

		ctx.setConstant("eMail", userEmail);
		ctx.setConstant("link",
				getTemplateText(TemplateKey.RESET_PASSWORD_BASE_URL, ActionContext.getBaseUrl(securityContext.getRequest()), localeString)
				+ getTemplateText(TemplateKey.RESET_PASSWORD_PAGE, HtmlServlet.RESET_PASSWORD_PAGE, localeString)
				+ "?" + getTemplateText(TemplateKey.RESET_PASSWORD_CONFIRMATION_KEY_KEY, HtmlServlet.CONFIRMATION_KEY_KEY, localeString) + "=" + confKey
				+ "&" + getTemplateText(TemplateKey.RESET_PASSWORD_TARGET_PATH_KEY, HtmlServlet.TARGET_PATH_KEY, localeString) + "=" + getTemplateText(TemplateKey.RESET_PASSWORD_TARGET_PATH, AbstractDataServlet.prefixLocation(HtmlServlet.RESET_PASSWORD_PAGE), localeString)
		);

		final String textMailContent = replaceVariablesInTemplate(TemplateKey.RESET_PASSWORD_TEXT_BODY, "Go to ${link} to reset your password.", localeString, ctx);
		final String htmlMailContent = replaceVariablesInTemplate(TemplateKey.RESET_PASSWORD_HTML_BODY, "<div>Click <a href='${link}'>here</a> to reset your password.</div>", localeString, ctx);

		try {

			MailHelper.sendHtmlMail(
				getTemplateText(TemplateKey.RESET_PASSWORD_SENDER_ADDRESS, "structr-mail-daemon@localhost", localeString),
				getTemplateText(TemplateKey.RESET_PASSWORD_SENDER_NAME, "Structr Mail Daemon", localeString),
				userEmail, "", null, null, null,
				getTemplateText(TemplateKey.RESET_PASSWORD_SUBJECT, "Request to reset your Structr password", localeString),
				htmlMailContent, textMailContent);

		} catch (Exception e) {

			logger.error("Unable to send reset password e-mail", e);
			return false;
		}

		return true;

	}

	private String getTemplateText(final TemplateKey key, final String defaultValue, final String localeString) {

		try {

			final Query<NodeInterface> query = StructrApp.getInstance().nodeQuery(StructrTraits.MAIL_TEMPLATE).andName(key.name());

			if (localeString != null) {
				query.and(Traits.of(StructrTraits.MAIL_TEMPLATE).key("locale"), localeString);
			}

			NodeInterface template = query.getFirst();
			if (template != null) {

				final String text = template.as(MailTemplate.class).getText();
				return text != null ? text : defaultValue;

			} else {

				return defaultValue;
			}

		} catch (FrameworkException ex) {

			LoggerFactory.getLogger(ResetPasswordResource.class.getName()).warn("Could not get mail template for key " + key, ex);
		}

		return null;
	}

	private String replaceVariablesInTemplate(final TemplateKey key, final String defaultValue, final String localeString, final ActionContext ctx) throws FrameworkException {

		try {

			final String templateText = getTemplateText(key, defaultValue, localeString);

			return Scripting.replaceVariables(ctx, null, templateText, "sendResetPassword()");

		} catch (FrameworkException fxe) {

			logger.warn("Unable to send confirmation e-mail, scripting error in {} template. {}", key.name(), fxe);

			throw new FrameworkException(503, "Unable to send confirmation e-mail");
		}
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
	public Set<String> getAllowedHttpMethodsForOptionsCall() {
		return Set.of("OPTIONS", "POST");
	}
}
