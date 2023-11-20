package org.structr.web.resource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.APICallHandler;
import org.structr.api.search.SortOrder;
import org.structr.api.util.ResultStream;
import org.structr.common.AccessMode;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.MailHelper;
import org.structr.core.app.Query;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.MailTemplate;
import org.structr.core.entity.Principal;
import org.structr.core.property.PropertyKey;
import org.structr.core.property.PropertyMap;
import org.structr.core.script.Scripting;
import org.structr.rest.RestMethodResult;
import org.structr.rest.auth.AuthHelper;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.servlet.AbstractDataServlet;
import org.structr.schema.action.ActionContext;
import org.structr.web.entity.User;
import org.structr.web.servlet.HtmlServlet;

/**
 */
public class ResetPasswordResourceHandler extends APICallHandler {

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

	public ResetPasswordResourceHandler(final SecurityContext securityContext, final String url) {
		super(securityContext, url);
	}

	@Override
	public ResultStream doGet(final SortOrder sortOrder, int pageSize, int page) throws FrameworkException {
		throw new NotAllowedException("GET not allowed on " + getURL());
	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {
		throw new NotAllowedException("PUT not allowed on " + getURL());
	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if (propertySet.containsKey("eMail")) {

			String emailString  = (String) propertySet.get("eMail");

			if (StringUtils.isEmpty(emailString)) {
				throw new FrameworkException(422, "No e-mail address given.");
			}

			// cleanup user input
			emailString = emailString.trim().toLowerCase();

			final PropertyKey<String> confirmationKey = StructrApp.key(User.class, "confirmationKey");
			final PropertyKey<String> eMail           = StructrApp.key(User.class, "eMail");
			final String localeString                 = (String) propertySet.get("locale");
			final String confKey                      = AuthHelper.getConfirmationKey();
			final Principal user                      = StructrApp.getInstance().nodeQuery(User.class).and(eMail, emailString).getFirst();

			if (user != null) {

				// update confirmation key
				user.setProperties(SecurityContext.getSuperUserInstance(), new PropertyMap(confirmationKey, confKey));

				if (!sendResetPasswordLink(user, propertySet, localeString, confKey)) {

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

	@Override
	public RestMethodResult doOptions() throws FrameworkException {
		throw new NotAllowedException("OPTIONS not allowed on " + getURL());
	}

	private boolean sendResetPasswordLink(final Principal user, final Map<String, Object> propertySetFromUserPOST, final String localeString, final String confKey) throws FrameworkException {

		final String userEmail  = user.getProperty("eMail");
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

			final Query<MailTemplate> query = StructrApp.getInstance().nodeQuery(MailTemplate.class).andName(key.name());

			if (localeString != null) {
				query.and("locale", localeString);
			}

			MailTemplate template = query.getFirst();
			if (template != null) {

				final String text = template.getProperty("text");
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
	public Class getEntityClass() {
		return null;
	}

	@Override
	public boolean isCollection() {
		return false;
	}
}
