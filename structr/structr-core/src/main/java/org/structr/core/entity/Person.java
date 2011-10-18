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



package org.structr.core.entity;

import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.EntityContext;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Person extends Principal {

	static {

		EntityContext.registerPropertySet(Person.class,
						  PropertyView.All,
						  Key.values());

		// public properties
		EntityContext.registerPropertySet(Person.class,
						  PropertyView.Public,
						  Key.salutation,
						  Key.firstName,
						  Key.middleNameOrInitial,
						  Key.lastName);
	}

	//~--- constant enums -------------------------------------------------

	public static enum Key implements PropertyKey {

		salutation, firstName, middleNameOrInitial, lastName, email1, email2, phoneNumber1, phoneNumber2,
		faxNumber1, faxNumber2, street, zipCode, city, state, country, birthday, gender, newsletter
	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getIconSrc() {
		return "/images/user.png";
	}

	public String getFirstName() {
		return getStringProperty(Key.firstName.name());
	}

	public String getLastName() {
		return getStringProperty(Key.lastName.name());
	}

	public String getSalutation() {
		return getStringProperty(Key.salutation.name());
	}

	public String getMiddleNameOrInitial() {
		return getStringProperty(Key.middleNameOrInitial.name());
	}

	public String getEmail1() {
		return getStringProperty(Key.email1.name());
	}

	public String getEmail2() {
		return getStringProperty(Key.email2.name());
	}

	public String getPhoneNumber1() {
		return getStringProperty(Key.phoneNumber1.name());
	}

	public String getPhoneNumber2() {
		return getStringProperty(Key.phoneNumber2.name());
	}

	public String getFaxNumber1() {
		return getStringProperty(Key.faxNumber1.name());
	}

	public String getFaxNumber2() {
		return getStringProperty(Key.faxNumber2.name());
	}

	public String getStreet() {
		return getStringProperty(Key.street.name());
	}

	public String getZipCode() {
		return getStringProperty(Key.zipCode.name());
	}

	public String getState() {
		return getStringProperty(Key.state.name());
	}

	public String getCountry() {
		return getStringProperty(Key.country.name());
	}

	public String getCity() {
		return getStringProperty(Key.city.name());
	}

	public boolean getNewsletter() {
		return getBooleanProperty(Key.newsletter.name());
	}

	public Date getBirthday() {
		return getDateProperty(Key.birthday.name());
	}

	public String getGender() {
		return getStringProperty(Key.gender.name());
	}

	//~--- set methods ----------------------------------------------------

	public void setFirstName(final String firstName) {

		setProperty(Key.firstName.name(),
			    firstName);

		String lastName = ((getLastName() != null) &&!(getLastName().isEmpty()))
				  ? getLastName()
				  : "";

		setName(lastName + ", " + firstName);
	}

	public void setLastName(final String lastName) {

		setProperty(Key.lastName.name(),
			    lastName);

		String firstName = ((getFirstName() != null) &&!(getFirstName().isEmpty()))
				   ? getFirstName()
				   : "";

		setProperty(AbstractNode.Key.name.name(),
			    lastName + ", " + firstName);
	}

	@Override
	public void setName(final String name) {

		setProperty(AbstractNode.Key.name.name(),
			    name);
	}

	public void setSalutation(final String salutation) {

		setProperty(Key.salutation.name(),
			    salutation);
	}

	public void setMiddleNameOrInitial(final String middleNameOrInitial) {

		setProperty(Key.middleNameOrInitial.name(),
			    middleNameOrInitial);
	}

	public void setEmail1(final String email1) {

		setProperty(Key.email1.name(),
			    email1);
	}

	public void setEmail2(final String email2) {

		setProperty(Key.email2.name(),
			    email2);
	}

	public void setPhoneNumber1(final String value) {

		setProperty(Key.phoneNumber1.name(),
			    value);
	}

	public void setPhoneNumber2(final String value) {

		setProperty(Key.phoneNumber2.name(),
			    value);
	}

	public void setFaxNumber1(final String value) {

		setProperty(Key.faxNumber1.name(),
			    value);
	}

	public void setFaxNumber2(final String value) {

		setProperty(Key.faxNumber2.name(),
			    value);
	}

	public void setStreet(final String value) {

		setProperty(Key.street.name(),
			    value);
	}

	public void setZipCode(final String value) {

		setProperty(Key.zipCode.name(),
			    value);
	}

	public void setState(final String value) {

		setProperty(Key.state.name(),
			    value);
	}

	public void setCountry(final String value) {

		setProperty(Key.country.name(),
			    value);
	}

	public void setCity(final String value) {

		setProperty(Key.city.name(),
			    value);
	}

	public void setNewsletter(final boolean value) {

		setProperty(Key.newsletter.name(),
			    value);
	}

	public void setBirthday(final Date value) {

		setProperty(Key.birthday.name(),
			    value);
	}

	public void setGender(final String value) {

		setProperty(Key.gender.name(),
			    value);
	}
}
