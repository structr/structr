/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.entity.app;

import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 *
 * @author Christian Morgner
 */
public class SubmitButton extends FormField {

    public SubmitButton() {
    }

    @Override
    public void renderView(StringBuilder out, AbstractNode startNode, String editUrl, Long editNodeId, User user) {

        String name = getName();
        String label = getLabel();


        out.append("<input type=\"submit\"");
        out.append(" name=\"").append(name).append("\"");

        if (label != null) {
            out.append(" value=\"").append(label).append("\"");
        }
        
        out.append(">");
    }

    @Override
    public String getIconSrc() {
        return "/images/tag.png";
    }

	@Override
	public String getErrorMessage()
	{
		return(null);
	}

	@Override
	public Object getErrorValue()
	{
		return(null);
	}

	@Override
	public void setErrorValue(Object errorValue)
	{
	}
}
