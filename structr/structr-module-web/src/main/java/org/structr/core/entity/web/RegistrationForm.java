/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.structr.common.SessionContext;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Render a login form.
 *
 * Username and password field names have to match with a corresponding
 * LoginCheck node to make login work.
 *
 * @author axel
 */
public class RegistrationForm extends Form {

    private final static String ICON_SRC = "/images/form.png";
    protected final static String defaultUsernameFieldName = "regForm_username";
    protected final static String defaultPasswordFieldName = "regForm_password";
    protected final static String defaultAssignedUsername = "admin";
    protected final static String defaultInlineCss = "<style type=\"text/css\">body { font-family: sans-serif; font-size: 12px; }</style>";
    protected final static String defaultPublicUserDirectoryName = "Public Users";
    protected final static String defaultSenderAddress = "registration@structr.org";
    protected final static String defaultSenderName = "structr Registration Robot";
    protected final static String defaultConfirmationKeyFieldName = "confirmationKey";
    protected final static String defaultConfirmPasswordFieldName = "regForm_confirmPassword";
    protected final static String defaultFirstNameFieldName = "regForm_firstName";
    protected final static String defaultLastNameFieldName = "regForm_lastName";
    protected final static String defaultEmailFieldName = "regForm_email";
    protected final static String defaultConfirmEmailFieldName = "regForm_confirmEmail";
    protected final static String defaultZipCodeFieldName = "regForm_zipCode";
    protected final static String defaultCityFieldName = "regForm_city";
    protected final static String defaultStreetFieldName = "regForm_street";
    protected final static String defaultCountryFieldName = "regForm_country";
    protected final static String defaultAgreedToTermsOfUseFieldName = "regForm_agreedToTermsOfUse";
    protected final static String defaultNewsletterFieldName = "regForm_newsletter";
    public final static String PUBLIC_USER_DIRECTORY_NAME_KEY = "publicUserDirectoryName";
    public final static String SENDER_ADDRESS_KEY = "senderAddress";
    public final static String SENDER_NAME_KEY = "senderName";
    public final static String INLINE_CSS_KEY = "inlineCss";
    public final static String ASSIGNED_USERNAME_KEY = "assignedUsername";
    public final static String CONFIRMATION_KEY_FIELD_NAME_KEY = "confirmationKeyFieldName";
    public final static String CONFIRM_PASSWORD_FIELD_NAME_KEY = "confirmPasswordFieldName";
    public final static String FIRST_NAME_FIELD_NAME_KEY = "firstNameFieldName";
    public final static String LAST_NAME_FIELD_NAME_KEY = "lastNameFieldName";
    public final static String EMAIL_FIELD_NAME_KEY = "emailFieldName";
    public final static String CONFIRM_EMAIL_FIELD_NAME_KEY = "confirmEmailFieldName";
    public final static String ZIP_CODE_FIELD_NAME_KEY = "zipCodeFieldName";
    public final static String CITY_FIELD_NAME_KEY = "cityFieldName";
    public final static String STREET_FIELD_NAME_KEY = "streetFieldName";
    public final static String COUNTRY_FIELD_NAME_KEY = "countryFieldName";
    public final static String AGREED_TO_TERMS_OF_USE_FIELD_NAME_KEY = "agreedToTermsOfUseFieldName";
    public final static String NEWSLETTER_FIELD_NAME_KEY = "newsletterFieldName";
    /** Name of username field */
    public final static String USERNAME_FIELD_NAME_KEY = "usernameFieldName";
    /** Name of password field */
    public final static String PASSWORD_FIELD_NAME_KEY = "passwordFieldName";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Return name of username field
     *
     * @return
     */
    public String getUsernameFieldName() {
        return getStringProperty(USERNAME_FIELD_NAME_KEY);
    }

    /**
     * Return name of password field
     *
     * @return
     */
    public String getPasswordFieldName() {
        return getStringProperty(PASSWORD_FIELD_NAME_KEY);
    }

    /**
     * Return name of confirm password field
     *
     * @return
     */
    public String getConfirmPasswordFieldName() {
        return getStringProperty(CONFIRM_PASSWORD_FIELD_NAME_KEY);
    }

    /**
     * Set name of username field
     *
     * @param value
     */
    public void setUsernameFieldName(final String value) {
        setProperty(USERNAME_FIELD_NAME_KEY, value);
    }

    /**
     * Set name of password field
     *
     * @param value
     */
    public void setPasswordFieldName(final String value) {
        setProperty(USERNAME_FIELD_NAME_KEY, value);
    }

    /**
     * Set name of confirm password field
     *
     * @param value
     */
    public void setConfirmPasswordFieldName(final String value) {
        setProperty(CONFIRM_PASSWORD_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of confirmation key field
     *
     * @return
     */
    public String getConfirmationKeyFieldName() {
        return getStringProperty(CONFIRMATION_KEY_FIELD_NAME_KEY);
    }

    /**
     * Set name of confirmation key field
     *
     * @param value
     */
    public void setConfirmationKeyFieldName(final String value) {
        setProperty(CONFIRMATION_KEY_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of first name field
     *
     * @return
     */
    public String getFirstNameFieldName() {
        return getStringProperty(FIRST_NAME_FIELD_NAME_KEY);
    }

    /**
     * Set name of username field
     *
     * @param value
     */
    public void setFirstNameFieldName(final String value) {
        setProperty(FIRST_NAME_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of last name field
     *
     * @return
     */
    public String getLastNameFieldName() {
        return getStringProperty(LAST_NAME_FIELD_NAME_KEY);
    }

    /**
     * Set name of last name field
     *
     * @param value
     */
    public void setLastNameFieldName(final String value) {
        setProperty(LAST_NAME_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of email field
     *
     * @return
     */
    public String getEmailFieldName() {
        return getStringProperty(EMAIL_FIELD_NAME_KEY);
    }

    /**
     * Set name of email field
     *
     * @param value
     */
    public void setEmailFieldName(final String value) {
        setProperty(EMAIL_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of confirm email field
     *
     * @return
     */
    public String getConfirmEmailFieldName() {
        return getStringProperty(CONFIRM_EMAIL_FIELD_NAME_KEY);
    }

    /**
     * Set name of confirm email field
     *
     * @param value
     */
    public void setConfirmEmailFieldName(final String value) {
        setProperty(CONFIRM_EMAIL_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of zip code field
     *
     * @return
     */
    public String getZipCodeFieldName() {
        return getStringProperty(ZIP_CODE_FIELD_NAME_KEY);
    }

    /**
     * Set name of zip code field
     *
     * @param value
     */
    public void setZipCodeFieldName(final String value) {
        setProperty(ZIP_CODE_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of city field
     *
     * @return
     */
    public String getCityFieldName() {
        return getStringProperty(CITY_FIELD_NAME_KEY);
    }

    /**
     * Set name of city field
     *
     * @param value
     */
    public void setCityFieldName(final String value) {
        setProperty(CITY_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of street field
     *
     * @return
     */
    public String getStreetFieldName() {
        return getStringProperty(STREET_FIELD_NAME_KEY);
    }

    /**
     * Set name of street field
     *
     * @param value
     */
    public void setStreetFieldName(final String value) {
        setProperty(STREET_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of country field
     *
     * @return
     */
    public String getCountryFieldName() {
        return getStringProperty(COUNTRY_FIELD_NAME_KEY);
    }

    /**
     * Set name of country field
     *
     * @param value
     */
    public void setCountryFieldName(final String value) {
        setProperty(COUNTRY_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of agreed to terms of use field
     *
     * @return
     */
    public String getAgreedToTermsOfUseFieldName() {
        return getStringProperty(AGREED_TO_TERMS_OF_USE_FIELD_NAME_KEY);
    }

    /**
     * Set name of agreed to terms of use field
     *
     * @param value
     */
    public void setAgreedToTermsOfUseFieldName(final String value) {
        setProperty(AGREED_TO_TERMS_OF_USE_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of newsletter field
     *
     * @return
     */
    public String getNewsletterFieldName() {
        return getStringProperty(NEWSLETTER_FIELD_NAME_KEY);
    }

    /**
     * Set name of newsletter field
     *
     * @param value
     */
    public void setNewsletterFieldName(final String value) {
        setProperty(NEWSLETTER_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of sender address field
     *
     * @return
     */
    public String getSenderAddress() {
        return getStringProperty(SENDER_ADDRESS_KEY);
    }

    /**
     * Set name of sender address field
     *
     * @param value
     */
    public void setSenderAddress(final String value) {
        setProperty(SENDER_ADDRESS_KEY, value);
    }

    /**
     * Return name of sender name field
     *
     * @return
     */
    public String getSenderName() {
        return getStringProperty(SENDER_NAME_KEY);
    }

    /**
     * Set name of sender name field
     *
     * @param value
     */
    public void setSenderName(final String value) {
        setProperty(SENDER_NAME_KEY, value);
    }

    /**
     * Return name of public user directory
     *
     * @return
     */
    public String getPublicUserDirectoryName() {
        return getStringProperty(PUBLIC_USER_DIRECTORY_NAME_KEY);
    }

    /**
     * Set name of public user directory
     *
     * @param value
     */
    public void setPublicUserDirectoryName(final String value) {
        setProperty(PUBLIC_USER_DIRECTORY_NAME_KEY, value);
    }

    /**
     * Return inline CSS
     *
     * @return
     */
    public String getInlineCss() {
        return getStringProperty(INLINE_CSS_KEY);
    }

    /**
     * Set inline CSS
     *
     * @param value
     */
    public void setInlineCss(final String value) {
        setProperty(INLINE_CSS_KEY, value);
    }

    /**
     * Return assigned username
     *
     * @return
     */
    public String getAssignedUsername() {
        return getStringProperty(ASSIGNED_USERNAME_KEY);
    }

    /**
     * Set assigned username
     *
     * @param value
     */
    public void setAssignedUsername(final String value) {
        setProperty(ASSIGNED_USERNAME_KEY, value);
    }

    /**
     * Render edit view
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

            HttpServletRequest request = SessionContext.getRequest();

            if (request == null) {
                return;
            }

            HttpSession session = request.getSession();

            if (session == null) {
                return;
            }

            String usernameFromSession = (String) session.getAttribute(USERNAME_KEY);
            Boolean alreadyLoggedIn = usernameFromSession != null;

            if (alreadyLoggedIn) {
                //out.append("<div class=\"okMsg\">").append("Your are logged in as ").append(usernameFromSession).append(".</div>");
                return;
            }

            Boolean sessionBlocked = (Boolean) session.getAttribute(SESSION_BLOCKED);

            if (Boolean.TRUE.equals(sessionBlocked)) {
                out.append("<div class=\"errorMsg\">").append("Too many login attempts, session is blocked for login").append("</div>");
                return;
            }

            String confirmationKeyFieldName = getConfirmationKeyFieldName() != null ? getConfirmationKeyFieldName() : defaultConfirmationKeyFieldName;
            String confirmationKey = StringUtils.trimToEmpty(request.getParameter(confirmationKeyFieldName));

            if (StringUtils.isNotEmpty(confirmationKey)) {
                // Don't show form if confirmation key is set
                return;
            }

            // Get values from config page, or defaults
            String action = getAction() != null ? getAction() : defaultAction;
            String submitButtonName = getSubmitButtonName() != null ? getSubmitButtonName() : defaultSubmitButtonName;
            String antiRobotFieldName = getAntiRobotFieldName() != null ? getAntiRobotFieldName() : defaultAntiRobotFieldName;
            String usernameFieldName = getUsernameFieldName() != null ? getUsernameFieldName() : defaultUsernameFieldName;
            String passwordFieldName = getPasswordFieldName() != null ? getPasswordFieldName() : defaultPasswordFieldName;
            String firstNameFieldName = getFirstNameFieldName() != null ? getFirstNameFieldName() : defaultFirstNameFieldName;
            String lastNameFieldName = getLastNameFieldName() != null ? getLastNameFieldName() : defaultLastNameFieldName;
            String emailFieldName = getEmailFieldName() != null ? getEmailFieldName() : defaultEmailFieldName;
            String streetFieldName = getStreetFieldName() != null ? getStreetFieldName() : defaultStreetFieldName;
            String zipCodeFieldName = getZipCodeFieldName() != null ? getZipCodeFieldName() : defaultZipCodeFieldName;
            String cityFieldName = getCityFieldName() != null ? getCityFieldName() : defaultCityFieldName;
            String countryFieldName = getCountryFieldName() != null ? getCountryFieldName() : defaultCountryFieldName;
            String confirmEmailFieldName = getConfirmEmailFieldName() != null ? getConfirmEmailFieldName() : defaultConfirmEmailFieldName;
            String confirmPasswordFieldName = getConfirmPasswordFieldName() != null ? getConfirmPasswordFieldName() : defaultConfirmPasswordFieldName;
            String cssClass = getCssClass() != null ? getCssClass() : defaultCssClass;
            String label = getLabel() != null ? getLabel() : defaultLabel;
            String agreedToTermsOfUseFieldName = getAgreedToTermsOfUseFieldName() != null ? getAgreedToTermsOfUseFieldName() : defaultAgreedToTermsOfUseFieldName;
            String newsletterFieldName = getNewsletterFieldName() != null ? getNewsletterFieldName() : defaultNewsletterFieldName;

            String username = StringUtils.trimToEmpty(request.getParameter(usernameFieldName));
            String password = StringUtils.trimToEmpty(request.getParameter(passwordFieldName));
            String confirmPassword = StringUtils.trimToEmpty(request.getParameter(confirmPasswordFieldName));
            String firstName = StringUtils.trimToEmpty(request.getParameter(firstNameFieldName));
            String lastName = StringUtils.trimToEmpty(request.getParameter(lastNameFieldName));
            String email = StringUtils.trimToEmpty(request.getParameter(emailFieldName));
            String confirmEmail = StringUtils.trimToEmpty(request.getParameter(confirmEmailFieldName));
            String zipCode = StringUtils.trimToEmpty(request.getParameter(zipCodeFieldName));
            String street = StringUtils.trimToEmpty(request.getParameter(streetFieldName));
            String city = StringUtils.trimToEmpty(request.getParameter(cityFieldName));
            String country = StringUtils.trimToEmpty(request.getParameter(countryFieldName));
            String agreedToTermsOfUse = StringUtils.isNotEmpty(request.getParameter(agreedToTermsOfUseFieldName)) ? "checked=\"checked\"" : "";
            String newsletter = StringUtils.isNotEmpty(request.getParameter(newsletterFieldName)) ? "checked=\"checked\"" : "";


            out.append("<form name=\"").append(getName()).append("\" action=\"").append(action).append("\" method=\"post\">");
            out.append("<input type=\"hidden\" name=\"").append(antiRobotFieldName).append("\" value=\"\">");
            out.append("<table class=\"").append(cssClass).append("\">");
            out.append("<tr><th><span class=\"heading\">").append(label).append("</span></th><th></th></tr>");
            out.append("<tr><td class=\"label\">Username*</td><td></td></tr>");
            out.append("<tr><td class=\"field\"><input type=\"text\" name=\"").append(usernameFieldName).append("\" value=\"").append(username).append("\" size=\"30\"></td></tr>");
            out.append("<tr><td class=\"label\">Password*</td><td class=\"label\">Confirm Password*</td></tr>");
            out.append("<tr>");
            out.append("<td class=\"field\"><input type=\"password\" name=\"").append(passwordFieldName).append("\" value=\"").append(password).append("\" size=\"30\"></td>");
            out.append("<td class=\"field\"><input type=\"password\" name=\"").append(confirmPasswordFieldName).append("\" value=\"").append(confirmPassword).append("\" size=\"30\"></td>");
            out.append("</tr>");
            out.append("<tr><td class=\"label\">First Name*</td><td class=\"label\">Last Name*</td></tr>");
            out.append("<tr>");
            out.append("<td class=\"field\"><input type=\"text\" name=\"").append(firstNameFieldName).append("\" value=\"").append(firstName).append("\" size=\"30\"></td>");
            out.append("<td class=\"field\"><input type=\"text\" name=\"").append(lastNameFieldName).append("\" value=\"").append(lastName).append("\" size=\"30\"></td>");
            out.append("</tr>");
            out.append("<tr><td class=\"label\" colspan=\"2\">Email*</td></tr>");
            out.append("<tr><td class=\"field twoColumns\" colspan=\"2\"><input type=\"text\" name=\"").append(emailFieldName).append("\" value=\"").append(email).append("\" size=\"60\"></td></tr>");
            out.append("<tr><td class=\"label\" colspan=\"2\">Confirm Email*</td></tr>");
            out.append("<tr><td class=\"field twoColumns\" colspan=\"2\"><input type=\"text\" name=\"").append(confirmEmailFieldName).append("\" value=\"").append(confirmEmail).append("\" size=\"60\"></td></tr>");
            out.append("<tr><td class=\"label\" colspan=\"2\">Street</td></tr>");
            out.append("<tr><td class=\"field twoColumns\" colspan=\"2\"><input type=\"text\" name=\"").append(streetFieldName).append("\" value=\"").append(street).append("\" size=\"60\"></td></tr>");
            out.append("<tr><td class=\"label\">ZIP / Postal Code</td><td class=\"label\">City</td></tr>");
            out.append("<tr>");
            out.append("<td class=\"field\"><input type=\"text\" name=\"").append(zipCodeFieldName).append("\" value=\"").append(zipCode).append("\" size=\"30\"></td>");
            out.append("<td class=\"field\"><input type=\"text\" name=\"").append(cityFieldName).append("\" value=\"").append(city).append("\" size=\"30\"></td>");
            out.append("</tr>");
            out.append("<tr><td class=\"label\" colspan=\"2\">Country</td></tr>");
            out.append("<tr><td class=\"field twoColumns\" colspan=\"2\"><input type=\"text\" name=\"").append(countryFieldName).append("\" value=\"").append(country).append("\" size=\"60\"></td></tr>");
            out.append("<tr><td class=\"button\" colspan=\"2\"><input type=\"submit\" name=\"").append(submitButtonName).append("\" value=\"Submit\"></td></tr>");
            out.append("<tr><td class=\"field\" colspan=\"2\"><input type=\"checkbox\" name=\"").append(newsletterFieldName).append("\" ").append(newsletter).append(">I would like to receive a newsletter</td></tr>");
            out.append("<tr><td class=\"field\" colspan=\"2\"><input type=\"checkbox\" name=\"").append(agreedToTermsOfUseFieldName).append("\" ").append(agreedToTermsOfUse).append(">I agree to the terms of use</td></tr>");
            out.append("</table>");
            out.append("<p>* must be filled out.</p>");
            out.append("</form>");

        }
    }
}
