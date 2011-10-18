/*
 *  Copyright (C) 2011 Axel Morgner, structr <structr@structr.org>
 * 
 *  This file is part of structr <http://structr.org>.
 * 
 *  structr is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  structr is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.structr.core.entity.web;

import java.util.List;
import org.structr.common.PropertyKey;
import org.structr.common.PropertyView;
import org.structr.core.EntityContext;

/**
 * Abstract base class for all web forms
 *
 * @author axel
 */
public abstract class Form extends WebNode {


	static {

		EntityContext.registerPropertySet(Form.class,
						  PropertyView.All,
						  Key.values());
	}

	public enum Key implements PropertyKey {
		action, label, cssClass, submitButtonName,
		antiRobotFieldName, parameterNames, mandatoryParameterNames,
		stripFromValues;
	}



    @Override
    public String getIconSrc() {
        return "/images/form.png";
    }


    /**
     * Get parameter names
     *
     * @return
     */
    public String getParameterNames() {
        return getStringArrayPropertyAsString(Key.parameterNames.name());
    }

    public List<String> getParameterNamesAsList() {
        return getStringListProperty(Key.parameterNames.name());
    }

    /**
     * Set parameter names
     *
     * @param stringList
     */
    public void setParameterNames(final String value) {
        setPropertyAsStringArray(Key.parameterNames.name(), value);
    }

    /**
     * Get mandatory parameter names
     *
     * @return
     */
    public String getMandatoryParameterNames() {
        return getStringArrayPropertyAsString(Key.mandatoryParameterNames.name());
    }

    public List<String> getMandatoryParameterNamesAsList() {
        return getStringListProperty(Key.mandatoryParameterNames.name());
    }

    /**
     * Set mandatory parameter names
     *
     * @param stringList
     */
    public void setMandatoryParameterNames(final String value) {
        setPropertyAsStringArray(Key.mandatoryParameterNames.name(), value);
    }

    /**
     * Get strings to be stripped from all parameter values
     *
     * @return
     */
    public String getStripFromValues() {
        return getStringArrayPropertyAsString(Key.stripFromValues.name());
    }

    public List<String> getStripFromValuesAsList() {
        return getStringListProperty(Key.stripFromValues.name());
    }

    /**
     * Set strings to be stripped from all parameter values
     *
     * @param stringList
     */
    public void setStripFromValues(final String value) {
        setPropertyAsStringArray(Key.stripFromValues.name(), value);
    }

    /**
     * Return name of anti robot field
     *
     * @return
     */
    public String getAntiRobotFieldName() {
        return getStringProperty(Key.antiRobotFieldName.name());
    }

    /**
     * Set name of anti robot field
     *
     * @param value
     */
    public void setAntiRobotFieldName(final String value) {
        setProperty(Key.antiRobotFieldName.name(), value);
    }

    /**
     * Return name of submit button
     *
     * @return
     */
    public String getSubmitButtonName() {
        return getStringProperty(Key.submitButtonName.name());
    }

    /**
     * Set name of submit button
     *
     * @param value
     */
    public void setSubmitButtonName(final String value) {
        setProperty(Key.submitButtonName.name(), value);
    }

    /**
     * Return CSS class
     *
     * @return
     */
    public String getCssClass() {
        return getStringProperty(Key.cssClass.name());
    }

    /**
     * Set CSS class
     *
     * @return
     */
    public void setCssClass(final String value) {
        setProperty(Key.cssClass.name(), value);
    }

    /**
     * Return action
     *
     * @return
     */
    public String getAction() {
        return getStringProperty(Key.action.name());
    }

    /**
     * Set action
     *
     * @param value
     */
    public void setAction(final String value) {
        setProperty(Key.action.name(), value);
    }

    /**
     * Return label
     *
     * @return
     */
    public String getLabel() {
        return getStringProperty(Key.label.name());
    }

    /**
     * Set label
     *
     * @param value
     */
    public void setLabel(final String value) {
        setProperty(Key.label.name(), value);
    }
}
