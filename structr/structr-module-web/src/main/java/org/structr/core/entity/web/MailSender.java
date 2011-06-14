/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
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
package org.structr.core.entity.web;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.structr.common.MailHelper;
import org.structr.common.StructrOutputStream;
import org.structr.core.entity.AbstractNode;

/**
 * Send e-mails containing form data.
 *
 * Parameter names have to be registered in order for their values to be used in
 * subject and body templates.
 *
 * The parameter values can be addressed using placeholders like e.g. ${param}
 * for the value of the parameter named 'param'.
 *
 * @author axel
 */
public class MailSender extends Form {

    /**
     * Render view
     *
     * @param out
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
    @Override
    public void renderNode(final StructrOutputStream out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId) {

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {
 
            readParameters();
            if (!validateParameters()) return;

            String to = replaceInContent(getToAddressTemplate(), parameterMap);
            String toName = replaceInContent(getToNameTemplate(), parameterMap);
            String from = replaceInContent(getFromAddressTemplate(), parameterMap);
            String fromName = replaceInContent(getFromNameTemplate(), parameterMap);
            String cc = replaceInContent(getCcAddressTemplate(), parameterMap);
            String bcc = replaceInContent(getBccAddressTemplate(), parameterMap);

            String subject = replaceInContent(getMailSubjectTemplate(), parameterMap);
            String htmlContent = replaceInContent(getHtmlBodyTemplate(), parameterMap);
            String textContent = null;
            if (StringUtils.isNotEmpty(htmlContent)) {
                textContent = Jsoup.parse(htmlContent).text();
            }

            // If no errors so far, try sending e-mail

            if (errorMsg.length() == 0) {

                // Send e-mail
                try {

                    MailHelper.sendHtmlMail(from, fromName, to, toName, cc, bcc, from, subject, htmlContent, textContent);

                    out.append("<div class=\"okMsg\">").append("An e-mail with your invitation was send to ").append(to).append("</div>");
                    out.append("<div class=\"htmlMessage\">").append(htmlContent).append("</div>");

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error while sending e-mail", e);
                    errorMsg.append("<div class=\"errorMsg\">").append("Error while sending e-mail: ").append(e.getMessage()).append("</div>");
                }
            }

            if (errorMsg.length() > 0) {
                out.append(errorMsg).append(errorStyle);
                return;
            }

        }

    }

    private String replaceInContent(final String template, final Map parameterMap) {

        if (template != null) {

            StringBuilder content = new StringBuilder(template);

            int start = content.indexOf(KEY_PREFIX);
            while (start > -1) {

                int end = content.indexOf(KEY_SUFFIX, start + KEY_PREFIX.length());

                if (end < 0) {
                    logger.log(Level.WARNING, "Key suffix {0} not found in template", new Object[]{KEY_SUFFIX});
                    break;
                }

                String key = content.substring(start + KEY_PREFIX.length(), end);

                StringBuilder replacement = new StringBuilder();

                replacement.append(parameterMap.get(key));


                String replaceBy = replacement.toString();

                content.replace(start, end
                        + KEY_SUFFIX.length(), replaceBy);
                // avoid replacing in the replacement again
                start = content.indexOf(KEY_PREFIX, start + replaceBy.length() + 1);
            }

            return content.toString();

        } else {
            return null;
        }
    }
    private static final Logger logger = Logger.getLogger(MailSender.class.getName());
    private final static String ICON_SRC = "/images/email_go.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    private final static String KEY_PREFIX = "${";
    private final static String KEY_SUFFIX = "}";
    /** Template for mail subject line */
    public final static String MAIL_SUBJECT_TEMPLATE_KEY = "mailSubjectTemplate";
    /** Template for mail body */
    public final static String HTML_BODY_TEMPLATE_KEY = "htmlBodyTemplate";
    /** Template for 'to address' */
    public final static String TO_ADDRESS_TEMPLATE_KEY = "toAddressTemplate";
    /** Template for 'from address' */
    public final static String FROM_ADDRESS_TEMPLATE_KEY = "fromAddressTemplate";
    /** Template for 'cc address' */
    public final static String CC_ADDRESS_TEMPLATE_KEY = "ccAddressTemplate";
    /** Template for 'bcc address' */
    public final static String BCC_ADDRESS_TEMPLATE_KEY = "bccAddressTemplate";
    /** Template for 'to name' */
    public final static String TO_NAME_TEMPLATE_KEY = "toNameTemplate";
    /** Template for 'from name' */
    public final static String FROM_NAME_TEMPLATE_KEY = "fromNameTemplate";

    /**
     * Get template for mail subject line
     *
     * @return
     */
    public String getMailSubjectTemplate() {
        return getStringProperty(MAIL_SUBJECT_TEMPLATE_KEY);
    }

    /**
     * Set template for mail subject line
     *
     * @param value
     */
    public void setMailSubjectTemplate(final String value) {
        setProperty(MAIL_SUBJECT_TEMPLATE_KEY, value);
    }

    /**
     * Get template for mail body
     *
     * @return
     */
    public String getHtmlBodyTemplate() {
        return getStringProperty(HTML_BODY_TEMPLATE_KEY);
    }

    /**
     * Set template for mail body
     *
     * @param value
     */
    public void setHtmlBodyTemplate(final String value) {
        setProperty(HTML_BODY_TEMPLATE_KEY, value);
    }

    /**
     * Get template for 'to address'
     *
     * @return
     */
    public String getToAddressTemplate() {
        return getStringProperty(TO_ADDRESS_TEMPLATE_KEY);
    }

    /**
     * Set template for 'to address'
     *
     * @param value
     */
    public void setToAddressTemplate(final String value) {
        setProperty(TO_ADDRESS_TEMPLATE_KEY, value);
    }

    /**
     * Get template for 'from address'
     *
     * @return
     */
    public String getFromAddressTemplate() {
        return getStringProperty(FROM_ADDRESS_TEMPLATE_KEY);
    }

    /**
     * Set template for 'from address'
     *
     * @param value
     */
    public void setFromAddressTemplate(final String value) {
        setProperty(FROM_ADDRESS_TEMPLATE_KEY, value);
    }

    /**
     * Get template for 'to name'
     *
     * @return
     */
    public String getToNameTemplate() {
        return getStringProperty(TO_NAME_TEMPLATE_KEY);
    }

    /**
     * Set template for 'to name'
     *
     * @param value
     */
    public void setToNameTemplate(final String value) {
        setProperty(TO_NAME_TEMPLATE_KEY, value);
    }

    /**
     * Get template for 'from name'
     *
     * @return
     */
    public String getFromNameTemplate() {
        return getStringProperty(FROM_NAME_TEMPLATE_KEY);
    }

    /**
     * Set template for 'to address'
     *
     * @param value
     */
    public void setFromNameTemplate(final String value) {
        setProperty(FROM_NAME_TEMPLATE_KEY, value);
    }

    /**
     * Get template for 'cc address'
     *
     * @return
     */
    public String getCcAddressTemplate() {
        return getStringProperty(CC_ADDRESS_TEMPLATE_KEY);
    }

    /**
     * Set template for 'cc address'
     *
     * @param value
     */
    public void setCcAddressTemplate(final String value) {
        setProperty(CC_ADDRESS_TEMPLATE_KEY, value);
    }

    /**
     * Get template for 'bcc address'
     *
     * @return
     */
    public String getBccAddressTemplate() {
        return getStringProperty(BCC_ADDRESS_TEMPLATE_KEY);
    }

    /**
     * Set template for 'bcc address'
     *
     * @param value
     */
    public void setBccAddressTemplate(final String value) {
        setProperty(BCC_ADDRESS_TEMPLATE_KEY, value);
    }
}
