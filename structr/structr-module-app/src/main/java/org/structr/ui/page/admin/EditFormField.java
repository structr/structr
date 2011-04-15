/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.apache.click.control.FieldSet;
import org.apache.click.control.TextField;
import org.structr.core.entity.app.FormField;

/**
 *
 * @author axel
 */
public class EditFormField extends DefaultEdit {

    public EditFormField() {

        FieldSet formFieldParameter = new FieldSet("Form Field Parameter");

        formFieldParameter.add(new TextField(FormField.LABEL_KEY, true));
        formFieldParameter.add(new TextField(FormField.DESCRIPTION_KEY));
        formFieldParameter.add(new TextField(FormField.HINT_KEY));
        formFieldParameter.add(new TextField(FormField.HELP_TEXT_KEY));

        editPropertiesForm.add(formFieldParameter);

    }
}
