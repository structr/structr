/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ui.page.admin;

import org.apache.click.control.Checkbox;
import org.apache.click.control.FieldSet;
import org.apache.click.control.PasswordField;
import org.apache.click.control.TextField;
import org.apache.click.extras.control.EmailField;
import org.structr.core.entity.Person;
import org.structr.core.entity.User;

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

        FieldSet userFields = new FieldSet("User Information");

        userFields.add(new TextField(User.REAL_NAME_KEY));
        userFields.add(new PasswordField(User.PASSWORD_KEY));
        userFields.add(new EmailField(Person.EMAIL_1_KEY, "E-Mail"));

        // FIXME: support password change with hidden password,
        // so that password is not set to the default 'XXXXXXXX'
        // TODO: test, if fix works
        Checkbox blocked = new Checkbox(User.BLOCKED_KEY);
        userFields.add(blocked);
        
        editPropertiesForm.add(userFields);



    }

}
