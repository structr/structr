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

/**
 * 
 * @author amorgner
 * 
 */
public class Person extends Principal {

    public final static String SALUTATION_KEY = "salutation";
    public final static String FIRST_NAME_KEY = "firstName";
    public final static String MIDDLE_NAME_OR_INITIAL_KEY = "middleNameOrInitial";
    public final static String LAST_NAME_KEY = "lastName";
    public final static String EMAIL_1_KEY = "email1";
    public final static String EMAIL_2_KEY = "email2";
    public final static String EMAIL_3_KEY = "email3";
    public final static String EMAIL_4_KEY = "email4";
    public final static String PHONE_NUMBER_1_KEY = "phoneNumber1";
    public final static String PHONE_NUMBER_2_KEY = "phoneNumber2";
    public final static String PHONE_NUMBER_3_KEY = "phoneNumber3";
    public final static String PHONE_NUMBER_4_KEY = "phoneNumber4";
    public final static String PHONE_NUMBER_5_KEY = "phoneNumber5";
    public final static String PHONE_NUMBER_6_KEY = "phoneNumber6";
    public final static String FAX_NUMBER_1_KEY = "faxNumber1";
    public final static String FAX_NUMBER_2_KEY = "faxNumber2";
    public final static String FAX_NUMBER_3_KEY = "faxNumber3";
    public final static String STREET_KEY = "street";
    public final static String ZIP_CODE_KEY = "zipCode";
    public final static String CITY_KEY = "city";
    public final static String STATE_KEY = "state";
    public final static String COUNTRY_KEY = "country";
    public final static String BIRTHDAY_KEY = "birthday";
    public final static String GENDER_KEY = "gender";
    public final static String NEWSLETTER_KEY = "newsletter";
    private final static String ICON_SRC = "/images/user.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    public void setFirstName(final String firstName) {
        setProperty(FIRST_NAME_KEY, firstName);
        String lastName = getLastName() != null && !(getLastName().isEmpty()) ? getLastName() : "";
        setProperty(NAME_KEY, lastName + ", " + firstName);
    }

    public String getFirstName() {
        return (String) getProperty(FIRST_NAME_KEY);
    }

    public void setLastName(final String lastName) {
        setProperty(LAST_NAME_KEY, lastName);
        String firstName = getFirstName() != null && !(getFirstName().isEmpty()) ? getFirstName() : "";
        setProperty(NAME_KEY, lastName + ", " + firstName);
    }

    @Override
    public void setName(final String name) {
        setProperty(NAME_KEY, name);
    }

    public String getLastName() {
        return (String) getProperty(LAST_NAME_KEY);
    }

    public void setSalutation(final String salutation) {
        setProperty(SALUTATION_KEY, salutation);
    }

    public String getSalutation() {
        return (String) getProperty(SALUTATION_KEY);
    }

    public void setMiddleNameOrInitial(final String middleNameOrInitial) {
        setProperty(MIDDLE_NAME_OR_INITIAL_KEY, middleNameOrInitial);
    }

    public String getMiddleNameOrInitial() {
        return (String) getProperty(MIDDLE_NAME_OR_INITIAL_KEY);
    }

    public void setEmail1(final String email1) {
        setProperty(EMAIL_1_KEY, email1);
    }

    public String getEmail1() {
        return (String) getProperty(EMAIL_1_KEY);
    }

    public void setEmail2(final String email2) {
        setProperty(EMAIL_2_KEY, email2);
    }

    public String getEmail2() {
        return (String) getProperty(EMAIL_2_KEY);
    }

    public void setEmail3(final String email3) {
        setProperty(EMAIL_3_KEY, email3);
    }

    public String getEmail3() {
        return (String) getProperty(EMAIL_3_KEY);
    }

    public void setEmail4(final String email4) {
        setProperty(EMAIL_4_KEY, email4);
    }

    public String getEmail4() {
        return (String) getProperty(EMAIL_4_KEY);
    }

    public void setPhoneNumber1(final String value) {
        setProperty(PHONE_NUMBER_1_KEY, value);
    }

    public String getPhoneNumber1() {
        return (String) getProperty(PHONE_NUMBER_1_KEY);
    }

    public void setPhoneNumber2(final String value) {
        setProperty(PHONE_NUMBER_2_KEY, value);
    }

    public String getPhoneNumber2() {
        return (String) getProperty(PHONE_NUMBER_2_KEY);
    }

    public void setPhoneNumber3(final String value) {
        setProperty(PHONE_NUMBER_3_KEY, value);
    }

    public String getPhoneNumber3() {
        return (String) getProperty(PHONE_NUMBER_3_KEY);
    }

    public void setPhoneNumber4(final String value) {
        setProperty(PHONE_NUMBER_4_KEY, value);
    }

    public String getPhoneNumber4() {
        return (String) getProperty(PHONE_NUMBER_4_KEY);
    }

    public void setPhoneNumber5(final String value) {
        setProperty(PHONE_NUMBER_5_KEY, value);
    }

    public String getPhoneNumber5() {
        return (String) getProperty(PHONE_NUMBER_5_KEY);
    }

    public void setPhoneNumber6(final String value) {
        setProperty(PHONE_NUMBER_6_KEY, value);
    }

    public String getPhoneNumber6() {
        return (String) getProperty(PHONE_NUMBER_6_KEY);
    }

    public void setFaxNumber1(final String value) {
        setProperty(FAX_NUMBER_1_KEY, value);
    }

    public String getFaxNumber1() {
        return (String) getProperty(FAX_NUMBER_1_KEY);
    }

    public void setFaxNumber2(final String value) {
        setProperty(FAX_NUMBER_2_KEY, value);
    }

    public String getFaxNumber2() {
        return (String) getProperty(FAX_NUMBER_2_KEY);
    }

    public void setFaxNumber3(final String value) {
        setProperty(FAX_NUMBER_3_KEY, value);
    }

    public String getFaxNumber3() {
        return (String) getProperty(FAX_NUMBER_3_KEY);
    }

    public void setStreet(final String value) {
        setProperty(STREET_KEY, value);
    }

    public String getStreet() {
        return (String) getProperty(STREET_KEY);
    }

    public void setZipCode(final String value) {
        setProperty(ZIP_CODE_KEY, value);
    }

    public String getZipCode() {
        return (String) getProperty(ZIP_CODE_KEY);
    }

    public void setState(final String value) {
        setProperty(STATE_KEY, value);
    }

    public String getState() {
        return (String) getProperty(STATE_KEY);
    }

    public void setCountry(final String value) {
        setProperty(COUNTRY_KEY, value);
    }

    public String getCountry() {
        return (String) getProperty(COUNTRY_KEY);
    }

    public void setCity(final String value) {
        setProperty(CITY_KEY, value);
    }

    public String getCity() {
        return (String) getProperty(CITY_KEY);
    }

    public void setNewsletter(final boolean value) {
        setProperty(NEWSLETTER_KEY, value);
    }

    public boolean getNewsletter() {
        return getBooleanProperty(NEWSLETTER_KEY);
    }

    public void setBirthday(final Date value) {
        setProperty(BIRTHDAY_KEY, value);
    }

    public Date getBirthday() {
        return getDateProperty(BIRTHDAY_KEY);
    }

    public void setGender(final String value) {
        setProperty(GENDER_KEY, value);
    }

    public String getGender() {
        return (String) getProperty(GENDER_KEY);
    }
}
