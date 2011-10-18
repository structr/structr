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

import org.structr.common.PropertyKey;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.renderer.NewsletterFormRenderer;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

//~--- classes ----------------------------------------------------------------

/**
 * Render a form for newsletter submission.
 *
 * @author axel
 */
public class NewsletterForm extends Form {

	private final static String ICON_SRC                             = "/images/form.png";
	protected final static String defaultAgreedToTermsOfUseFieldName = "newsletterForm_agreedToTermsOfUse";
	protected final static String defaultAssignedUsername            = "admin";
	protected final static String defaultCityFieldName               = "newsletterForm_city";
	protected final static String defaultConfirmEmailFieldName       = "newsletterForm_confirmEmail";
	protected final static String defaultConfirmPasswordFieldName    = "newsletterForm_confirmPassword";
	protected final static String defaultConfirmationKeyFieldName    = "confirmationKey";
	protected final static String defaultCountryFieldName            = "newsletterForm_country";
	protected final static String defaultEmailFieldName              = "newsletterForm_email";
	protected final static String defaultFirstNameFieldName          = "newsletterForm_firstName";
	protected final static String defaultInlineCss                   =
		"<style type=\"text/css\">body { font-family: sans-serif; font-size: 12px; }</style>";
	protected final static String defaultLastNameFieldName       = "newsletterForm_lastName";
	protected final static String defaultNewsletterFieldName     = "newsletterForm_newsletter";
	protected final static String defaultPasswordFieldName       = "newsletterForm_password";
	protected final static String defaultPublicUserDirectoryName = "Public Users";
	protected final static String defaultSenderAddress           = "registration@structr.org";
	protected final static String defaultSenderName              = "structr Registration Robot";
	protected final static String defaultStreetFieldName         = "newsletterForm_street";
	protected final static String defaultUsernameFieldName       = "newsletterForm_username";
	protected final static String defaultZipCodeFieldName        = "newsletterForm_zipCode";

	//~--- constant enums -------------------------------------------------

	public enum Key implements PropertyKey {

		publicUserDirectoryName, senderAddress, senderName, inlineCss, assignedUsername,
		confirmationKeyFieldName, confirmPasswordFieldName, agreedToTermsOfUseFieldName, newsletterFieldName,
		firstNameFieldName, lastNameFieldName, emailFieldName, confirmEmailFieldName, zipCodeFieldName,
		cityFieldName, streetFieldName, countryFieldName, usernameFieldName, passwordFieldName;
	}

	//~--- methods --------------------------------------------------------

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {

		renderers.put(RenderMode.Default,
			      new NewsletterFormRenderer());
	}

	//~--- get methods ----------------------------------------------------

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
		return getStringProperty(Key.usernameFieldName.name());
	}

	/**
	 * Return name of password field
	 *
	 * @return
	 */
	public String getPasswordFieldName() {
		return getStringProperty(Key.passwordFieldName.name());
	}

	/**
	 * Return name of confirm password field
	 *
	 * @return
	 */
	public String getConfirmPasswordFieldName() {
		return getStringProperty(Key.confirmPasswordFieldName.name());
	}

	/**
	 * Return name of confirmation key field
	 *
	 * @return
	 */
	public String getConfirmationKeyFieldName() {
		return getStringProperty(Key.confirmationKeyFieldName.name());
	}

	/**
	 * Return name of first name field
	 *
	 * @return
	 */
	public String getFirstNameFieldName() {
		return getStringProperty(Key.firstNameFieldName.name());
	}

	/**
	 * Return name of last name field
	 *
	 * @return
	 */
	public String getLastNameFieldName() {
		return getStringProperty(Key.lastNameFieldName.name());
	}

	/**
	 * Return name of email field
	 *
	 * @return
	 */
	public String getEmailFieldName() {
		return getStringProperty(Key.emailFieldName.name());
	}

	/**
	 * Return name of confirm email field
	 *
	 * @return
	 */
	public String getConfirmEmailFieldName() {
		return getStringProperty(Key.confirmEmailFieldName.name());
	}

	/**
	 * Return name of zip code field
	 *
	 * @return
	 */
	public String getZipCodeFieldName() {
		return getStringProperty(Key.zipCodeFieldName.name());
	}

	/**
	 * Return name of city field
	 *
	 * @return
	 */
	public String getCityFieldName() {
		return getStringProperty(Key.cityFieldName.name());
	}

	/**
	 * Return name of street field
	 *
	 * @return
	 */
	public String getStreetFieldName() {
		return getStringProperty(Key.streetFieldName.name());
	}

	/**
	 * Return name of country field
	 *
	 * @return
	 */
	public String getCountryFieldName() {
		return getStringProperty(Key.countryFieldName.name());
	}

	/**
	 * Return name of agreed to terms of use field
	 *
	 * @return
	 */
	public String getAgreedToTermsOfUseFieldName() {
		return getStringProperty(Key.agreedToTermsOfUseFieldName.name());
	}

	/**
	 * Return name of newsletter field
	 *
	 * @return
	 */
	public String getNewsletterFieldName() {
		return getStringProperty(Key.newsletterFieldName.name());
	}

	/**
	 * Return name of sender address field
	 *
	 * @return
	 */
	public String getSenderAddress() {
		return getStringProperty(Key.senderAddress.name());
	}

	/**
	 * Return name of sender name field
	 *
	 * @return
	 */
	public String getSenderName() {
		return getStringProperty(Key.senderName.name());
	}

	/**
	 * Return name of public user directory
	 *
	 * @return
	 */
	public String getPublicUserDirectoryName() {
		return getStringProperty(Key.publicUserDirectoryName.name());
	}

	/**
	 * Return inline CSS
	 *
	 * @return
	 */
	public String getInlineCss() {
		return getStringProperty(Key.inlineCss.name());
	}

	/**
	 * Return assigned username
	 *
	 * @return
	 */
	public String getAssignedUsername() {
		return getStringProperty(Key.assignedUsername.name());
	}

	//~--- set methods ----------------------------------------------------

	/**
	 * Set name of username field
	 *
	 * @param value
	 */
	public void setUsernameFieldName(final String value) {

		setProperty(Key.usernameFieldName.name(),
			    value);
	}

	/**
	 * Set name of password field
	 *
	 * @param value
	 */
	public void setPasswordFieldName(final String value) {

		setProperty(Key.passwordFieldName.name(),
			    value);
	}

	/**
	 * Set name of confirm password field
	 *
	 * @param value
	 */
	public void setConfirmPasswordFieldName(final String value) {

		setProperty(Key.confirmPasswordFieldName.name(),
			    value);
	}

	/**
	 * Set name of confirmation key field
	 *
	 * @param value
	 */
	public void setConfirmationKeyFieldName(final String value) {

		setProperty(Key.confirmationKeyFieldName.name(),
			    value);
	}

	/**
	 * Set name of username field
	 *
	 * @param value
	 */
	public void setFirstNameFieldName(final String value) {

		setProperty(Key.firstNameFieldName.name(),
			    value);
	}

	/**
	 * Set name of last name field
	 *
	 * @param value
	 */
	public void setLastNameFieldName(final String value) {

		setProperty(Key.lastNameFieldName.name(),
			    value);
	}

	/**
	 * Set name of email field
	 *
	 * @param value
	 */
	public void setEmailFieldName(final String value) {

		setProperty(Key.emailFieldName.name(),
			    value);
	}

	/**
	 * Set name of confirm email field
	 *
	 * @param value
	 */
	public void setConfirmEmailFieldName(final String value) {

		setProperty(Key.confirmEmailFieldName.name(),
			    value);
	}

	/**
	 * Set name of zip code field
	 *
	 * @param value
	 */
	public void setZipCodeFieldName(final String value) {

		setProperty(Key.zipCodeFieldName.name(),
			    value);
	}

	/**
	 * Set name of city field
	 *
	 * @param value
	 */
	public void setCityFieldName(final String value) {

		setProperty(Key.cityFieldName.name(),
			    value);
	}

	/**
	 * Set name of street field
	 *
	 * @param value
	 */
	public void setStreetFieldName(final String value) {

		setProperty(Key.streetFieldName.name(),
			    value);
	}

	/**
	 * Set name of country field
	 *
	 * @param value
	 */
	public void setCountryFieldName(final String value) {

		setProperty(Key.countryFieldName.name(),
			    value);
	}

	/**
	 * Set name of agreed to terms of use field
	 *
	 * @param value
	 */
	public void setAgreedToTermsOfUseFieldName(final String value) {

		setProperty(Key.agreedToTermsOfUseFieldName.name(),
			    value);
	}

	/**
	 * Set name of newsletter field
	 *
	 * @param value
	 */
	public void setNewsletterFieldName(final String value) {

		setProperty(Key.newsletterFieldName.name(),
			    value);
	}

	/**
	 * Set name of sender address field
	 *
	 * @param value
	 */
	public void setSenderAddress(final String value) {

		setProperty(Key.senderAddress.name(),
			    value);
	}

	/**
	 * Set name of sender name field
	 *
	 * @param value
	 */
	public void setSenderName(final String value) {

		setProperty(Key.senderName.name(),
			    value);
	}

	/**
	 * Set name of public user directory
	 *
	 * @param value
	 */
	public void setPublicUserDirectoryName(final String value) {

		setProperty(Key.publicUserDirectoryName.name(),
			    value);
	}

	/**
	 * Set inline CSS
	 *
	 * @param value
	 */
	public void setInlineCss(final String value) {

		setProperty(Key.inlineCss.name(),
			    value);
	}

	/**
	 * Set assigned username
	 *
	 * @param value
	 */
	public void setAssignedUsername(final String value) {

		setProperty(Key.assignedUsername.name(),
			    value);
	}
}
