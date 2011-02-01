/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ui.page.admin;

import org.apache.click.extras.control.IntegerField;
import org.structr.core.entity.web.Menu;
import org.structr.ui.page.admin.DefaultEdit;
import org.structr.ui.page.admin.DefaultEdit;

/**
 *
 * @author amorgner
 */
public class EditMenu extends DefaultEdit {

    public EditMenu() {

        editPropertiesForm.add(new IntegerField(Menu.MAX_DEPTH_KEY, "Max. Menu Depth"));

    }

}
