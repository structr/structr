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

import org.apache.click.control.Field;
import org.apache.click.control.FieldSet;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.EmailField;
import org.apache.click.extras.control.TelephoneField;
import org.structr.core.entity.Person;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author amorgner
 */
public class EditPerson extends DefaultEdit {

    protected FieldSet personFields = new FieldSet("Personal Information");

    public EditPerson() {

        super();


        personFields.setColumns(2);
        personFields.add(new TextField(Person.Key.salutation.name()));
        personFields.add(new TextField(Person.Key.firstName.name()));
	
        personFields.add(new TextField(Person.Key.middleNameOrInitial.name(), "Middle Name or Initial"));
        personFields.add(new TextField(Person.Key.lastName.name()));

        personFields.add(new EmailField(Person.Key.email.name(), "E-mail"));
        personFields.add(new EmailField(Person.Key.email2.name(), "2nd E-mail"));

        personFields.add(new TelephoneField(Person.Key.phoneNumber1.name(), "Phone"));
        personFields.add(new TelephoneField(Person.Key.phoneNumber2.name(), "2nd Phone"));

        personFields.add(new TelephoneField(Person.Key.faxNumber1.name(), "Fax"));
        personFields.add(new TelephoneField(Person.Key.faxNumber2.name(), "2nd Fax"));

	editPropertiesForm.add(personFields);



    }

    @Override
    public void onInit() {

        super.onInit();

        // make the name field read only,
        // value is settable via getFirstName/getLastName methods only
        Field nameField = editPropertiesForm.getField(AbstractNode.Key.name.name());
        nameField.setReadonly(true);
    }
}
