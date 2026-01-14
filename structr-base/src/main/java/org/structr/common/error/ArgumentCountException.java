/*
 * Copyright (C) 2010-2026 Structr GmbH
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
package org.structr.common.error;


public class ArgumentCountException extends IllegalArgumentException {

	public ArgumentCountException(final String message) {
		super(message);
	}

	public static ArgumentCountException notBetween(final Integer actualLength, final Integer expectedMinLength, final Integer expectedMaxLength){
		return new ArgumentCountException("Expected between " + expectedMinLength + " and " + expectedMaxLength + " arguments but got " + actualLength);
	}

	public static ArgumentCountException notEqual(final Integer actualLength, final Integer expectedLength){
		return new ArgumentCountException("Expected exactly " + expectedLength + " arguments but got " + actualLength);
	}

	public static ArgumentCountException tooFew(final Integer actualLength, final Integer expectedMinLength){
		return new ArgumentCountException("Expected at least " + expectedMinLength + " arguments but got " + actualLength);
	}

}
