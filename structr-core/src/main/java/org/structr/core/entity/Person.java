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

import org.structr.core.property.StringProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.BooleanProperty;
import org.structr.common.PropertyView;
import org.structr.common.error.FrameworkException;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.logging.Logger;
import org.structr.core.property.Property;
import org.structr.common.View;
import org.structr.core.Services;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.validator.TypeUniquenessValidator;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 *
 */
public class Person extends AbstractNode {
	
	private static final Logger logger = Logger.getLogger(Person.class.getName());

	public static final Property<String>  salutation          = new StringProperty("salutation");
	public static final Property<String>  firstName           = new StringProperty("firstName").indexed();
	public static final Property<String>  middleNameOrInitial = new StringProperty("middleNameOrInitial");
	public static final Property<String>  lastName            = new StringProperty("lastName").indexed();
	
	public static final Property<String>  twitterName         = new StringProperty("twitterName").validator(new TypeUniquenessValidator(Person.class)).indexed();
	public static final Property<String>  eMail               = new StringProperty("eMail").validator(new TypeUniquenessValidator(Person.class)).indexed();
	public static final Property<String>  eMail2              = new StringProperty("eMail2");
	
	public static final Property<String>  phoneNumber1        = new StringProperty("phoneNumber1");
	public static final Property<String>  phoneNumber2        = new StringProperty("phoneNumber2");
	public static final Property<String>  faxNumber1          = new StringProperty("faxNumber1");
	public static final Property<String>  faxNumber2          = new StringProperty("faxNumber2");
	
	public static final Property<String>  street              = new StringProperty("street").indexed();
	public static final Property<String>  zipCode             = new StringProperty("zipCode").indexed();
	public static final Property<String>  city                = new StringProperty("city").indexed();
	public static final Property<String>  state               = new StringProperty("state").indexed();
	public static final Property<String>  country             = new StringProperty("country").indexed();
	
	public static final Property<Date>    birthday            = new ISO8601DateProperty("birthday");
	public static final Property<String>  gender              = new StringProperty("gender");
	public static final Property<Boolean> newsletter          = new BooleanProperty("newsletter");
	
	public static final View publicView = new View(Person.class, PropertyView.Public,
		name, salutation, firstName, middleNameOrInitial, lastName
	);

	//~--- set methods ----------------------------------------------------

	public void setFirstName(final String firstName) throws FrameworkException {
			
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {


				setProperty(Person.firstName, firstName);
				
				String _lastName = getProperty(Person.lastName);

				String lastName = (_lastName != null &&!(_lastName.isEmpty()))
						  ? _lastName : "";

				setProperty(Person.name, lastName + ", " + firstName);
				
				return null;
			}
		});

	}

	public void setLastName(final String lastName) throws FrameworkException {
			
		Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction() {

			@Override
			public Object execute() throws FrameworkException {


				setProperty(Person.lastName, lastName);
				
				String _firstName = getProperty(Person.firstName);

				String firstName = ((_firstName != null) &&!(_firstName.isEmpty()))
						   ? _firstName
						   : "";

				setProperty(AbstractNode.name, lastName + ", " + firstName);
				
				return null;
			}
		});

	}

}
