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

/**
 * Abstract base class for all web forms
 *
 * @author axel
 */
public abstract class Form extends WebNode {

    /** Form action */
    public final static String ACTION_KEY = "action";
    /** Form label */
    public final static String LABEL_KEY = "label";
    /** CSS Class of form table */
    public final static String CSS_CLASS_KEY = "cssClass";
    /** Name of submit button (must not be empty to process form) */
    public final static String SUBMIT_BUTTON_NAME_KEY = "submitButtonName";
    /** Name of anti robot field (must be empty to process form) */
    public final static String ANTI_ROBOT_FIELD_NAME_KEY = "antiRobotFieldName";
    /** List with parameter names */
    public final static String PARAMETER_NAMES_KEY = "parameterNames";
    /** List with mandatory parameter names */
    public final static String MANDATORY_PARAMETER_NAMES_KEY = "mandatoryParameterNames";
    /** List with strings to be removed from parameter values */
    public final static String STRIP_FROM_VALUES_KEY = "stripFromValues";

    private final static String ICON_SRC = "/images/form.png";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }


    /**
     * Get parameter names
     *
     * @return
     */
    public String getParameterNames() {
        return getStringArrayPropertyAsString(PARAMETER_NAMES_KEY);
    }

    public List<String> getParameterNamesAsList() {
        return getStringListProperty(PARAMETER_NAMES_KEY);
    }

    /**
     * Set parameter names
     *
     * @param stringList
     */
    public void setParameterNames(final String value) {
        setPropertyAsStringArray(PARAMETER_NAMES_KEY, value);
    }

    /**
     * Get mandatory parameter names
     *
     * @return
     */
    public String getMandatoryParameterNames() {
        return getStringArrayPropertyAsString(MANDATORY_PARAMETER_NAMES_KEY);
    }

    public List<String> getMandatoryParameterNamesAsList() {
        return getStringListProperty(MANDATORY_PARAMETER_NAMES_KEY);
    }

    /**
     * Set mandatory parameter names
     *
     * @param stringList
     */
    public void setMandatoryParameterNames(final String value) {
        setPropertyAsStringArray(MANDATORY_PARAMETER_NAMES_KEY, value);
    }

    /**
     * Get strings to be stripped from all parameter values
     *
     * @return
     */
    public String getStripFromValues() {
        return getStringArrayPropertyAsString(STRIP_FROM_VALUES_KEY);
    }

    public List<String> getStripFromValuesAsList() {
        return getStringListProperty(STRIP_FROM_VALUES_KEY);
    }

    /**
     * Set strings to be stripped from all parameter values
     *
     * @param stringList
     */
    public void setStripFromValues(final String value) {
        setPropertyAsStringArray(STRIP_FROM_VALUES_KEY, value);
    }

    /**
     * Return name of anti robot field
     *
     * @return
     */
    public String getAntiRobotFieldName() {
        return getStringProperty(ANTI_ROBOT_FIELD_NAME_KEY);
    }

    /**
     * Set name of anti robot field
     *
     * @param value
     */
    public void setAntiRobotFieldName(final String value) {
        setProperty(ANTI_ROBOT_FIELD_NAME_KEY, value);
    }

    /**
     * Return name of submit button
     *
     * @return
     */
    public String getSubmitButtonName() {
        return getStringProperty(SUBMIT_BUTTON_NAME_KEY);
    }

    /**
     * Set name of submit button
     *
     * @param value
     */
    public void setSubmitButtonName(final String value) {
        setProperty(SUBMIT_BUTTON_NAME_KEY, value);
    }

    /**
     * Return CSS class
     *
     * @return
     */
    public String getCssClass() {
        return getStringProperty(CSS_CLASS_KEY);
    }

    /**
     * Set CSS class
     *
     * @return
     */
    public void setCssClass(final String value) {
        setProperty(CSS_CLASS_KEY, value);
    }

    /**
     * Return action
     *
     * @return
     */
    public String getAction() {
        return getStringProperty(ACTION_KEY);
    }

    /**
     * Set action
     *
     * @param value
     */
    public void setAction(final String value) {
        setProperty(ACTION_KEY, value);
    }

    /**
     * Return label
     *
     * @return
     */
    public String getLabel() {
        return getStringProperty(LABEL_KEY);
    }

    /**
     * Set label
     *
     * @param value
     */
    public void setLabel(final String value) {
        setProperty(LABEL_KEY, value);
    }
}
