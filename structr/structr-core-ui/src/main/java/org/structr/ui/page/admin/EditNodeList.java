/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.ui.page.admin;

import org.apache.click.control.ActionLink;
import org.apache.click.control.FieldSet;

/**
 *
 * @author chrisi
 */
public class EditNodeList extends DefaultEdit
{
	protected FieldSet fields = new FieldSet("NodeList test");

	public EditNodeList()
	{
		fields.add(new ActionLink("test", "Test", this, "onTestClick"));

		editPropertiesForm.add(fields);
	}

	public void onTestClick()
	{
		
	}
}
