/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.apache.click.control.Panel;

/**
 * Edit html.
 * 
 * @author amorgner
 */
public class EditHtmlSource extends EditPlainText {

    protected Panel editHtmlSourcePanel = new Panel("editHtmlSourcePanel", "/panel/edit-html-source-panel.htm");

    public EditHtmlSource() {

        addControl(editHtmlSourcePanel);
        
    }
}
