/*
 *  Copyright (C) 2010-2012 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;

import org.structr.core.Services;

//~--- JDK imports ------------------------------------------------------------


//~--- classes ----------------------------------------------------------------

/**
 * Helper class for sending simple or HTML emails.
 * 
 * @author Axel Morgner
 */
public abstract class MailHelper {

	private static final String charset      = "UTF-8";
	private static final String smtpHost     = Services.getSmtpHost();
	private static final String smtpPort     = Services.getSmtpPort();
	private static final String smtpUser     = Services.getSmtpUser();
	private static final String smtpPassword = Services.getSmtpPassword();

	//~--- constructors ---------------------------------------------------

	public MailHelper() {}

	//~--- methods --------------------------------------------------------

	public static void sendHtmlMail(final String from, final String fromName, final String to, final String toName, final String cc, final String bcc, final String bounce, final String subject,
					final String htmlContent, final String textContent)
		throws EmailException {

		HtmlEmail mail = new HtmlEmail();

		setup(mail, to, toName, from, fromName, bcc, bounce, subject);
		mail.setHtmlMsg(htmlContent);
		mail.setTextMsg(textContent);
		mail.send();

	}

	public static void sendSimpleMail(final String from, final String fromName, final String to, final String toName, final String cc, final String bcc, final String bounce, final String subject,
					  final String textContent)
		throws EmailException {

		SimpleEmail mail = new SimpleEmail();

		setup(mail, to, toName, from, fromName, bcc, bounce, subject);
		mail.setMsg(textContent);
		mail.send();

	}

	private static void setup(final Email mail, final String to, final String toName, final String from, final String fromName, final String bcc, final String bounce, final String subject)
		throws EmailException {

		mail.setCharset(charset);
		mail.setHostName(smtpHost);
		mail.setSmtpPort(Integer.parseInt(smtpPort));
		mail.setTLS(true);
		mail.setCharset(charset);
		mail.setHostName(smtpHost);
		mail.setSmtpPort(Integer.parseInt(smtpPort));
		
		if (StringUtils.isNotBlank(smtpUser) && StringUtils.isNotBlank(smtpPassword)) {
			mail.setAuthentication(smtpUser, smtpPassword);
		}
		
		mail.addTo(to, toName);
		mail.setFrom(from, fromName);
		
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
	 * @return 
	 */
	public static String replacePlaceHoldersInTemplate(final String template, final Map<String, String> replacementMap) {
		
		List<String> toReplace = new ArrayList<String>();
		List<String> replaceBy = new ArrayList<String>();
		
		for (Entry<String, String> property : replacementMap.entrySet()) {
			
			toReplace.add(property.getKey());
			replaceBy.add(property.getValue());
			
		}
		
		return StringUtils.replaceEachRepeatedly(template, toReplace.toArray(new String[toReplace.size()]), replaceBy.toArray(new String[replaceBy.size()]));
		
	}

}
