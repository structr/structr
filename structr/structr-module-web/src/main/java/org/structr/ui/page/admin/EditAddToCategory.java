/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ui.page.admin;

import org.apache.click.control.FieldSet;
import org.apache.click.control.TextField;
import org.structr.core.entity.web.AddToCategory;

/**
 * Edit properties of {@link AddToCategory} entity.
 *
 * @author axel
 */
public class EditAddToCategory extends DefaultEdit {
    
    public EditAddToCategory() {

        super();

        FieldSet formFields = new FieldSet("Form Parameter");
        formFields.add(new TextField(AddToCategory.CATEGORY_PARAMETER_NAME_KEY, "Category Parameter", 30));
        editPropertiesForm.add(formFields);

    }

}
