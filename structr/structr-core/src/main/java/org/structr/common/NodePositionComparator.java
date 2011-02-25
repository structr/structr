/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import java.util.Comparator;
import org.structr.core.entity.AbstractNode;

/**
 *
 * @author cmorgner
 */
public class NodePositionComparator implements Comparator<AbstractNode> {

    @Override
    public int compare(AbstractNode o1, AbstractNode o2) {
        return (o1.getPosition().compareTo(o2.getPosition()));
    }
}
