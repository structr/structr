/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.process.function;

import org.apache.commons.mail.EmailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.common.error.ArgumentCountException;
import org.structr.common.error.ArgumentNullException;
import org.structr.common.error.FrameworkException;
import org.structr.common.helper.MailHelper;
import org.structr.core.app.QueryGroup;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.MailTemplate;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Traits;
import org.structr.core.traits.definitions.MailTemplateTraitDefinition;
import org.structr.docs.Example;
import org.structr.docs.Parameter;
import org.structr.docs.Signature;
import org.structr.docs.Usage;
import org.structr.docs.ontology.FunctionCategory;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.web.resource.ResetPasswordResource;
import org.structr.web.resource.ResetPasswordResourceHandler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Generic notification function for the Structr Process Engine.
 *
 * Dispatches notifications to different channels (email, log) based on
 * the channel parameter. Designed to be called from BPMN service tasks
 * for process-driven notifications.
 *
 * Usage:
 *   notify(channel, recipient, subject, message)
 *
 * Channels:
 *   "email"     - sends HTML email via configured SMTP (falls back to plaintext)
 *   "log"       - logs the notification via SLF4J (useful for testing/debugging)
 *
 * Future channels (not yet implemented):
 *   "mqtt"      - publish to MQTT topic
 *   "kafka"     - publish to Kafka topic
 *   "webhook"   - HTTP POST to URL
 */
public class NotifyFunction extends Function<Object, Object> {

	private static final Logger logger = LoggerFactory.getLogger(NotifyFunction.class.getName());

	private enum TemplateKey {
		PROCESS_NOTIFICATION_SENDER_NAME,
		PROCESS_NOTIFICATION_SENDER_ADDRESS,
		PROCESS_NOTIFICATION_SUBJECT,
		PROCESS_NOTIFICATION_TEXT_BODY,
		PROCESS_NOTIFICATION_HTML_BODY,
		PROCESS_NOTIFICATION_BASE_URL,
		PROCESS_NOTIFICATION_TARGET_PAGE,
		PROCESS_NOTIFICATION_ERROR_PAGE,
		PROCESS_NOTIFICATION_PAGE,
		PROCESS_NOTIFICATION_CONFIRMATION_KEY_KEY,
		PROCESS_NOTIFICATION_TARGET_PAGE_KEY,
		PROCESS_NOTIFICATION_ERROR_PAGE_KEY
	}

	@Override
	public String getName() {
		return "notify";
	}

	@Override
	public String getRequiredModule() {
		return null;
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) throws FrameworkException {

		try {

			assertArrayHasMinLengthAndAllElementsNotNull(sources, 4);

			final String channel   = sources[0].toString();
			final String recipient = sources[1].toString();
			final String subject   = sources[2].toString();
			final String message   = sources[3].toString();

			switch (channel) {

				case "email":
					return sendEmailNotification(recipient, subject, message);

				case "log":
					logger.info("NOTIFY [{}] to={} subject={} message={}", channel, recipient, subject, message);
					return true;

				default:
					logger.warn("Unknown notification channel: {}. Supported channels: email, log", channel);
					throw new FrameworkException(422, "Unknown notification channel: " + channel + ". Supported channels: email, log");
			}

		} catch (ArgumentNullException | ArgumentCountException ex) {

			logParameterError(caller, sources, ex.getMessage(), ctx.isJavaScriptContext());
			return usage(ctx.isJavaScriptContext());
		}
	}

	/**
	 * Sends an email notification using the configured SMTP settings.
	 * Uses the system's configured sender address (smtp.sender) as the from address.
	 */
	private Object sendEmailNotification(final String recipient, final String subject, final String message) throws FrameworkException {

		final String smtpUserSetting      = Settings.SmtpUser.getValue();
		final String defaultSenderAddress = (Settings.isValidEmail(smtpUserSetting)) ? smtpUserSetting : "structr-mail-daemon@localhost";

		final String fromAddress = getTemplateText(TemplateKey.PROCESS_NOTIFICATION_SENDER_ADDRESS, defaultSenderAddress, Locale.getDefault().toString());
		final String fromName    = getTemplateText(TemplateKey.PROCESS_NOTIFICATION_SENDER_NAME, "Structr Mail Daemon", Locale.getDefault().toString());

		try {

			// Send as HTML with plaintext fallback
			return MailHelper.sendHtmlMail(
				fromAddress,
				fromName,
				recipient,
				recipient,   // toName = recipient address (no separate name available)
				null,        // cc
				null,        // bcc
				fromAddress, // bounce address
				subject,
				message,     // HTML content
				message      // plaintext fallback (same content, stripped by mail client)
			);

		} catch (final EmailException eex) {

			throw new FrameworkException(422, "Failed to send email notification: " + eex.getMessage(), eex.getCause());
		}
	}

	private String getTemplateText(final TemplateKey key, final String defaultValue, final String localeString) {

		try {

			final QueryGroup<NodeInterface> query = StructrApp.getInstance().nodeQuery(StructrTraits.MAIL_TEMPLATE).name(key.name());

			if (localeString != null) {
				query.key(Traits.of(StructrTraits.MAIL_TEMPLATE).key(MailTemplateTraitDefinition.LOCALE_PROPERTY), localeString);
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

	// --- Documentation methods ---

	@Override
	public List<Signature> getSignatures() {
		return Signature.forAllScriptingLanguages("channel, recipient, subject, message");
	}

	@Override
	public List<Usage> getUsages() {

		return List.of(
			Usage.structrScript("Usage: ${notify(channel, recipient, subject, message)}"),
			Usage.javaScript("Usage: ${{$.notify(channel, recipient, subject, message)}}")
		);
	}

	@Override
	public String getShortDescription() {
		return "Sends a notification via the specified channel.";
	}

	@Override
	public String getLongDescription() {

		return "Generic notification function for the Structr Process Engine. Dispatches notifications to different "
			+ "channels based on the first parameter. Currently supports 'email' (sends via configured SMTP) and "
			+ "'log' (writes to server log). Designed to be called from BPMN service tasks for process-driven "
			+ "notifications. Future channels include MQTT, Kafka, and webhook.";
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"The 'email' channel uses the SMTP configuration from structr.conf (smtp.sender, smtp.name).",
			"The 'log' channel writes to the server log at INFO level -- useful for testing process flows without configuring SMTP.",
			"Additional channels (mqtt, kafka, webhook) will be added in future releases.",
			"This function is designed for use in BPMN service tasks but can be called from any scripting context."
		);
	}

	@Override
	public List<Parameter> getParameters() {

		return List.of(
			Parameter.mandatory("channel", "notification channel: 'email' or 'log'"),
			Parameter.mandatory("recipient", "recipient address (email address for 'email' channel, identifier for other channels)"),
			Parameter.mandatory("subject", "notification subject"),
			Parameter.mandatory("message", "notification message body (HTML supported for email channel)")
		);
	}

	@Override
	public List<Example> getExamples() {

		return List.of(
			Example.structrScript(
				"${notify('email', 'user@example.com', 'Leave Request Approved', 'Your leave request has been approved.')}",
				"Send an email notification"
			),
			Example.structrScript(
				"${notify('log', 'admin', 'Process Completed', 'Leave request process completed for user X.')}",
				"Log a notification (for testing)"
			),
			Example.javaScript(
				"${{$.notify('email', user.eMail, 'Task Assigned', 'You have a new task: ' + task.name);}}",
				"Send email notification in JavaScript using dynamic data"
			)
		);
	}

	@Override
	public FunctionCategory getCategory() {
		return FunctionCategory.Miscellaneous;
	}
}
