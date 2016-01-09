/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.structr.core.Services;

//~--- JDK imports ------------------------------------------------------------


//~--- classes ----------------------------------------------------------------

/**
 * Helper class for sending simple or HTML e-mails.
 *
 *
 */
public abstract class MailHelper {

	private static final String charset      = "UTF-8";

	//~--- constructors ---------------------------------------------------

	public MailHelper() {}

	//~--- methods --------------------------------------------------------

	public static String sendHtmlMail(final String from, final String fromName, final String to, final String toName, final String cc, final String bcc, final String bounce, final String subject,
					final String htmlContent, final String textContent)
		throws EmailException {

		HtmlEmail mail = new HtmlEmail();

		setup(mail, to, toName, from, fromName, cc, bcc, bounce, subject);
		mail.setHtmlMsg(htmlContent);
		mail.setTextMsg(textContent);

		return mail.send();
	}

	public static String sendSimpleMail(final String from, final String fromName, final String to, final String toName, final String cc, final String bcc, final String bounce, final String subject,
					  final String textContent)
		throws EmailException {

		SimpleEmail mail = new SimpleEmail();

		setup(mail, to, toName, from, fromName, cc, bcc, bounce, subject);
		mail.setMsg(textContent);

		return mail.send();
	}

	private static void setup(final Email mail, final String to, final String toName, final String from, final String fromName, final String cc, final String bcc, final String bounce, final String subject)
		throws EmailException {

		// FIXME: this might be slow if the config file is read each time
		final Properties config     = Services.getInstance().getCurrentConfig();
		final String smtpHost       = config.getProperty(Services.SMTP_HOST, "localhost");
		final String smtpPort       = config.getProperty(Services.SMTP_PORT, "25");
		final String smtpUser       = config.getProperty(Services.SMTP_USER);
		final String smtpPassword   = config.getProperty(Services.SMTP_PASSWORD);
		final String smtpUseTLS     = config.getProperty(Services.SMTP_USE_TLS, "true");
		final String smtpRequireTLS = config.getProperty(Services.SMTP_REQUIRE_TLS, "false");

		mail.setCharset(charset);
		mail.setHostName(smtpHost);
		mail.setSmtpPort(Integer.parseInt(smtpPort));
		mail.setStartTLSEnabled(Boolean.parseBoolean(smtpUseTLS));
		mail.setStartTLSRequired(Boolean.parseBoolean(smtpRequireTLS));
		mail.setCharset(charset);
		mail.setHostName(smtpHost);
		mail.setSmtpPort(Integer.parseInt(smtpPort));

		if (StringUtils.isNotBlank(smtpUser) && StringUtils.isNotBlank(smtpPassword)) {
			mail.setAuthentication(smtpUser, smtpPassword);
		}

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
