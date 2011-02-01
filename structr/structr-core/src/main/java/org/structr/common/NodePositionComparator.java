/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.common;

import java.util.Comparator;
import org.structr.core.entity.StructrNode;

/**
 *
 * @author cmorgner
 */
public class NodePositionComparator implements Comparator<StructrNode> {

    @Override
    public int compare(StructrNode o1, StructrNode o2) {
        return (o1.getPosition().compareTo(o2.getPosition()));
    }
}
