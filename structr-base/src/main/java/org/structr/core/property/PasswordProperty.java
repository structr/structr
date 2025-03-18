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
package org.structr.core.property;

import org.apache.commons.lang3.RandomStringUtils;
import org.structr.api.config.Settings;
import org.structr.common.SecurityContext;
import org.structr.common.error.*;
import org.structr.core.GraphObject;
import org.structr.core.auth.HashHelper;
import org.structr.core.converter.ValidationInfo;
import org.structr.core.entity.Principal;
import org.structr.core.graph.CreationContainer;
import org.structr.core.graph.NodeInterface;
import org.structr.core.traits.StructrTraits;
import org.structr.core.traits.Trait;
import org.structr.core.traits.Traits;

import java.util.Date;
import org.structr.core.traits.definitions.PrincipalTraitDefinition;

/**
 * A {@link StringProperty} that converts its value to a hexadecimal SHA512 hash upon storage.
 * The return value of this property will always be the password hash, the clear-text password
 * will be lost.
 *
 *
 */
public class PasswordProperty extends StringProperty {

	private ValidationInfo validationInfo = null;

	public PasswordProperty(String name) {
		this(name, null);
	}

	public PasswordProperty(String name, ValidationInfo info) {
		super(name);

		this.validationInfo = info;
	}

	@Override
	public void registrationCallback(final Trait trait) {

		if (validationInfo != null && validationInfo.getErrorKey() == null) {
			validationInfo.setErrorKey(this);
		}
	}

	@Override
	public String typeName() {
		return "String";
	}

	@Override
	public Object setProperty(SecurityContext securityContext, GraphObject obj, String clearTextPassword) throws FrameworkException {

		final Traits traits = Traits.of(StructrTraits.PRINCIPAL);
		final Object returnValue;
		GraphObject wrappedObject = null;

		if (clearTextPassword != null) {

			if (validationInfo != null) {

				String errorType     = validationInfo.getErrorType();
				PropertyKey errorKey = validationInfo.getErrorKey();
				int minLength        = validationInfo.getMinLength();

				if (minLength > 0 && clearTextPassword.length() < minLength) {

					throw new FrameworkException(422, "Validation of entity with ID " + obj.getUuid() + " failed", new TooShortToken(errorType, errorKey.jsonName(), minLength));
				}
			}


			if (obj instanceof CreationContainer) {

				wrappedObject = ((CreationContainer)obj).getWrappedObject();

				if (wrappedObject != null && wrappedObject instanceof NodeInterface node) {

					final Principal principal   = node.as(Principal.class);
					final String oldSalt        = principal.getSalt();
					final String oldEncPassword = principal.getEncryptedPassword();

					boolean passwordChangedOrFirstPassword = (oldEncPassword == null || oldSalt == null || !oldEncPassword.equals(HashHelper.getHash(clearTextPassword, oldSalt)));
					if (passwordChangedOrFirstPassword) {

						obj.setProperty(traits.key(PrincipalTraitDefinition.PASSWORD_CHANGE_DATE_PROPERTY), new Date().getTime());
					}
				}
			}

			final String salt = RandomStringUtils.randomAlphanumeric(16);

			obj.setProperty(traits.key(PrincipalTraitDefinition.SALT_PROPERTY), salt);

			returnValue = super.setProperty(securityContext, obj, HashHelper.getHash(clearTextPassword, salt));

			if (Settings.PasswordClearSessionsOnChange.getValue() && wrappedObject != null && wrappedObject.is(StructrTraits.PRINCIPAL)) {
				wrappedObject.removeProperty(traits.key(PrincipalTraitDefinition.SESSION_IDS_PROPERTY));
			}

		} else {

			returnValue = super.setProperty(securityContext, obj, null);
		}

		if (Settings.PasswordClearSessionsOnChange.getValue() && wrappedObject != null && wrappedObject.is(StructrTraits.PRINCIPAL)) {
			wrappedObject.removeProperty(traits.key(PrincipalTraitDefinition.SESSION_IDS_PROPERTY));
		}

		if (Settings.PasswordComplexityEnforce.getValue()) {
			checkPasswordPolicy(obj, clearTextPassword);
		}

		return returnValue;
	}

	private void checkPasswordPolicy (GraphObject obj, final String clearTextPassword) throws FrameworkException {

		final String passwordToCheck  = clearTextPassword == null ? "" : clearTextPassword;
		final ErrorBuffer errorBuffer = new ErrorBuffer();

		final int passwordMinLength             = Settings.PasswordComplexityMinLength.getValue();
		final boolean enforceMinUpperCase       = Settings.PasswordComplexityRequireUpperCase.getValue();
		final boolean enforceMinLowerCase       = Settings.PasswordComplexityRequireLowerCase.getValue();
		final boolean enforceMinDigits          = Settings.PasswordComplexityRequireDigit.getValue();
		final boolean enforceMinNonAlphaNumeric = Settings.PasswordComplexityRequireNonAlphaNumeric.getValue();

		final String passwordWithoutUpperCase   = passwordToCheck.replaceAll("[A-Z]", "");
		final String passwordWithoutLowerCase   = passwordToCheck.replaceAll("[a-z]", "");
		final String passwordWithoutDigits      = passwordToCheck.replaceAll("[0-9]", "");

		final int passwordLength                = passwordToCheck.length();
		final int upperCaseCharactersInPassword = passwordLength - passwordWithoutUpperCase.length();
		final int lowerCaseCharactersInPassword = passwordLength - passwordWithoutLowerCase.length();
		final int digitsInPassword              = passwordLength - passwordWithoutDigits.length();
		final int otherCharactersInPassword     = (passwordLength - upperCaseCharactersInPassword - lowerCaseCharactersInPassword - digitsInPassword);

		if (passwordLength < passwordMinLength) {
			errorBuffer.add(new TooShortToken(StructrTraits.USER, PrincipalTraitDefinition.PASSWORD_PROPERTY, passwordMinLength));
		}

		if (enforceMinUpperCase && upperCaseCharactersInPassword == 0) {
			errorBuffer.add(new SemanticErrorToken(StructrTraits.USER, PrincipalTraitDefinition.PASSWORD_PROPERTY, "must_contain_uppercase"));
		}

		if (enforceMinLowerCase && lowerCaseCharactersInPassword == 0) {
			errorBuffer.add(new SemanticErrorToken(StructrTraits.USER, PrincipalTraitDefinition.PASSWORD_PROPERTY, "must_contain_lowercase"));
		}

		if (enforceMinDigits && digitsInPassword == 0) {
			errorBuffer.add(new SemanticErrorToken(StructrTraits.USER, PrincipalTraitDefinition.PASSWORD_PROPERTY, "must_contain_digits"));
		}

		if (enforceMinNonAlphaNumeric && otherCharactersInPassword == 0) {
			errorBuffer.add(new SemanticErrorToken(StructrTraits.USER, PrincipalTraitDefinition.PASSWORD_PROPERTY, "must_contain_non_alpha_numeric"));
		}

		if (errorBuffer.hasError()) {

			if (((CreationContainer) obj).getWrappedObject() == null) {
				throw new PasswordPolicyViolationException(422, "Password policy violation prevented creation of entity with ID " + ((CreationContainer<?>) obj).getData().get("id") + "!", errorBuffer);
			} else {
				throw new PasswordPolicyViolationException(422, "Password policy violation prevented password change on entity with ID " + ((CreationContainer) obj).getWrappedObject().getUuid() + "!", errorBuffer);
			}
		}
	}
}
