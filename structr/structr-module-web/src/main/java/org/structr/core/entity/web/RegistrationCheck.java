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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.structr.common.RenderMode;
import org.structr.core.NodeRenderer;
import org.structr.renderer.RegistrationCheckRenderer;

/**
 * Checks registration information.
 *
 * <ul>
 * <li>All mandatory fields must be filled out
 * <li>Username must not exist
 * <li>Passwords must match
 * <li>Password must be strong enough
 * <li>Checkbox for agreement with terms of use has to be checked
 * </ul>
 *
 * If anything is ok, the user will be registered.
 *
 * If not, error messages are displayed.
 *
 * @author axel
 */
public class RegistrationCheck extends LoginCheck
{
	private final static String ICON_SRC = "/images/application_key.png";

	@Override
	public String getIconSrc()
	{
		return ICON_SRC;
	}
	public final static String PUBLIC_USER_DIRECTORY_NAME_KEY = "publicUserDirectoryName";
	public final static String SENDER_ADDRESS_KEY = "senderAddress";
	public final static String SENDER_NAME_KEY = "senderName";
	public final static String INLINE_CSS_KEY = "inlineCss";
	public final static String ASSIGNED_USERNAME_KEY = "assignedUsername";
	public final static String CONFIRMATION_KEY_FIELD_NAME_KEY = "confirmationKeyFieldName";
	public final static String CONFIRM_PASSWORD_FIELD_NAME_KEY = "confirmPasswordFieldName";
	public final static String AGREED_TO_TERMS_OF_USE_FIELD_NAME_KEY = "agreedToTermsOfUseFieldName";
	public final static String NEWSLETTER_FIELD_NAME_KEY = "newsletterFieldName";

	/**
	 * Return name of confirmation key field
	 *
	 * @return
	 */
	public String getConfirmationKeyFieldName()
	{
		return getStringProperty(CONFIRMATION_KEY_FIELD_NAME_KEY);
	}

	/**
	 * Set name of confirmation key field
	 *
	 * @param value
	 */
	public void setConfirmationKeyFieldName(final String value)
	{
		setProperty(CONFIRMATION_KEY_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of confirm password field
	 *
	 * @return
	 */
	public String getConfirmPasswordFieldName()
	{
		return getStringProperty(CONFIRM_PASSWORD_FIELD_NAME_KEY);
	}

	/**
	 * Set name of confirm password field
	 *
	 * @param value
	 */
	public void setConfirmPasswordFieldName(final String value)
	{
		setProperty(CONFIRM_PASSWORD_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of first name field
	 *
	 * @return
	 */
	public String getFirstNameFieldName()
	{
		return getStringProperty(RegistrationForm.FIRST_NAME_FIELD_NAME_KEY);
	}

	/**
	 * Set name of username field
	 *
	 * @param value
	 */
	public void setFirstNameFieldName(final String value)
	{
		setProperty(RegistrationForm.FIRST_NAME_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of last name field
	 *
	 * @return
	 */
	public String getLastNameFieldName()
	{
		return getStringProperty(RegistrationForm.LAST_NAME_FIELD_NAME_KEY);
	}

	/**
	 * Set name of last name field
	 *
	 * @param value
	 */
	public void setLastNameFieldName(final String value)
	{
		setProperty(RegistrationForm.LAST_NAME_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of email field
	 *
	 * @return
	 */
	public String getEmailFieldName()
	{
		return getStringProperty(RegistrationForm.EMAIL_FIELD_NAME_KEY);
	}

	/**
	 * Set name of email field
	 *
	 * @param value
	 */
	public void setEmailFieldName(final String value)
	{
		setProperty(RegistrationForm.EMAIL_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of confirm email field
	 *
	 * @return
	 */
	public String getConfirmEmailFieldName()
	{
		return getStringProperty(RegistrationForm.CONFIRM_EMAIL_FIELD_NAME_KEY);
	}

	/**
	 * Set name of confirm email field
	 *
	 * @param value
	 */
	public void setConfirmEmailFieldName(final String value)
	{
		setProperty(RegistrationForm.CONFIRM_EMAIL_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of zip code field
	 *
	 * @return
	 */
	public String getZipCodeFieldName()
	{
		return getStringProperty(RegistrationForm.ZIP_CODE_FIELD_NAME_KEY);
	}

	/**
	 * Set name of zip code field
	 *
	 * @param value
	 */
	public void setZipCodeFieldName(final String value)
	{
		setProperty(RegistrationForm.ZIP_CODE_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of city field
	 *
	 * @return
	 */
	public String getCityFieldName()
	{
		return getStringProperty(RegistrationForm.CITY_FIELD_NAME_KEY);
	}

	/**
	 * Set name of city field
	 *
	 * @param value
	 */
	public void setCityFieldName(final String value)
	{
		setProperty(RegistrationForm.CITY_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of street field
	 *
	 * @return
	 */
	public String getStreetFieldName()
	{
		return getStringProperty(RegistrationForm.STREET_FIELD_NAME_KEY);
	}

	/**
	 * Set name of street field
	 *
	 * @param value
	 */
	public void setStreetFieldName(final String value)
	{
		setProperty(RegistrationForm.STREET_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of state field
	 *
	 * @return
	 */
	public String getStateFieldName()
	{
		return getStringProperty(RegistrationForm.STATE_FIELD_NAME_KEY);
	}

	/**
	 * Set name of state field
	 *
	 * @param value
	 */
	public void setStateFieldName(final String value)
	{
		setProperty(RegistrationForm.STATE_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of country field
	 *
	 * @return
	 */
	public String getCountryFieldName()
	{
		return getStringProperty(RegistrationForm.COUNTRY_FIELD_NAME_KEY);
	}

	/**
	 * Set name of country field
	 *
	 * @param value
	 */
	public void setCountryFieldName(final String value)
	{
		setProperty(RegistrationForm.COUNTRY_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of birthday field
	 *
	 * @return
	 */
	public String getBirthdayFieldName()
	{
		return getStringProperty(RegistrationForm.BIRTHDAY_FIELD_NAME_KEY);
	}

	/**
	 * Set name of birthday field
	 *
	 * @param value
	 */
	public void setBirthdayFieldName(final String value)
	{
		setProperty(RegistrationForm.BIRTHDAY_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of gender field
	 *
	 * @return
	 */
	public String getGenderFieldName()
	{
		return getStringProperty(RegistrationForm.GENDER_FIELD_NAME_KEY);
	}

	/**
	 * Set name of gender field
	 *
	 * @param value
	 */
	public void setGenderFieldName(final String value)
	{
		setProperty(RegistrationForm.GENDER_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of agreed to terms of use field
	 *
	 * @return
	 */
	public String getAgreedToTermsOfUseFieldName()
	{
		return getStringProperty(AGREED_TO_TERMS_OF_USE_FIELD_NAME_KEY);
	}

	/**
	 * Set name of agreed to terms of use field
	 *
	 * @param value
	 */
	public void setAgreedToTermsOfUseFieldName(final String value)
	{
		setProperty(AGREED_TO_TERMS_OF_USE_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of newsletter field
	 *
	 * @return
	 */
	public String getNewsletterFieldName()
	{
		return getStringProperty(NEWSLETTER_FIELD_NAME_KEY);
	}

	/**
	 * Set name of newsletter field
	 *
	 * @param value
	 */
	public void setNewsletterFieldName(final String value)
	{
		setProperty(NEWSLETTER_FIELD_NAME_KEY, value);
	}

	/**
	 * Return name of sender address field
	 *
	 * @return
	 */
	public String getSenderAddress()
	{
		return getStringProperty(SENDER_ADDRESS_KEY);
	}

	/**
	 * Set name of sender address field
	 *
	 * @param value
	 */
	public void setSenderAddress(final String value)
	{
		setProperty(SENDER_ADDRESS_KEY, value);
	}

	/**
	 * Return name of sender name field
	 *
	 * @return
	 */
	public String getSenderName()
	{
		return getStringProperty(SENDER_NAME_KEY);
	}

	/**
	 * Set name of sender name field
	 *
	 * @param value
	 */
	public void setSenderName(final String value)
	{
		setProperty(SENDER_NAME_KEY, value);
	}

	/**
	 * Return name of public user directory
	 *
	 * @return
	 */
	public String getPublicUserDirectoryName()
	{
		return getStringProperty(PUBLIC_USER_DIRECTORY_NAME_KEY);
	}

	/**
	 * Set name of public user directory
	 *
	 * @param value
	 */
	public void setPublicUserDirectoryName(final String value)
	{
		setProperty(PUBLIC_USER_DIRECTORY_NAME_KEY, value);
	}

	/**
	 * Return inline CSS
	 *
	 * @return
	 */
	public String getInlineCss()
	{
		return getStringProperty(INLINE_CSS_KEY);
	}

	/**
	 * Set inline CSS
	 *
	 * @param value
	 */
	public void setInlineCss(final String value)
	{
		setProperty(INLINE_CSS_KEY, value);
	}

	/**
	 * Return assigned username
	 *
	 * @return
	 */
	public String getAssignedUsername()
	{
		return getStringProperty(ASSIGNED_USERNAME_KEY);
	}

	/**
	 * Set assigned username
	 *
	 * @param value
	 */
	public void setAssignedUsername(final String value)
	{
		setProperty(ASSIGNED_USERNAME_KEY, value);
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers)
	{
		renderers.put(RenderMode.Default, new RegistrationCheckRenderer());
	}
}
