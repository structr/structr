/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.apache.click.control.Panel;
import org.apache.click.util.*;

/**
 * Edit html.
 * 
 * @author amorgner
 */
public class EditSimpleFeature extends EditPlainText {

    @Bindable
    protected Panel editSimpleFeaturePanel = new Panel("editSimpleFeaturePanel", "/panel/edit-simple-feature-panel.htm");

}
