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
public class EditSubmitButton extends EditTextField
{
	public EditSubmitButton()
	{
		FieldSet formFieldParameter = new FieldSet("Submit Button Parameters");

		formFieldParameter.add(new TextField(FormField.LABEL_KEY, true));

		editPropertiesForm.add(formFieldParameter);
	}
}
