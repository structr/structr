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



package org.structr.core.entity;

import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import org.structr.common.Property;
import org.structr.common.View;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 *
 */
public class Person extends PrincipalImpl {

	public static final Property<String>  salutation          = new Property<String>("salutation");
	public static final Property<String>  firstName           = new Property<String>("firstName");
	public static final Property<String>  middleNameOrInitial = new Property<String>("middleNameOrInitial");
	public static final Property<String>  lastName            = new Property<String>("lastName");
	public static final Property<String>  email               = new Property<String>("email");
	public static final Property<String>  email2              = new Property<String>("email2");
	public static final Property<String>  phoneNumber1        = new Property<String>("phoneNumber1");
	public static final Property<String>  phoneNumber2        = new Property<String>("phoneNumber2");
	public static final Property<String>  faxNumber1          = new Property<String>("faxNumber1");
	public static final Property<String>  faxNumber2          = new Property<String>("faxNumber2");
	public static final Property<String>  street              = new Property<String>("street");
	public static final Property<String>  zipCode             = new Property<String>("zipCode");
	public static final Property<String>  city                = new Property<String>("city");
	public static final Property<String>  state               = new Property<String>("state");
	public static final Property<String>  country             = new Property<String>("country");
	public static final Property<Date>    birthday            = new Property<Date>("birthday");
	public static final Property<String>  gender              = new Property<String>("gender");
	public static final Property<Boolean> newsletter          = new Property<Boolean>("newsletter");
	
	public static final View publicView = new View(PropertyView.Public,
		salutation, firstName, middleNameOrInitial, lastName
	);
	
	static {

//		EntityContext.registerPropertySet(Person.class, PropertyView.All, Key.values());

		// public properties
//		EntityContext.registerPropertySet(Person.class, PropertyView.Public, Key.salutation, Key.firstName, Key.middleNameOrInitial, Key.lastName);

	}

	//~--- constant enums -------------------------------------------------

	
	//~--- get methods ----------------------------------------------------

	public String getFirstName() {

		return getStringProperty(Person.firstName);

	}

	public String getLastName() {

		return getStringProperty(Person.lastName);

	}

	public String getSalutation() {

		return getStringProperty(Person.salutation);

	}

	public String getMiddleNameOrInitial() {

		return getStringProperty(Person.middleNameOrInitial);

	}

	public String getEmail() {

		return getStringProperty(Person.email);

	}

	public String getEmail2() {

		return getStringProperty(Person.email2);

	}

	public String getPhoneNumber1() {

		return getStringProperty(Person.phoneNumber1);

	}

	public String getPhoneNumber2() {

		return getStringProperty(Person.phoneNumber2);

	}

	public String getFaxNumber1() {

		return getStringProperty(Person.faxNumber1);

	}

	public String getFaxNumber2() {

		return getStringProperty(Person.faxNumber2);

	}

	public String getStreet() {

		return getStringProperty(Person.street);

	}

	public String getZipCode() {

		return getStringProperty(Person.zipCode);

	}

	public String getState() {

		return getStringProperty(Person.state);

	}

	public String getCountry() {

		return getStringProperty(Person.country);

	}

	public String getCity() {

		return getStringProperty(Person.city);

	}

	public boolean getNewsletter() {

		return getBooleanProperty(Person.newsletter);

	}

	public Date getBirthday() {

		return getDateProperty(Person.birthday);

	}

	public String getGender() {

		return getStringProperty(Person.gender);

	}

	//~--- set methods ----------------------------------------------------

	public void setFirstName(final String firstName) throws FrameworkException {

		setProperty(Person.firstName, firstName);

		String lastName = ((getLastName() != null) &&!(getLastName().isEmpty()))
				  ? getLastName()
				  : "";

		setName(lastName + ", " + firstName);

	}

	public void setLastName(final String lastName) throws FrameworkException {

		setProperty(Person.lastName, lastName);

		String firstName = ((getFirstName() != null) &&!(getFirstName().isEmpty()))
				   ? getFirstName()
				   : "";

		setProperty(AbstractNode.name, lastName + ", " + firstName);

	}

	@Override
	public void setName(final String name) throws FrameworkException {

		setProperty(AbstractNode.name, name);

	}

	public void setSalutation(final String salutation) throws FrameworkException {

		setProperty(Person.salutation, salutation);

	}

	public void setMiddleNameOrInitial(final String middleNameOrInitial) throws FrameworkException {

		setProperty(Person.middleNameOrInitial, middleNameOrInitial);

	}

	public void setEmail(final String email) throws FrameworkException {

		setProperty(Person.email, email);

	}

	public void setEmail2(final String email2) throws FrameworkException {

		setProperty(Person.email2, email2);

	}

	public void setPhoneNumber1(final String value) throws FrameworkException {

		setProperty(Person.phoneNumber1, value);

	}

	public void setPhoneNumber2(final String value) throws FrameworkException {

		setProperty(Person.phoneNumber2, value);

	}

	public void setFaxNumber1(final String value) throws FrameworkException {

		setProperty(Person.faxNumber1, value);

	}

	public void setFaxNumber2(final String value) throws FrameworkException {

		setProperty(Person.faxNumber2, value);

	}

	public void setStreet(final String value) throws FrameworkException {

		setProperty(Person.street, value);

	}

	public void setZipCode(final String value) throws FrameworkException {

		setProperty(Person.zipCode, value);

	}

	public void setState(final String value) throws FrameworkException {

		setProperty(Person.state, value);

	}

	public void setCountry(final String value) throws FrameworkException {

		setProperty(Person.country, value);

	}

	public void setCity(final String value) throws FrameworkException {

		setProperty(Person.city, value);

	}

	public void setNewsletter(final boolean value) throws FrameworkException {

		setProperty(Person.newsletter, value);

	}

	public void setBirthday(final Date value) throws FrameworkException {

		setProperty(Person.birthday, value);

	}

	public void setGender(final String value) throws FrameworkException {

		setProperty(Person.gender, value);

	}

}
