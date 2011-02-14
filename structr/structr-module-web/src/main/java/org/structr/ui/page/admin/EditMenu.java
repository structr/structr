/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.ui.page.admin;

import org.apache.click.extras.control.IntegerField;
import org.structr.core.entity.web.Menu;

/**
 *
 * @author amorgner
 */
public class EditMenu extends DefaultEdit {

    public EditMenu() {

        editPropertiesForm.add(new IntegerField(Menu.MAX_DEPTH_KEY, "Max. Menu Depth"));

    }

}
