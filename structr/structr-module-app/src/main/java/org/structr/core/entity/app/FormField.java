/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.entity.app;

import org.structr.core.entity.AbstractNode;

/**
 *
 * @author axel
 */
public abstract class FormField extends AbstractNode {
    
    public static final String LABEL_KEY = "label";
    public static final String DESCRIPTION_KEY = "description";
    public static final String HINT_KEY = "hint";
    public static final String HELP_TEXT_KEY = "helpText";

    public abstract String getErrorMessage();
    public abstract Object getErrorValue();
    public abstract void setErrorValue(Object errorValue);

    public String getLabel() {
        return getStringProperty(LABEL_KEY);
    }

    public void setLabel(final String value) {
        setProperty(LABEL_KEY, value);
    }

    public String getDescription() {
        return getStringProperty(DESCRIPTION_KEY);
    }

    public void setDescription(final String value) {
        setProperty(DESCRIPTION_KEY, value);
    }

    public String getHint() {
        return getStringProperty(HINT_KEY);
    }

    public void setHint(final String value) {
        setProperty(HINT_KEY, value);
    }

    public String getHelpText() {
        return getStringProperty(HELP_TEXT_KEY);
    }

    public void setHelpText(final String value) {
        setProperty(HELP_TEXT_KEY, value);
    }

}
