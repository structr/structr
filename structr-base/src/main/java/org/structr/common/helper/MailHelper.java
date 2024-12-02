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
package org.structr.common.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.structr.api.config.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


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

		HtmlEmail mail = new HtmlEmail();

		configureAdvancedMail(mail, amc);

		if (StringUtils.isNotBlank(amc.getFromName())) {
			mail.setFrom(amc.getFromAddress(), amc.getFromName());
		} else {
			mail.setFrom(amc.getFromAddress());
		}

		for (Map.Entry<String, String> entry : amc.getTo().entrySet()) {
			if (StringUtils.isNotBlank(entry.getValue())) {
				mail.addTo(entry.getKey(), entry.getValue());
			} else {
				mail.addTo(entry.getKey());
			}
		}

		for (Map.Entry<String, String> entry : amc.getCc().entrySet()) {
			if (StringUtils.isNotBlank(entry.getValue())) {
				mail.addCc(entry.getKey(), entry.getValue());
			} else {
				mail.addCc(entry.getKey());
			}
		}

		for (Map.Entry<String, String> entry : amc.getBcc().entrySet()) {
			if (StringUtils.isNotBlank(entry.getValue())) {
				mail.addBcc(entry.getKey(), entry.getValue());
			} else {
				mail.addBcc(entry.getKey());
			}
		}

		for (Map.Entry<String, String> entry : amc.getReplyTo().entrySet()) {
			if (StringUtils.isNotBlank(entry.getValue())) {
				mail.addReplyTo(entry.getKey(), entry.getValue());
			} else {
				mail.addReplyTo(entry.getKey());
			}
		}

		for (Map.Entry<String, String> entry : amc.getCustomHeaders().entrySet()) {
			mail.addHeader(entry.getKey(), entry.getValue());
		}

		if (StringUtils.isNotBlank(amc.getBounceAddress())) {
			mail.setBounceAddress(amc.getBounceAddress());
		}

		mail.setSubject(amc.getSubject());

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
			mail.attach(attachment.getDataSource(), attachment.getName(), attachment.getDescription(), attachment.getDisposition());
		}

		return mail.send();
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
				mail.attach(attachment.getDataSource(), attachment.getName(), attachment.getDescription(), attachment.getDisposition());
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

		mail.addTo(to, toName);
		mail.setFrom(from, fromName);

		if (StringUtils.isNotBlank(cc)) {
			mail.addCc(cc);
		}

		if (StringUtils.isNotBlank(bcc)) {
			mail.addBcc(bcc);
		}

		if (StringUtils.isNotBlank(bounce)) {
			mail.setBounceAddress(bounce);
		}

		mail.setSubject(subject);

	}

	private static void configureMail(final Email mail) {

		final String smtpHost        = Settings.SmtpHost.getValue();
		final int smtpPort           = Settings.SmtpPort.getValue();
		final String smtpUser        = Settings.SmtpUser.getValue();
		final String smtpPassword    = Settings.SmtpPassword.getValue();
		final boolean smtpUseTLS     = Settings.SmtpTlsEnabled.getValue();
		final boolean smtpRequireTLS = Settings.SmtpTlsRequired.getValue();

		configureMail(mail, smtpHost, smtpPort, smtpUser, smtpPassword, smtpUseTLS, smtpRequireTLS);
	}



	private static void configureAdvancedMail(final Email mail, final AdvancedMailContainer amc) {

		if (amc.shouldUseManualConfiguration()) {

			configureMail(mail, amc.getSmtpHost(), amc.getSmtpPort(), amc.getSmtpUser(), amc.getSmtpPassword(), amc.getSmtpUseTLS(), amc.getSmtpRequireTLS());

		} else {

			final String configurationPrefix = amc.getConfigurationPrefix();

			final String smtpHost        = Settings.SmtpHost.getPrefixedValue(configurationPrefix);
			final int smtpPort           = Settings.SmtpPort.getPrefixedValue(configurationPrefix);
			final String smtpUser        = Settings.SmtpUser.getPrefixedValue(configurationPrefix);
			final String smtpPassword    = Settings.SmtpPassword.getPrefixedValue(configurationPrefix);
			final boolean smtpUseTLS     = Settings.SmtpTlsEnabled.getPrefixedValue(configurationPrefix);
			final boolean smtpRequireTLS = Settings.SmtpTlsRequired.getPrefixedValue(configurationPrefix);

			configureMail(mail, smtpHost, smtpPort, smtpUser, smtpPassword, smtpUseTLS, smtpRequireTLS);
		}
	}

	private static void configureMail(final Email mail, final String smtpHost, final int smtpPort, final String smtpUser, final String smtpPassword, final boolean smtpUseTLS, final boolean smtpRequireTLS) {

		mail.setHostName(smtpHost);
		mail.setSmtpPort(smtpPort);
		mail.setStartTLSEnabled(smtpUseTLS);
		mail.setStartTLSRequired(smtpRequireTLS);
		mail.setCharset(charset);

		if (StringUtils.isNotBlank(smtpUser) && StringUtils.isNotBlank(smtpPassword)) {
			mail.setAuthentication(smtpUser, smtpPassword);
		}
	}

	/**
	 * Parse the template and replace any of the keys in the replacement map by
	 * the given values
	 *
	 * @param template
	 * @param replacementMap
	 * @return template string with included replacements
	 */
	public static String replacePlaceHoldersInTemplate(final String template, final Map<String, String> replacementMap) {

		List<String> toReplace = new ArrayList<>();
		List<String> replaceBy = new ArrayList<>();

		for (Entry<String, String> property : replacementMap.entrySet()) {

			toReplace.add(property.getKey());
			replaceBy.add(property.getValue());

		}

		return StringUtils.replaceEachRepeatedly(template, toReplace.toArray(new String[toReplace.size()]), replaceBy.toArray(new String[replaceBy.size()]));

	}

}
