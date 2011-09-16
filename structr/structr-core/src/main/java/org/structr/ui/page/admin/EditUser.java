/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 *
 *  This file is part of structr <http://structr.org>.
 *
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.ui.page.admin;

import org.apache.click.control.Checkbox;
import org.apache.click.control.FieldSet;
import org.apache.click.control.PasswordField;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.EmailField;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Person;
import org.structr.core.entity.User;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author amorgner
 */
public class EditUser extends DefaultEdit {

	// TODO: implement a table with nodes which the user has a security relationship to
	// TODO: implement a table with any group the user belongs to
	// TODO: implement a form where the user (or an admin) can edit his/her
	// profile, including f.e. an image
	public EditUser() {

		super();

		FieldSet userFields = new FieldSet("User Information");

		userFields.setColumns(2);
		userFields.add(new TextField(AbstractNode.NAME_KEY, true));
		userFields.add(new TextField(User.Key.realName.name(), true));
		userFields.add(new PasswordField(User.Key.password.name()));
		userFields.add(new EmailField(Person.EMAIL_1_KEY, "E-Mail", true));
		userFields.add(new Checkbox(User.Key.backendUser.name()));
		userFields.add(new Checkbox(User.Key.frontendUser.name()));
		userFields.add(new Checkbox(User.Key.blocked.name()));
		editPropertiesForm.add(userFields);
	}
}
