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
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.renderer.RegistrationFormRenderer;

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
	public final static String STATE_FIELD_NAME_KEY = "stateFieldName";
	public final static String COUNTRY_FIELD_NAME_KEY = "countryFieldName";
	public final static String BIRTHDAY_FIELD_NAME_KEY = "birthdayFieldName";
	public final static String GENDER_FIELD_NAME_KEY = "genderFieldName";
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
	 * Return name of state field
	 *
	 * @return
	 */
	public String getStateFieldName() {
		return getStringProperty(STATE_FIELD_NAME_KEY);
	}

	/**
	 * Set name of state field
	 *
	 * @param value
	 */
	public void setStateFieldName(final String value) {
		setProperty(STATE_FIELD_NAME_KEY, value);
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
	 * Return name of birthday field
	 *
	 * @return
	 */
	public String getBirthdayFieldName() {
		return getStringProperty(BIRTHDAY_FIELD_NAME_KEY);
	}

	/**
	 * Set name of birthday field
	 *
	 * @param value
	 */
	public void setBirthdayFieldName(final String value) {
		setProperty(BIRTHDAY_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of gender field
	 *
	 * @return
	 */
	public String getGenderFieldName() {
		return getStringProperty(GENDER_FIELD_NAME_KEY);
	}

	/**
	 * Set name of gender field
	 *
	 * @param value
	 */
	public void setGenderFieldName(final String value) {
		setProperty(GENDER_FIELD_NAME_KEY, value);
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

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {
		renderers.put(RenderMode.Default, new RegistrationFormRenderer());
	}
}
