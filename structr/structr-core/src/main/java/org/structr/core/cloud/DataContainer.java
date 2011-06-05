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

package org.structr.core.cloud;

import java.io.Serializable;
import java.util.LinkedList;
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
     * Estimated size, needed to calculate buffer size.
     *
     * Start with a reasonable size, say 1K
     */
    protected int estimatedSize = 1024;

    /**
     * Return the properties map
     *
     * @return
     */
    public Map getProperties() {
        return properties;
    }

    public int getEstimatedSize() {
        return estimatedSize;
    }

    // <editor-fold defaultstate="collapsed" desc="toString() method">
    /**
     * Implement standard toString() method
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();

        List<String> props = new LinkedList<String>();

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
