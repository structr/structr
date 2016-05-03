/**
 * Copyright (C) 2010-2016 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
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
 *
 */
public class Person extends AbstractNode {

	public static final Property<String>  salutation          = new StringProperty("salutation").cmis();
	public static final Property<String>  firstName           = new StringProperty("firstName").cmis().indexed();
	public static final Property<String>  middleNameOrInitial = new StringProperty("middleNameOrInitial").cmis();
	public static final Property<String>  lastName            = new StringProperty("lastName").cmis().indexed();

	public static final Property<String>  twitterName         = new StringProperty("twitterName").cmis().indexed();
	public static final Property<String>  eMail               = new StringProperty("eMail").cmis().indexed();
	public static final Property<String>  eMail2              = new StringProperty("eMail2").cmis();

	public static final Property<String>  phoneNumber1        = new StringProperty("phoneNumber1").cmis();
	public static final Property<String>  phoneNumber2        = new StringProperty("phoneNumber2").cmis();
	public static final Property<String>  faxNumber1          = new StringProperty("faxNumber1").cmis();
	public static final Property<String>  faxNumber2          = new StringProperty("faxNumber2").cmis();

	public static final Property<String>  street              = new StringProperty("street").cmis().indexed();
	public static final Property<String>  zipCode             = new StringProperty("zipCode").cmis().indexed();
	public static final Property<String>  city                = new StringProperty("city").cmis().indexed();
	public static final Property<String>  state               = new StringProperty("state").cmis().indexed();
	public static final Property<String>  country             = new StringProperty("country").cmis().indexed();

	public static final Property<Date>    birthday            = new ISO8601DateProperty("birthday").cmis();
	public static final Property<String>  gender              = new StringProperty("gender").cmis();
	public static final Property<Boolean> newsletter          = new BooleanProperty("newsletter").cmis();

	public static final View publicView = new View(Person.class, PropertyView.Public,
		name, salutation, firstName, middleNameOrInitial, lastName, twitterName, eMail, zipCode, city, state, country
	);

	public static final View uiView = new View(Person.class, PropertyView.Ui,
		name, salutation, firstName, middleNameOrInitial, lastName, twitterName, eMail, zipCode, city, state, country
	);

}
