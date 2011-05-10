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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.structr.common.CurrentRequest;

/**
 * Abstract base class for all web forms
 *
 * @author axel
 */
public abstract class Form extends WebNode {

    protected final static String defaultAction = "";
    protected final static String defaultCssClass = "formTable";
    protected final static String defaultLabel = "Login";
    protected final static String defaultSubmitButtonName = "form_submit";
    protected final static String defaultAntiRobotFieldName = "form_antiRobot";

    protected Map parameterMap = new HashMap<String, String>();
    protected StringBuilder errorMsg = new StringBuilder();
    protected StringBuilder errorStyle = new StringBuilder();

    private String submitButtonName;
    private String antiRobotFieldName;

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
    private static final Logger logger = Logger.getLogger(Form.class.getName());

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    protected boolean validateParameters() {

        if (StringUtils.isEmpty(param(submitButtonName))) {
            // Don't process form at all if submit button was not pressed
            return false;
        }

        if (StringUtils.isNotEmpty(param(antiRobotFieldName))) {
            // Don't process form if someone has filled the anti-robot field
            return false;
        }

        // Check mandatory parameters

        errorStyle.append("<style type=\"text/css\">");

        List<String> mandatoryParameterNames = getMandatoryParameterNamesAsList();

        if (mandatoryParameterNames != null) {

            for (String mandatoryParameterName : mandatoryParameterNames) {
                if (StringUtils.isEmpty(param(mandatoryParameterName))) {
                    errorMsg.append("<div class=\"errorMsg\">").append("Please fill out \"").append("<script type=\"text/javascript\">document.write(getLabel('").append(mandatoryParameterName).append("'));</script>\"").append("</div>");
                    errorStyle.append("input[name=").append(mandatoryParameterName).append("] { background-color: #ffc }\n");
                }
            }
        }
        errorStyle.append("</style>");

        return true;
    }

    protected void readParameters() {

           HttpServletRequest request = CurrentRequest.getRequest();

            if (request == null) {
                return;
            }

//            HttpSession session = request.getSession();
//
//            if (session == null) {
//                return;
//            }

        List<String> parameterNames = getParameterNamesAsList();

        // Get values from config page, or defaults
        submitButtonName = getSubmitButtonName() != null ? getSubmitButtonName() : defaultSubmitButtonName;
        antiRobotFieldName = getAntiRobotFieldName() != null ? getAntiRobotFieldName() : defaultAntiRobotFieldName;

        // Static, technical parameters
        parameterMap.put(submitButtonName, request.getParameter(submitButtonName));
        parameterMap.put(antiRobotFieldName, request.getParameter(antiRobotFieldName));

        if (parameterNames != null) {
            for (String parameterName : parameterNames) {

                String parameterValue = request.getParameter(parameterName);

                // Clean values and add to parameter map
                parameterMap.put(parameterName, clean(parameterValue));
            }
        }
    }

    protected String clean(final String input) {

        String output = StringUtils.trimToEmpty(input);

        for (String strip : getStripFromValuesAsList()) {
            output = StringUtils.replace(output, strip, "");
        }

        return output;
    }

    protected String param(final String key) {
        return (parameterMap.containsKey(key) ? (String) parameterMap.get(key) : "");
    }


    /**
     * Get parameter names
     *
     * @return
     */
    public String getParameterNames() {
        return getStringArrayPropertyAsString(PARAMETER_NAMES_KEY);
    }

    private List<String> getParameterNamesAsList() {
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

    private List<String> getMandatoryParameterNamesAsList() {
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

    private List<String> getStripFromValuesAsList() {
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
