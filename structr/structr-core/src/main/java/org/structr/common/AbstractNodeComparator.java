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
package org.structr.common;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.Link;

/**
 *
 * @author axel
 */
public class AbstractNodeComparator implements Comparator {

    private static final Logger logger = Logger.getLogger(AbstractNodeComparator.class.getName());
    public static final String ASCENDING = "asc";
    public static final String DESCENDING = "desc";

    private String sortKey;
    private String sortOrder;

    @Override
    public int compare(Object o1, Object o2) {
        return compare((AbstractNode) o1, (AbstractNode) o2);
    }


    //public AbstractNodeComparator() {};

    public AbstractNodeComparator(final String sortKey, final String sortOrder) {
        this.sortKey = sortKey;
        this.sortOrder = sortOrder;
    };
    
    private int compare(AbstractNode nodeOne, AbstractNode nodeTwo) {

        if (nodeOne instanceof Link) {
            nodeOne = ((Link) nodeOne).getStructrNode();
        }

        if (nodeTwo instanceof Link) {
            nodeTwo = ((Link) nodeTwo).getStructrNode();
        }

        Method getterOne = null;
        try {
            getterOne = nodeOne.getClass().getMethod(sortKey);
        } catch (Exception ex) {
            logger.log(Level.FINE, "Cannot invoke method {0}", sortKey);
        }

        Method getterTwo = null;
        try {
            getterTwo = nodeOne.getClass().getMethod(sortKey);
        } catch (Exception ex) {
            logger.log(Level.FINE, "Cannot invoke method {0}", sortKey);
        }
        int result = 0;

        if (getterOne != null && getterTwo != null) {

            Object valueOne = null;
            try {
                valueOne = getterOne.invoke(nodeOne);
            } catch (Exception ex) {
                logger.log(Level.FINE, "Cannot invoke method {0} on {1}", new Object[]{getterOne, nodeOne});
            }
            Object valueTwo = null;
            try {
                valueTwo = getterTwo.invoke(nodeTwo);
            } catch (Exception ex) {
                logger.log(Level.FINE, "Cannot invoke method {0} on {1}", new Object[]{getterTwo, nodeTwo});
            }

            if (valueOne != null && valueTwo != null) {
                if (valueOne instanceof Comparable && valueTwo instanceof Comparable) {

                    if (sortOrder != null && sortOrder.equals(DESCENDING)) {
                        result = ((Comparable) valueOne).compareTo((Comparable) valueTwo);
                    } else {
                        result = ((Comparable) valueTwo).compareTo((Comparable) valueOne);
                    }


                } else {

                    if (sortOrder != null && sortOrder.equals(DESCENDING)) {
                        result = valueOne.toString().compareTo(valueTwo.toString());
                    } else {
                        result = valueTwo.toString().compareTo(valueOne.toString());
                    }
                }
            }

        }

        return result;
    }
}
