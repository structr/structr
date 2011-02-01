package org.structr.core.entity;

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
        // use first name and last name instead
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

}
