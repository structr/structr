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
package org.structr.docs;

import java.util.List;

public interface Documentable {

	/**
	 * Returns the type of this Documentable.
	 *
	 * @return
	 */
	DocumentableType getType();

	/**
	 * Returns the name of this Documentable.
	 *
	 * @return the name
	 */
	String getName();

	/**
	 * Returns the short description of this Documentable. This method
	 * must return a non-null value, otherwise a NullPointerException
	 * will be thrown, because every Documentable needs at least a
	 * short description.
	 *
	 * @return the short description
	 */
	String getShortDescription();

	/**
	 * Returns the long description of this Documentable. This method
	 * may return null or the empty string to indicate that the object
	 * has no long description.
	 *
	 * @return the long description or null
	 */
	String getLongDescription();

	/**
	 * Returns the parameters of this Documentable, or null if no
	 * parameters are defined.
	 *
	 * @return the parameters or null
	 */
	List<Parameter> getParameters();

	/**
	 * Returns examples for this Documentable, or null if no examples
	 * exist.
	 *
	 * @return the examples or null
	 */
	List<Example> getExamples();

	/**
	 * Returns notes for this Documentable, or null if no notes
	 * exist.
	 *
	 * @return the notes or null
	 */
	List<String> getNotes();

	/**
	 * Returns the signatures of this Documentable, or null if no
	 * signatures are defined.
	 *
	 * @return the signatures or null
	 */
	List<Signature> getSignatures();

	/**
	 * Returns the languages for which this Documentable is valid. This
	 * method must return a non-null value, otherwise a NullPointerException
	 * is thrown, because every Documentable must specify the languages
	 * for which it is valid.
	 *
	 * @return the languages
	 */
	List<Language> getLanguages();

	/**
	 * Returns the usages of this Documentable, or null if no usages exist.
	 *
	 * @return the usages or null
	 */
	List<Usage> getUsages();
}
