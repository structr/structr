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

import org.apache.click.control.FieldSet;
import org.apache.click.control.TextArea;
import org.apache.click.control.TextField;
import org.structr.core.entity.web.Form;
import org.structr.core.entity.web.MailSender;

/**
 * Edit properties of {@link MailSender} entity.
 *
 * @author axel
 */
public class EditMailSender extends DefaultEdit {
    
    public EditMailSender() {

        super();

        FieldSet formFields = new FieldSet("Form Parameter");
        formFields.add(new TextArea(Form.Key.parameterNames.name(), "Template parameters", 60, 10));
        formFields.add(new TextArea(Form.Key.mandatoryParameterNames.name(), "Mandatory form fields", 60, 10));
        formFields.add(new TextArea(Form.Key.stripFromValues.name(), "Strings to be stripped from parameter values", 60, 10));
        formFields.add(new TextField(Form.Key.antiRobotFieldName.name(), "Name of hidden anti-robot field", 30));
        formFields.add(new TextField(Form.Key.submitButtonName.name(), "Name of submit button", 30));

        editPropertiesForm.add(formFields);


        FieldSet mailFields = new FieldSet("E-Mail Fields");

        mailFields.add(new TextField(MailSender.Key.toAddressTemplate.name(), "Template for e-mail 'to address' field", 80));
        mailFields.add(new TextField(MailSender.Key.toNameTemplate.name(), "Template for e-mail 'to name' field", 80));

        mailFields.add(new TextField(MailSender.Key.fromAddressTemplate.name(), "Template for e-mail 'from address' field", 80));
        mailFields.add(new TextField(MailSender.Key.fromNameTemplate.name(), "Template for e-mail 'from name' field", 80));

        mailFields.add(new TextField(MailSender.Key.ccAddressTemplate.name(), "Template for e-mail 'cc address' field", 80));
        mailFields.add(new TextField(MailSender.Key.bccAddressTemplate.name(), "Template for e-mail 'bcc name' field", 80));

        mailFields.add(new TextField(MailSender.Key.mailSubjectTemplate.name(), "Template for e-mail subject line", 100));
        mailFields.add(new TextArea(MailSender.Key.htmlBodyTemplate.name(), "Template for e-mail body (HTML)", 80, 20));

        editPropertiesForm.add(mailFields);

    }

}
