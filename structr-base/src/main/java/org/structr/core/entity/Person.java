/*
 * Copyright (C) 2010-2024 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.common.View;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.DateProperty;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;

import java.util.Date;

/**
 */
public class Person extends AbstractNode {

	public static final Property<String> salutationProperty          = new StringProperty("salutation").partOfBuiltInSchema();
	public static final Property<String> firstNameProperty           = new StringProperty("firstName").partOfBuiltInSchema();
	public static final Property<String> middleNameOrInitialProperty = new StringProperty("middleNameOrInitial").partOfBuiltInSchema();
	public static final Property<String> lastNameProperty            = new StringProperty("lastName").partOfBuiltInSchema();
	public static final Property<String> eMailProperty               = new StringProperty("eMail").partOfBuiltInSchema();
	public static final Property<String> eMail2Property              = new StringProperty("eMail2").partOfBuiltInSchema();
	public static final Property<String> phoneNumber1Property        = new StringProperty("phoneNumber1").partOfBuiltInSchema();
	public static final Property<String> phoneNumber2Property        = new StringProperty("phoneNumber2").partOfBuiltInSchema();
	public static final Property<String> faxNumber1Property          = new StringProperty("faxNumber1").partOfBuiltInSchema();
	public static final Property<String> faxNumber2Property          = new StringProperty("faxNumber2").partOfBuiltInSchema();
	public static final Property<String> countryProperty             = new StringProperty("country").partOfBuiltInSchema();
	public static final Property<String> streetProperty              = new StringProperty("street").partOfBuiltInSchema();
	public static final Property<String> zipCodeProperty             = new StringProperty("zipCode").partOfBuiltInSchema();
	public static final Property<String> cityProperty                = new StringProperty("city").partOfBuiltInSchema();
	public static final Property<String> stateProperty               = new StringProperty("state").partOfBuiltInSchema();
	public static final Property<Date> birthdayProperty              = new DateProperty("birthday").partOfBuiltInSchema();
	public static final Property<String> genderProperty              = new StringProperty("gender").partOfBuiltInSchema();
	public static final Property<Boolean> newsletterProperty         = new BooleanProperty("newsletter").partOfBuiltInSchema();

	public static final View defaultView = new View(Person.class, PropertyView.Public,
		salutationProperty, firstNameProperty, middleNameOrInitialProperty, lastNameProperty,
		eMailProperty, countryProperty, streetProperty, zipCodeProperty, cityProperty, stateProperty
	);

	public static final View uiView  = new View(Person.class, PropertyView.Ui,
		salutationProperty, firstNameProperty, middleNameOrInitialProperty, lastNameProperty,
		eMailProperty, eMail2Property, phoneNumber1Property, phoneNumber2Property, faxNumber1Property, faxNumber2Property,
		countryProperty, streetProperty, zipCodeProperty, cityProperty, stateProperty,
		birthdayProperty, genderProperty, newsletterProperty
	);
}
