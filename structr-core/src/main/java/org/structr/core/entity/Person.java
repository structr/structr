/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.structr.core.entity;

import org.structr.core.property.PasswordProperty;
import org.structr.core.property.StringProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.common.Permission;
import org.structr.core.property.Property;
import org.structr.common.RelType;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.core.Services;
import org.structr.core.graph.CreateRelationshipCommand;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 *
 */
public class Person extends AbstractNode implements Principal {

	public static final Property<String>  salutation          = new StringProperty("salutation");
	public static final Property<String>  firstName           = new StringProperty("firstName");
	public static final Property<String>  middleNameOrInitial = new StringProperty("middleNameOrInitial");
	public static final Property<String>  lastName            = new StringProperty("lastName");
	public static final Property<String>  email               = new StringProperty("email");
	public static final Property<String>  email2              = new StringProperty("email2");
	public static final Property<String>  password            = new PasswordProperty("password");
	public static final Property<String>  phoneNumber1        = new StringProperty("phoneNumber1");
	public static final Property<String>  phoneNumber2        = new StringProperty("phoneNumber2");
	public static final Property<String>  faxNumber1          = new StringProperty("faxNumber1");
	public static final Property<String>  faxNumber2          = new StringProperty("faxNumber2");
	public static final Property<String>  street              = new StringProperty("street");
	public static final Property<String>  zipCode             = new StringProperty("zipCode");
	public static final Property<String>  city                = new StringProperty("city");
	public static final Property<String>  state               = new StringProperty("state");
	public static final Property<String>  country             = new StringProperty("country");
	public static final Property<Date>    birthday            = new ISO8601DateProperty("birthday");
	public static final Property<String>  gender              = new StringProperty("gender");
	public static final Property<Boolean> newsletter          = new BooleanProperty("newsletter");
	
	public static final View publicView = new View(Person.class, PropertyView.Public,
		name, salutation, firstName, middleNameOrInitial, lastName
	);
	
	static {

//		EntityContext.registerPropertySet(Person.class, PropertyView.All, Key.values());

		// public properties
//		EntityContext.registerPropertySet(Person.class, PropertyView.Public, Key.salutation, Key.firstName, Key.middleNameOrInitial, Key.lastName);

	}

	//~--- constant enums -------------------------------------------------

	
	//~--- get methods ----------------------------------------------------

	public String getFirstName() {

		return getProperty(Person.firstName);

	}

	public String getLastName() {

		return getProperty(Person.lastName);

	}

	public String getSalutation() {

		return getProperty(Person.salutation);

	}

	public String getMiddleNameOrInitial() {

		return getProperty(Person.middleNameOrInitial);

	}

	public String getEmail() {

		return getProperty(Person.email);

	}

	public String getEmail2() {

		return getProperty(Person.email2);

	}

	public String getPhoneNumber1() {

		return getProperty(Person.phoneNumber1);

	}

	public String getPhoneNumber2() {

		return getProperty(Person.phoneNumber2);

	}

	public String getFaxNumber1() {

		return getProperty(Person.faxNumber1);

	}

	public String getFaxNumber2() {

		return getProperty(Person.faxNumber2);

	}

	public String getStreet() {

		return getProperty(Person.street);

	}

	public String getZipCode() {

		return getProperty(Person.zipCode);

	}

	public String getState() {

		return getProperty(Person.state);

	}

	public String getCountry() {

		return getProperty(Person.country);

	}

	public String getCity() {

		return getProperty(Person.city);

	}

	public boolean getNewsletter() {

		return getBooleanProperty(Person.newsletter);

	}

	public Date getBirthday() {

		return getDateProperty(Person.birthday);

	}

	public String getGender() {

		return getProperty(Person.gender);

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

	@Override
	public void block() throws FrameworkException {

		setBlocked(Boolean.TRUE);

	}

	@Override
	public void grant(Permission permission, AbstractNode obj) {

		AbstractRelationship secRel = obj.getSecurityRelationship(this);

		if (secRel == null) {

			try {

				secRel = createSecurityRelationshipTo(obj);

			} catch (FrameworkException ex) {

				Logger.getLogger(Person.class.getName()).log(Level.SEVERE, "Could not create security relationship!", ex);

			}

		}

		secRel.addPermission(permission);

	}

	@Override
	public void revoke(Permission permission, AbstractNode obj) {

		AbstractRelationship secRel = obj.getSecurityRelationship(this);

		if (secRel == null) {

			try {

				secRel = createSecurityRelationshipTo(obj);

			} catch (FrameworkException ex) {

				Logger.getLogger(Person.class.getName()).log(Level.SEVERE, "Could not create security relationship!", ex);

			}

		}

		secRel.removePermission(permission);

	}

	private AbstractRelationship createSecurityRelationshipTo(final AbstractNode obj) throws FrameworkException {

		return Services.command(SecurityContext.getSuperUserInstance(), CreateRelationshipCommand.class).execute(this, obj, RelType.SECURITY);

	}

	//~--- get methods ----------------------------------------------------

	@Override
	public String getEncryptedPassword() {

		boolean dbNodeHasProperty = dbNode.hasProperty(password.dbName());

		if (dbNodeHasProperty) {

			Object dbValue = dbNode.getProperty(password.dbName());

			return (String) dbValue;

		} else {

			return null;
		}

	}

	@Override
	public Boolean getBlocked() {

		return (Boolean) getProperty(blocked);

	}

	@Override
	public List<Principal> getParents() {

		List<Principal> parents               = new LinkedList<Principal>();
		List<AbstractRelationship> parentRels = getIncomingRelationships(RelType.CHILDREN);

		for (AbstractRelationship rel : parentRels) {

			AbstractNode node = rel.getStartNode();

			if (node instanceof Principal) {

				parents.add((Principal) node);
			}

		}

		return parents;

	}

	@Override
	public Boolean isBlocked() {

		return Boolean.TRUE.equals(getBlocked());

	}

	//~--- set methods ----------------------------------------------------

	@Override
	public void setBlocked(final Boolean blocked) throws FrameworkException {}


}
