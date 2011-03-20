/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
