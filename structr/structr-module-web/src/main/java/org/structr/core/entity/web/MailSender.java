/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.structr.common.MailHelper;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Send e-mails containing form data.
 *
 * Parameter names have to be registered in order for their values to be used in
 * subject and body templates.
 *
 * The parameter values can be addressed using placeholders like e.g. ${param}
 * for the value of the parameter named 'param'.
 *
 * At the moment, parameter validation has to be made client-side.
 *
 * TODO: Validate parameter server-side
 *
 * @author axel
 */
public class MailSender extends WebNode {

    private static final Logger logger = Logger.getLogger(MailSender.class.getName());
    private final static String ICON_SRC = "/images/email_go.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }
    private final static String KEY_PREFIX = "${";
    private final static String KEY_SUFFIX = "}";
    protected final static String defaultSubmitButtonName = LoginForm.defaultSubmitButtonName;
    protected final static String defaultAntiRobotFieldName = LoginForm.defaultAntiRobotFieldName;
    /** List with parameter names */
    public final static String PARAMETER_NAMES_KEY = "parameterNames";
    /** List with mandatory parameter names */
    public final static String MANDATORY_PARAMETER_NAMES_KEY = "mandatoryParameterNames";
    /** List with strings to be removed from parameter values */
    public final static String STRIP_FROM_VALUES_KEY = "stripFromValues";
    /** Name of submit button */
    public final static String SUBMIT_BUTTON_NAME_KEY = "submitButtonName";
    /** Name of hidden anti-robot field */
    public final static String ANTI_ROBOT_FIELD_NAME_KEY = "antiRobotFieldName";
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
     * Get parameter names
     *
     * @return
     */
    public String getParameterNames() {
        return getStringArrayPropertyAsString(PARAMETER_NAMES_KEY);
    }

    private List<String> getParameterNamesAsList() {
        return getStringListProperty(PARAMETER_NAMES_KEY);
    }

    /**
     * Set parameter names
     *
     * @param stringList
     */
    public void setParameterNames(final String value) {
        setPropertyAsStringArray(PARAMETER_NAMES_KEY, value);
    }

    /**
     * Get mandatory parameter names
     *
     * @return
     */
    public String getMandatoryParameterNames() {
        return getStringArrayPropertyAsString(MANDATORY_PARAMETER_NAMES_KEY);
    }

    private List<String> getMandatoryParameterNamesAsList() {
        return getStringListProperty(MANDATORY_PARAMETER_NAMES_KEY);
    }

    /**
     * Set mandatory parameter names
     *
     * @param stringList
     */
    public void setMandatoryParameterNames(final String value) {
        setPropertyAsStringArray(MANDATORY_PARAMETER_NAMES_KEY, value);
    }

    /**
     * Get strings to be stripped from all parameter values
     *
     * @return
     */
    public String getStripFromValues() {
        return getStringArrayPropertyAsString(STRIP_FROM_VALUES_KEY);
    }

    private List<String> getStripFromValuesAsList() {
        return getStringListProperty(STRIP_FROM_VALUES_KEY);
    }

    /**
     * Set strings to be stripped from all parameter values
     *
     * @param stringList
     */
    public void setStripFromValues(final String value) {
        setPropertyAsStringArray(STRIP_FROM_VALUES_KEY, value);
    }

    /**
     * Return name of anti robot field
     *
     * @return
     */
    public String getAntiRobotFieldName() {
        return getStringProperty(ANTI_ROBOT_FIELD_NAME_KEY);
    }

    /**
     * Set name of anti robot field
     *
     * @param value
     */
    public void setAntiRobotFieldName(final String value) {
        setProperty(ANTI_ROBOT_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of submit button
     *
     * @return
     */
    public String getSubmitButtonName() {
        return getStringProperty(SUBMIT_BUTTON_NAME_KEY);
    }

    /**
     * Set name of submit button
     *
     * @param value
     */
    public void setSubmitButtonName(final String value) {
        setProperty(SUBMIT_BUTTON_NAME_KEY, value);
    }

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

    /**
     * Render view
     *
     * @param out
     * @param startNode
     * @param editUrl
     * @param editNodeId
     */
    @Override
    public void renderView(StringBuilder out, final AbstractNode startNode,
            final String editUrl, final Long editNodeId, final User user) {

        // if this page is requested to be edited, render edit frame
        if (editNodeId != null && getId() == editNodeId.longValue()) {

            renderEditFrame(out, editUrl);

        } else {

            HttpServletRequest request = getRequest();

            if (request == null) {
                return;
            }

            HttpSession session = request.getSession();

            if (session == null) {
                return;
            }

            List<String> parameterNames = getParameterNamesAsList();
            Map parameterMap = new HashMap<String, String>();

            if (parameterNames != null) {
                for (String parameterName : parameterNames) {

                    String parameterValue = request.getParameter(parameterName);

                    // Clean values and add to parameter map
                    parameterMap.put(parameterName, clean(parameterValue));

                }

            }

            // Get values from config page, or defaults
            String submitButtonName = getSubmitButtonName() != null ? getSubmitButtonName() : defaultSubmitButtonName;
            String antiRobotFieldName = getAntiRobotFieldName() != null ? getAntiRobotFieldName() : defaultAntiRobotFieldName;

            String submitButton = request.getParameter(submitButtonName);
            String antiRobot = request.getParameter(antiRobotFieldName);

            if (StringUtils.isEmpty(submitButton)) {
                // Don't process form at all if submit button was not pressed
                return;
            }

            if (StringUtils.isNotEmpty(antiRobot)) {
                // Don't process form if someone has filled the anti-robot field
                return;
            }

            // Check mandatory parameters

            StringBuilder errorMsg = new StringBuilder();
            StringBuilder errorStyle = new StringBuilder();

            errorStyle.append("<style type=\"text/css\">");


            List<String> mandatoryParameterNames = getMandatoryParameterNamesAsList();

            if (mandatoryParameterNames != null) {

                for (String mandatoryParameterName : mandatoryParameterNames) {
                    if (StringUtils.isEmpty(request.getParameter(mandatoryParameterName))) {
                        errorMsg.append("<div class=\"errorMsg\">").append("Please fill out \"").append("<script type=\"text/javascript\">document.write(getLabel('").append(mandatoryParameterName).append("'));</script>\"").append("</div>");
                        errorStyle.append("input[name=").append(mandatoryParameterName).append("] { background-color: #ffc }\n");
                    }
                }
            }
            errorStyle.append("</style>");

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

    public String clean(final String input) {

        String output = StringUtils.trimToEmpty(input);

        for (String strip : getStripFromValuesAsList()) {
            output = StringUtils.replace(output, strip, "");
        }

        return output;
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
}
