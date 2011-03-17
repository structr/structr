/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.cloud;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Abstract superclass of {@link NodeDataContainer} and {@link RelationshipDataContainer}
 * 
 * @author axel
 */
public abstract class DataContainer implements Serializable {

    protected Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * Return the properties map
     *
     * @return
     */
    public Map getProperties() {
        return properties;
    }


    // <editor-fold defaultstate="collapsed" desc="toString() method">
    /**
     * Implement standard toString() method
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();

        List<String> props = new ArrayList<String>();

        for (String key : properties.keySet()) {

            Object value = properties.get(key);
            String displayValue = "";

            if (value.getClass().isPrimitive()) {
                displayValue = value.toString();
            } else if (value.getClass().isArray()) {

                if (value instanceof byte[]) {

                    displayValue = new String((byte[]) value);

                } else if (value instanceof char[]) {

                    displayValue = new String((char[]) value);

                } else if (value instanceof double[]) {

                    Double[] values = ArrayUtils.toObject((double[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof float[]) {

                    Float[] values = ArrayUtils.toObject((float[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof short[]) {

                    Short[] values = ArrayUtils.toObject((short[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof long[]) {

                    Long[] values = ArrayUtils.toObject((long[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof int[]) {

                    Integer[] values = ArrayUtils.toObject((int[]) value);
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof boolean[]) {

                    Boolean[] values = (Boolean[]) value;
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";

                } else if (value instanceof byte[]) {

                    displayValue = new String((byte[]) value);

                } else {

                    Object[] values = (Object[]) value;
                    displayValue = "[ " + StringUtils.join(values, " , ") + " ]";
                }


            } else {
                displayValue = value.toString();
            }

            props.add("\"" + key + "\"" + " : " + "\"" + displayValue + "\"");

        }

        out.append("{ ").append(StringUtils.join(props.toArray(), " , ")).append(" }");

        return out.toString();
    }// </editor-fold>

}
