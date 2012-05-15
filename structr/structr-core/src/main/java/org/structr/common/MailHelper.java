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

import java.util.logging.Logger;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.structr.core.Services;

/**
 *
 * @author axel
 */
public abstract class MailHelper {

    private static final Logger logger = Logger.getLogger(MailHelper.class.getName());

    public MailHelper() {
    }

    public static void sendHtmlMail(final String from, final String fromName, final String to, final String toName, final String cc, final String bcc, final String bounce, final String subject, final String htmlContent, final String textContent)
    throws EmailException {

        HtmlEmail mail = new HtmlEmail();

        mail.setCharset("utf-8");
        mail.setHostName(Services.getSmtpHost());
        mail.setSmtpPort(Integer.parseInt(Services.getSmtpPort()));

        mail.addTo(to, toName);
        mail.setFrom(from, fromName);
        mail.setBounceAddress(bounce);

        mail.setSubject(subject);

        mail.setHtmlMsg(htmlContent);
        mail.setTextMsg(textContent);

        mail.send();

    }
}
