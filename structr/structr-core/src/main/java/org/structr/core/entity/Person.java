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

import java.util.Date;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.EntityContext;

/**
 * 
 * @author amorgner
 * 
 */
public class Person extends Principal {

    public static enum Key implements PropertyKey {

	    salutation, firstName, middleNameOrInitial, lastName,
	    email1, email2, phoneNumber1, phoneNumber2,
	    faxNumber1, faxNumber2,
	    street, zipCode, city, state, country, birthday, gender, newsletter
    }

    static {
		EntityContext.registerPropertySet(Person.class, PropertyView.All, Key.values());

		// public properties
		EntityContext.registerPropertySet(User.class, PropertyView.Public,
			Key.salutation, Key.firstName, Key.middleNameOrInitial, Key.lastName
		);
    }

    private final static String ICON_SRC = "/images/user.png";
    
    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public void setFirstName(final String firstName) {
        setProperty(Key.firstName.name(), firstName);
        String lastName = getLastName() != null && !(getLastName().isEmpty()) ? getLastName() : "";
        setName(lastName + ", " + firstName);
    }

    public String getFirstName() {
        return getStringProperty(Key.firstName.name());
    }

    public void setLastName(final String lastName) {
        setProperty(Key.lastName.name(), lastName);
        String firstName = getFirstName() != null && !(getFirstName().isEmpty()) ? getFirstName() : "";
        setProperty(AbstractNode.Key.name.name(), lastName + ", " + firstName);
    }

    @Override
    public void setName(final String name) {
        setProperty(AbstractNode.Key.name.name(), name);
    }

    public String getLastName() {
        return getStringProperty(Key.lastName.name());
    }

    public void setSalutation(final String salutation) {
        setProperty(Key.salutation.name(), salutation);
    }

    public String getSalutation() {
        return getStringProperty(Key.salutation.name());
    }

    public void setMiddleNameOrInitial(final String middleNameOrInitial) {
        setProperty(Key.middleNameOrInitial.name(), middleNameOrInitial);
    }

    public String getMiddleNameOrInitial() {
        return getStringProperty(Key.middleNameOrInitial.name());
    }

    public void setEmail1(final String email1) {
        setProperty(Key.email1.name(), email1);
    }

    public String getEmail1() {
        return getStringProperty(Key.email1.name());
    }

    public void setEmail2(final String email2) {
        setProperty(Key.email2.name(), email2);
    }

    public String getEmail2() {
        return getStringProperty(Key.email2.name());
    }
    public void setPhoneNumber1(final String value) {
        setProperty(Key.phoneNumber1.name(), value);
    }

    public String getPhoneNumber1() {
        return getStringProperty(Key.phoneNumber1.name());
    }

    public void setPhoneNumber2(final String value) {
        setProperty(Key.phoneNumber2.name(), value);
    }

    public String getPhoneNumber2() {
        return getStringProperty(Key.phoneNumber2.name());
    }
    
    public void setFaxNumber1(final String value) {
        setProperty(Key.faxNumber1.name(), value);
    }

    public String getFaxNumber1() {
        return getStringProperty(Key.faxNumber1.name());
    }

    public void setFaxNumber2(final String value) {
        setProperty(Key.faxNumber2.name(), value);
    }

    public String getFaxNumber2() {
        return getStringProperty(Key.faxNumber2.name());
    }
    
    public void setStreet(final String value) {
        setProperty(Key.street.name(), value);
    }

    public String getStreet() {
        return getStringProperty(Key.street.name());
    }

    public void setZipCode(final String value) {
        setProperty(Key.zipCode.name(), value);
    }

    public String getZipCode() {
        return getStringProperty(Key.zipCode.name());
    }

    public void setState(final String value) {
        setProperty(Key.state.name(), value);
    }

    public String getState() {
        return getStringProperty(Key.state.name());
    }

    public void setCountry(final String value) {
        setProperty(Key.country.name(), value);
    }

    public String getCountry() {
        return getStringProperty(Key.country.name());
    }

    public void setCity(final String value) {
        setProperty(Key.city.name(), value);
    }

    public String getCity() {
        return getStringProperty(Key.city.name());
    }

    public void setNewsletter(final boolean value) {
        setProperty(Key.newsletter.name(), value);
    }

    public boolean getNewsletter() {
        return getBooleanProperty(Key.newsletter.name());
    }

    public void setBirthday(final Date value) {
        setProperty(Key.birthday.name(), value);
    }

    public Date getBirthday() {
        return getDateProperty(Key.birthday.name());
    }

    public void setGender(final String value) {
        setProperty(Key.gender.name(), value);
    }

    public String getGender() {
        return getStringProperty(Key.gender.name());
    }
}
