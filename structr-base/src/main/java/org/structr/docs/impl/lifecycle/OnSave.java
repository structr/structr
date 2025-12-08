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
package org.structr.docs.impl.lifecycle;

import org.structr.docs.Example;

import java.util.List;

public class OnSave extends LifecycleBase {

	public OnSave() {
		super("onSave");
	}

	@Override
	public String getShortDescription() {
		return "Called when an existing object of this type is modified.";
	}

	@Override
	public String getLongDescription() {
		return """
		The `onSave()` lifecycle method is called when an existing object of this type is modified. This method runs at the end of a transaction, but **before** property constraints etc. are evaluated.
		
		If you throw an error in this method, the enclosing transaction will be rolled back and nothing will be written to the database.
		
		If you want to execute code after successful validation, implement the `afterSave()` callback method.
		""";
	}

	@Override
	public List<Example> getExamples() {
		return List.of(
			Example.javaScript("""
			{
				if ($.this.name === 'foo') {
			
					// don't allow deletion of nodes named "foo"
					$.error('name', 'save_not_allowed', 'Name can\\'t be changed to "foo"');
			
				} else {
			
					$.log('Node with name ' + $.this.name + ' has been modified.');
				}
			 }
			""", "")
		);
	}

	@Override
	public List<String> getNotes() {

		return List.of(
			"See also: `afterSave()`, `error()` and `assert()`."
		);
	}
}
