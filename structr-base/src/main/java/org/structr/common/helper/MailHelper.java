/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.common.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.structr.api.config.Settings;

import java.util.List;
import java.util.Map;

public abstract class MailHelper {

	private static final String charset      = "UTF-8";

	public MailHelper() {}

	public static String sendHtmlMail(final String from, final String fromName, final String to, final String toName, final String cc, final String bcc, final String bounce, final String subject, final String htmlContent, final String textContent) throws EmailException {

		return _sendHtmlMail(from, fromName, to, toName, cc, bcc, bounce, subject, htmlContent, textContent, null);
	}

	public static String sendHtmlMail(final String from, final String fromName, final String to, final String toName, final String cc, final String bcc, final String bounce, final String subject, final String htmlContent, final String textContent, final List<DynamicMailAttachment> attachments) throws EmailException {

		return _sendHtmlMail(from, fromName, to, toName, cc, bcc, bounce, subject, htmlContent, textContent, attachments);
	}

	public static String sendAdvancedMail(final AdvancedMailContainer amc) throws EmailException {

		if (Settings.SmtpTesting.getValue()) {

			return "Testing";
		}

		return createAdvancedMail(amc).send();
	}

	public static HtmlEmail createAdvancedMail(final AdvancedMailContainer amc) throws EmailException {

		HtmlEmail mail = new HtmlEmail();

		configureAdvancedMail(mail, amc);

		if (StringUtils.isNotBlank(amc.getFromName())) {

			mail.setFrom(amc.getFromAddress(), stripLineBreaks(amc.getFromName()));

		} else {

			mail.setFrom(amc.getFromAddress());
		}

		for (Map.Entry<String, String> entry : amc.getTo().entrySet()) {

			if (StringUtils.isNotBlank(entry.getValue())) {

				mail.addTo(entry.getKey(), stripLineBreaks(entry.getValue()));

			} else {

				mail.addTo(entry.getKey());
			}
		}

		for (Map.Entry<String, String> entry : amc.getCc().entrySet()) {

			if (StringUtils.isNotBlank(entry.getValue())) {

				mail.addCc(entry.getKey(), stripLineBreaks(entry.getValue()));

			} else {

				mail.addCc(entry.getKey());
			}
		}

		for (Map.Entry<String, String> entry : amc.getBcc().entrySet()) {

			if (StringUtils.isNotBlank(entry.getValue())) {

				mail.addBcc(entry.getKey(), stripLineBreaks(entry.getValue()));

			} else {

				mail.addBcc(entry.getKey());
			}
		}

		for (Map.Entry<String, String> entry : amc.getReplyTo().entrySet()) {

			if (StringUtils.isNotBlank(entry.getValue())) {

				mail.addReplyTo(entry.getKey(), stripLineBreaks(entry.getValue()));

			} else {

				mail.addReplyTo(entry.getKey());
			}
		}

		for (Map.Entry<String, String> entry : amc.getCustomHeaders().entrySet()) {

			mail.addHeader(stripLineBreaks(entry.getKey()), stripLineBreaks(entry.getValue()));
		}

		if (StringUtils.isNotBlank(amc.getBounceAddress())) {

			mail.setBounceAddress(amc.getBounceAddress());
		}

		mail.setSubject(stripLineBreaks(amc.getSubject()));

		if (StringUtils.isNotBlank(amc.getHtmlContent())) {

			mail.setHtmlMsg(amc.getHtmlContent());
		}

		if (StringUtils.isNotBlank(amc.getTextContent())) {

			mail.setTextMsg(amc.getTextContent());
		}

		for (final Pair<String, String> part : amc.getMimeParts()) {

			mail.addPart(part.getLeft(), part.getRight());
		}

		for (final DynamicMailAttachment attachment : amc.getAttachments()) {

			// the attachment name is script-controlled and ends up in the part headers, where a line
			// break starts a header of its own
			mail.attach(attachment.getDataSource(), stripLineBreaks(attachment.getName()), stripLineBreaks(attachment.getDescription()), attachment.getDisposition());
		}

		return mail;
	}

	private static String _sendHtmlMail(final String from, final String fromName, final String to, final String toName, final String cc, final String bcc, final String bounce, final String subject, final String htmlContent, final String textContent, final List<DynamicMailAttachment> attachments) throws EmailException {

		if (Settings.SmtpTesting.getValue()) {

			return "Testing";
		}

		HtmlEmail mail = new HtmlEmail();

		setup(mail, to, toName, from, fromName, cc, bcc, bounce, subject);
		mail.setHtmlMsg(htmlContent);
		mail.setTextMsg(textContent);

		if (attachments != null) {

			for (final DynamicMailAttachment attachment : attachments) {

				// the attachment name is script-controlled and ends up in the part headers, where a line
				// break starts a header of its own
				mail.attach(attachment.getDataSource(), stripLineBreaks(attachment.getName()), stripLineBreaks(attachment.getDescription()), attachment.getDisposition());
			}
		}

		return mail.send();
	}

	public static String sendSimpleMail(final String from, final String fromName, final String to, final String toName, final String cc, final String bcc, final String bounce, final String subject, final String textContent) throws EmailException {

		SimpleEmail mail = new SimpleEmail();

		setup(mail, to, toName, from, fromName, cc, bcc, bounce, subject);
		mail.setMsg(textContent);

		return mail.send();
	}

	private static void setup(final Email mail, final String to, final String toName, final String from, final String fromName, final String cc, final String bcc, final String bounce, final String subject) throws EmailException {

		configureMail(mail);

		mail.addTo(to, stripLineBreaks(toName));
		mail.setFrom(from, stripLineBreaks(fromName));

		if (StringUtils.isNotBlank(cc)) {

			mail.addCc(cc);
		}

		if (StringUtils.isNotBlank(bcc)) {

			mail.addBcc(bcc);
		}

		if (StringUtils.isNotBlank(bounce)) {

			mail.setBounceAddress(bounce);
		}

		mail.setSubject(stripLineBreaks(subject));
	}

	/**
	 * Removes CR and LF from a value that is about to become part of a mail header.
	 *
	 * <p>A line break in a header name ends the header and starts a new one, so a name like
	 * "X-Custom\r\nBcc" adds a recipient that the sender never asked for. In a header value or a
	 * personal name, Jakarta Mail folds the break into a continuation line instead, which cannot add
	 * a header but still puts attacker-controlled text where the sender expects a name.
	 *
	 * <p>Addresses are deliberately left alone: Jakarta Mail rejects a control character in an
	 * address, and stripping it here would turn a rejected address into an accepted, wrong one.
	 *
	 * @param value a header name, header value, personal name or subject
	 * @return the value without CR and LF
	 */
	public static String stripLineBreaks(final String value) {

		return StringUtils.replaceChars(value, "\r\n", "");
	}

	private static void configureMail(final Email mail) {

		final String smtpHost        = Settings.SmtpHost.getValue();
		final int smtpPort           = Settings.SmtpPort.getValue();
		final String smtpUser        = Settings.SmtpUser.getValue();
		final String smtpPassword    = Settings.SmtpPassword.getValue();
		final boolean smtpTLSEnabled = Settings.SmtpTlsEnabled.getValue();
		final boolean smtpTLSRequired = Settings.SmtpTlsRequired.getValue();

		configureMail(mail, smtpHost, smtpPort, smtpUser, smtpPassword, smtpTLSEnabled, smtpTLSRequired);
	}

	private static void configureAdvancedMail(final Email mail, final AdvancedMailContainer amc) {

		if (amc.shouldUseManualConfiguration()) {

			configureMail(mail, amc.getSmtpHost(), amc.getSmtpPort(), amc.getSmtpUser(), amc.getSmtpPassword(), amc.getSmtpTLSEnabled(), amc.getSmtpTLSRequired());

		} else {

			final String configurationPrefix = amc.getConfigurationPrefix();
			final String smtpHost         = Settings.SmtpHost.getPrefixedValue(configurationPrefix);
			final int smtpPort            = Settings.SmtpPort.getPrefixedValue(configurationPrefix);
			final String smtpUser         = Settings.SmtpUser.getPrefixedValue(configurationPrefix);
			final String smtpPassword     = Settings.SmtpPassword.getPrefixedValue(configurationPrefix);
			final boolean smtpTLSEnabled  = Settings.SmtpTlsEnabled.getPrefixedValue(configurationPrefix);
			final boolean smtpTLSRequired = Settings.SmtpTlsRequired.getPrefixedValue(configurationPrefix);

			configureMail(mail, smtpHost, smtpPort, smtpUser, smtpPassword, smtpTLSEnabled, smtpTLSRequired);
		}
	}

	private static void configureMail(final Email mail, final String smtpHost, final int smtpPort, final String smtpUser, final String smtpPassword, final boolean smtpTLSEnabled, final boolean smtpTLSRequired) {

		mail.setHostName(smtpHost);
		mail.setSmtpPort(smtpPort);
		mail.setStartTLSEnabled(smtpTLSEnabled);
		mail.setStartTLSRequired(smtpTLSRequired);
		mail.setCharset(charset);

		if (StringUtils.isNotBlank(smtpUser) && StringUtils.isNotBlank(smtpPassword)) {

			mail.setAuthentication(smtpUser, smtpPassword);
		}
	}

//	/**
//	 * Parse the template and replace any of the keys in the replacement map by
//	 * the given values
//	 *
//	 * @param template
//	 * @param replacementMap
//	 * @return template string with included replacements
//	 */
//	public static String replacePlaceHoldersInTemplate(final String template, final Map<String, String> replacementMap) {
//
//		List<String> toReplace = new ArrayList<>();
//		List<String> replaceBy = new ArrayList<>();
//
//		for (Entry<String, String> property : replacementMap.entrySet()) {
//
//			toReplace.add(property.getKey());
//			replaceBy.add(property.getValue());
//
//		}
//
//		return StringUtils.replaceEachRepeatedly(template, toReplace.toArray(new String[toReplace.size()]), replaceBy.toArray(new String[replaceBy.size()]));
//	}
}
