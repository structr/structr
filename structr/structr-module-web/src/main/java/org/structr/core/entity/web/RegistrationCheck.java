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
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.common.RenderMode;
import org.structr.core.EntityContext;
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
public class RegistrationCheck extends LoginCheck {

	static {

		EntityContext.registerPropertySet(RegistrationCheck.class,
						  PropertyView.All,
						  Key.values());
	}

	public enum Key implements PropertyKey {
		publicUserDirectoryName, senderAddress, senderName, inlineCss,
		assignedUsername, confirmationKeyFieldName, confirmPasswordFieldName,
		agreedToTermsOfUseFieldName, newsletterFieldName;

	}

	@Override
	public String getIconSrc() {
		return "/images/application_key.png";
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
	 * Set name of confirmation key field
	 *
	 * @param value
	 */
	public void setConfirmationKeyFieldName(final String value) {
		setProperty(Key.confirmationKeyFieldName.name(), value);
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
	 * Set name of confirm password field
	 *
	 * @param value
	 */
	public void setConfirmPasswordFieldName(final String value) {
		setProperty(Key.confirmPasswordFieldName.name(), value);
	}

	/**
	 * Return name of first name field
	 *
	 * @return
	 */
	public String getFirstNameFieldName() {
		return getStringProperty(RegistrationForm.Key.firstNameFieldName.name());
	}

	/**
	 * Set name of username field
	 *
	 * @param value
	 */
	public void setFirstNameFieldName(final String value) {
		setProperty(RegistrationForm.Key.firstNameFieldName.name(), value);
	}

	/**
	 * Return name of last name field
	 *
	 * @return
	 */
	public String getLastNameFieldName() {
		return getStringProperty(RegistrationForm.Key.lastNameFieldName.name());
	}

	/**
	 * Set name of last name field
	 *
	 * @param value
	 */
	public void setLastNameFieldName(final String value) {
		setProperty(RegistrationForm.Key.lastNameFieldName.name(), value);
	}

	/**
	 * Return name of email field
	 *
	 * @return
	 */
	public String getEmailFieldName() {
		return getStringProperty(RegistrationForm.Key.emailFieldName.name());
	}

	/**
	 * Set name of email field
	 *
	 * @param value
	 */
	public void setEmailFieldName(final String value) {
		setProperty(RegistrationForm.Key.emailFieldName.name(), value);
	}

	/**
	 * Return name of confirm email field
	 *
	 * @return
	 */
	public String getConfirmEmailFieldName() {
		return getStringProperty(RegistrationForm.Key.confirmEmailFieldName.name());
	}

	/**
	 * Set name of confirm email field
	 *
	 * @param value
	 */
	public void setConfirmEmailFieldName(final String value) {
		setProperty(RegistrationForm.Key.confirmEmailFieldName.name(), value);
	}

	/**
	 * Return name of zip code field
	 *
	 * @return
	 */
	public String getZipCodeFieldName() {
		return getStringProperty(RegistrationForm.Key.zipCodeFieldName.name());
	}

	/**
	 * Set name of zip code field
	 *
	 * @param value
	 */
	public void setZipCodeFieldName(final String value) {
		setProperty(RegistrationForm.Key.zipCodeFieldName.name(), value);
	}

	/**
	 * Return name of city field
	 *
	 * @return
	 */
	public String getCityFieldName() {
		return getStringProperty(RegistrationForm.Key.cityFieldName.name());
	}

	/**
	 * Set name of city field
	 *
	 * @param value
	 */
	public void setCityFieldName(final String value) {
		setProperty(RegistrationForm.Key.cityFieldName.name(), value);
	}

	/**
	 * Return name of street field
	 *
	 * @return
	 */
	public String getStreetFieldName() {
		return getStringProperty(RegistrationForm.Key.streetFieldName.name());
	}

	/**
	 * Set name of street field
	 *
	 * @param value
	 */
	public void setStreetFieldName(final String value) {
		setProperty(RegistrationForm.Key.streetFieldName.name(), value);
	}

	/**
	 * Return name of state field
	 *
	 * @return
	 */
	public String getStateFieldName() {
		return getStringProperty(RegistrationForm.Key.stateFieldName.name());
	}

	/**
	 * Set name of state field
	 *
	 * @param value
	 */
	public void setStateFieldName(final String value) {
		setProperty(RegistrationForm.Key.stateFieldName.name(), value);
	}

	/**
	 * Return name of country field
	 *
	 * @return
	 */
	public String getCountryFieldName() {
		return getStringProperty(RegistrationForm.Key.countryFieldName.name());
	}

	/**
	 * Set name of country field
	 *
	 * @param value
	 */
	public void setCountryFieldName(final String value) {
		setProperty(RegistrationForm.Key.countryFieldName.name(), value);
	}

	/**
	 * Return name of birthday field
	 *
	 * @return
	 */
	public String getBirthdayFieldName() {
		return getStringProperty(RegistrationForm.Key.birthdayFieldName.name());
	}

	/**
	 * Set name of birthday field
	 *
	 * @param value
	 */
	public void setBirthdayFieldName(final String value) {
		setProperty(RegistrationForm.Key.birthdayFieldName.name(), value);
	}

	/**
	 * Return name of gender field
	 *
	 * @return
	 */
	public String getGenderFieldName() {
		return getStringProperty(RegistrationForm.Key.genderFieldName.name());
	}

	/**
	 * Set name of gender field
	 *
	 * @param value
	 */
	public void setGenderFieldName(final String value) {
		setProperty(RegistrationForm.Key.genderFieldName.name(), value);
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
	 * Set name of agreed to terms of use field
	 *
	 * @param value
	 */
	public void setAgreedToTermsOfUseFieldName(final String value) {
		setProperty(Key.agreedToTermsOfUseFieldName.name(), value);
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
	 * Set name of newsletter field
	 *
	 * @param value
	 */
	public void setNewsletterFieldName(final String value) {
		setProperty(Key.newsletterFieldName.name(), value);
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
	 * Set name of sender address field
	 *
	 * @param value
	 */
	public void setSenderAddress(final String value) {
		setProperty(Key.senderAddress.name(), value);
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
	 * Set name of sender name field
	 *
	 * @param value
	 */
	public void setSenderName(final String value) {
		setProperty(Key.senderName.name(), value);
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
	 * Set name of public user directory
	 *
	 * @param value
	 */
	public void setPublicUserDirectoryName(final String value) {
		setProperty(Key.publicUserDirectoryName.name(), value);
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
	 * Set inline CSS
	 *
	 * @param value
	 */
	public void setInlineCss(final String value) {
		setProperty(Key.inlineCss.name(), value);
	}

	/**
	 * Return assigned username
	 *
	 * @return
	 */
	public String getAssignedUsername() {
		return getStringProperty(Key.assignedUsername.name());
	}

	/**
	 * Set assigned username
	 *
	 * @param value
	 */
	public void setAssignedUsername(final String value) {
		setProperty(Key.assignedUsername.name(), value);
	}

	@Override
	public void initializeRenderers(Map<RenderMode, NodeRenderer> renderers) {
		renderers.put(RenderMode.Default, new RegistrationCheckRenderer());
	}
}
