/*
 * Copyright (C) 2010-2025 Structr GmbH
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

import org.structr.common.PropertyView;
import org.structr.core.entity.Person;
import org.structr.core.entity.Relation;
import org.structr.core.property.*;
import org.structr.core.traits.NodeTraitFactory;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.wrappers.PersonTraitWrapper;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 */
public class PersonTraitDefinition extends AbstractNodeTraitDefinition {

	public static final String SALUTATION_PROPERTY             = "salutation";
	public static final String FIRST_NAME_PROPERTY             = "firstName";
	public static final String MIDDLE_NAME_OR_INITIAL_PROPERTY = "middleNameOrInitial";
	public static final String LASTNAME_PROPERTY               = "lastName";
	public static final String EMAIL_PROPERTY                  = "eMail";
	public static final String EMAIL2_PROPERTY                 = "eMail2";
	public static final String PHONE_NUMBER1_PROPERTY          = "phoneNumber1";
	public static final String PHONE_NUMBER2_PROPERTY          = "phoneNumber2";
	public static final String FAX_NUMBER1_PROPERTY            = "faxNumber1";
	public static final String FAX_NUMBER2_PROPERTY            = "faxNumber2";
	public static final String COUNTRY_PROPERTY                = "country";
	public static final String STREET_PROPERTY                 = "street";
	public static final String ZIP_CODE_PROPERTY               = "zipCode";
	public static final String CITY_PROPERTY                   = "city";
	public static final String STATE_PROPERTY                  = "state";
	public static final String BIRTHDAY_PROPERTY               = "birthday";
	public static final String GENDER_PROPERTY                 = "gender";
	public static final String NEWSLETTER_PROPERTY             = "newsletter";

	public PersonTraitDefinition() {
		super(StructrTraits.PERSON);
	}

	@Override
	public Map<Class, NodeTraitFactory> getNodeTraitFactories() {

		return Map.of(
			Person.class, (traits, node) -> new PersonTraitWrapper(traits, node)
		);
	}

	@Override
	public Set<PropertyKey> getPropertyKeys() {

		final Property<String> salutationProperty          = new StringProperty(SALUTATION_PROPERTY);
		final Property<String> firstNameProperty           = new StringProperty(FIRST_NAME_PROPERTY);
		final Property<String> middleNameOrInitialProperty = new StringProperty(MIDDLE_NAME_OR_INITIAL_PROPERTY);
		final Property<String> lastNameProperty            = new StringProperty(LASTNAME_PROPERTY);
		final Property<String> eMailProperty               = new StringProperty(EMAIL_PROPERTY);
		final Property<String> eMail2Property              = new StringProperty(EMAIL2_PROPERTY);
		final Property<String> phoneNumber1Property        = new StringProperty(PHONE_NUMBER1_PROPERTY);
		final Property<String> phoneNumber2Property        = new StringProperty(PHONE_NUMBER2_PROPERTY);
		final Property<String> faxNumber1Property          = new StringProperty(FAX_NUMBER1_PROPERTY);
		final Property<String> faxNumber2Property          = new StringProperty(FAX_NUMBER2_PROPERTY);
		final Property<String> countryProperty             = new StringProperty(COUNTRY_PROPERTY);
		final Property<String> streetProperty              = new StringProperty(STREET_PROPERTY);
		final Property<String> zipCodeProperty             = new StringProperty(ZIP_CODE_PROPERTY);
		final Property<String> cityProperty                = new StringProperty(CITY_PROPERTY);
		final Property<String> stateProperty               = new StringProperty(STATE_PROPERTY);
		final Property<Date> birthdayProperty              = new DateProperty(BIRTHDAY_PROPERTY);
		final Property<String> genderProperty              = new StringProperty(GENDER_PROPERTY);
		final Property<Boolean> newsletterProperty         = new BooleanProperty(NEWSLETTER_PROPERTY);

		return newSet(
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

	@Override
	public Map<String, Set<String>> getViews() {

		return Map.of(

			PropertyView.Public,
			newSet(
					SALUTATION_PROPERTY, FIRST_NAME_PROPERTY, MIDDLE_NAME_OR_INITIAL_PROPERTY, LASTNAME_PROPERTY,
					EMAIL_PROPERTY, COUNTRY_PROPERTY, STREET_PROPERTY, ZIP_CODE_PROPERTY, CITY_PROPERTY, STATE_PROPERTY
			),
			PropertyView.Ui,
			newSet(
					SALUTATION_PROPERTY, FIRST_NAME_PROPERTY, MIDDLE_NAME_OR_INITIAL_PROPERTY, LASTNAME_PROPERTY,
					EMAIL_PROPERTY, EMAIL2_PROPERTY, PHONE_NUMBER1_PROPERTY, PHONE_NUMBER2_PROPERTY, FAX_NUMBER1_PROPERTY, FAX_NUMBER2_PROPERTY,
					COUNTRY_PROPERTY, STREET_PROPERTY, ZIP_CODE_PROPERTY, CITY_PROPERTY, STATE_PROPERTY,
					BIRTHDAY_PROPERTY, GENDER_PROPERTY, NEWSLETTER_PROPERTY
			)
		);
	}

	@Override
	public Relation getRelation() {
		return null;
	}
}
