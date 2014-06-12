/**
 * Copyright (C) 2010-2014 Morgner UG (haftungsbeschränkt)
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.Date;
import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.ISO8601DateProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author Axel Morgner
 */
public class Person extends AbstractNode {

	public static final Property<String>  salutation          = new StringProperty("salutation");
	public static final Property<String>  firstName           = new StringProperty("firstName").indexed();
	public static final Property<String>  middleNameOrInitial = new StringProperty("middleNameOrInitial");
	public static final Property<String>  lastName            = new StringProperty("lastName").indexed();

	public static final Property<String>  twitterName         = new StringProperty("twitterName").indexed();
	public static final Property<String>  eMail               = new StringProperty("eMail").indexed();
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
		name, salutation, firstName, middleNameOrInitial, lastName, twitterName, eMail, zipCode, city, state, country
	);

	public static final View uiView = new View(Person.class, PropertyView.Ui,
		name, salutation, firstName, middleNameOrInitial, lastName, twitterName, eMail, zipCode, city, state, country
	);

}
