/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.apache.click.control.Panel;
import org.apache.click.util.*;

/**
 * Edit xml.
 * 
 * @author amorgner
 */
public class EditXml extends EditPlainText {

    @Bindable
    protected Panel editXmlPanel = new Panel("editXmlPanel", "/panel/edit-xml-panel.htm");

}
