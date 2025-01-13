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
package org.structr.core.traits.definitions;

import org.structr.core.entity.Person;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.wrappers.PersonTraitWrapper;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 */
public class PersonTraitDefinition extends AbstractTraitDefinition {

	/*
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
	*/

	public PersonTraitDefinition() {
		super("Person");
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Person.class, (traits, node) -> new PersonTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> salutationProperty          = new StringProperty("salutation");
		final Property<String> firstNameProperty           = new StringProperty("firstName");
		final Property<String> middleNameOrInitialProperty = new StringProperty("middleNameOrInitial");
		final Property<String> lastNameProperty            = new StringProperty("lastName");
		final Property<String> eMailProperty               = new StringProperty("eMail");
		final Property<String> eMail2Property              = new StringProperty("eMail2");
		final Property<String> phoneNumber1Property        = new StringProperty("phoneNumber1");
		final Property<String> phoneNumber2Property        = new StringProperty("phoneNumber2");
		final Property<String> faxNumber1Property          = new StringProperty("faxNumber1");
		final Property<String> faxNumber2Property          = new StringProperty("faxNumber2");
		final Property<String> countryProperty             = new StringProperty("country");
		final Property<String> streetProperty              = new StringProperty("street");
		final Property<String> zipCodeProperty             = new StringProperty("zipCode");
		final Property<String> cityProperty                = new StringProperty("city");
		final Property<String> stateProperty               = new StringProperty("state");
		final Property<Date> birthdayProperty              = new DateProperty("birthday");
		final Property<String> genderProperty              = new StringProperty("gender");
		final Property<Boolean> newsletterProperty         = new BooleanProperty("newsletter");

		return Set.of(
			salutationProperty,
			firstNameProperty,
			middleNameOrInitialProperty,
			lastNameProperty,
			eMailProperty,
			eMail2Property,
			phoneNumber1Property,
			phoneNumber2Property,
			faxNumber1Property,
			faxNumber2Property,
			countryProperty,
			streetProperty,
			zipCodeProperty,
			cityProperty,
			stateProperty,
			birthdayProperty,
			genderProperty,
			newsletterProperty
		);
	}
}
