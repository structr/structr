/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ui.page.admin;

import org.apache.click.control.FieldSet;
import org.apache.click.control.TextArea;
import org.apache.click.control.TextField;
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
        formFields.add(new TextArea(MailSender.PARAMETER_NAMES_KEY, "Template parameters", 60, 10));
        formFields.add(new TextArea(MailSender.MANDATORY_PARAMETER_NAMES_KEY, "Mandatory form fields", 60, 10));
        formFields.add(new TextArea(MailSender.STRIP_FROM_VALUES_KEY, "Strings to be stripped from parameter values", 60, 10));
        formFields.add(new TextField(MailSender.ANTI_ROBOT_FIELD_NAME_KEY, "Name of hidden anti-robot field", 30));
        formFields.add(new TextField(MailSender.SUBMIT_BUTTON_NAME_KEY, "Name of submit button", 30));

        editPropertiesForm.add(formFields);


        FieldSet mailFields = new FieldSet("E-Mail Fields");

        mailFields.add(new TextField(MailSender.TO_ADDRESS_TEMPLATE_KEY, "Template for e-mail 'to address' field", 80));
        mailFields.add(new TextField(MailSender.TO_NAME_TEMPLATE_KEY, "Template for e-mail 'to name' field", 80));

        mailFields.add(new TextField(MailSender.FROM_ADDRESS_TEMPLATE_KEY, "Template for e-mail 'from address' field", 80));
        mailFields.add(new TextField(MailSender.FROM_NAME_TEMPLATE_KEY, "Template for e-mail 'from name' field", 80));

        mailFields.add(new TextField(MailSender.CC_ADDRESS_TEMPLATE_KEY, "Template for e-mail 'cc address' field", 80));
        mailFields.add(new TextField(MailSender.BCC_ADDRESS_TEMPLATE_KEY, "Template for e-mail 'bcc name' field", 80));

        mailFields.add(new TextField(MailSender.MAIL_SUBJECT_TEMPLATE_KEY, "Template for e-mail subject line", 100));
        mailFields.add(new TextArea(MailSender.HTML_BODY_TEMPLATE_KEY, "Template for e-mail body (HTML)", 80, 20));

        editPropertiesForm.add(mailFields);

    }

}
