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
public class EditHtmlSource extends EditPlainText {

    @Bindable
    protected Panel editHtmlSourcePanel = new Panel("editHtmlSourcePanel", "/panel/edit-html-source-panel.htm");

}
