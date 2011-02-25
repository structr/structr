/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.agent;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.User;

/**
 * Use this interface for tasks that convert a given node into another
 * type.
 *
 * @author amorgner
 */
public class ConversionTask implements Task {

    private User user;
    private AbstractNode sourceNode;
    private Class targetNodeClass;

    public ConversionTask(final User user, final AbstractNode sourceNode, final Class targetNodeClass) {
        this.user = user;
        this.sourceNode = sourceNode;
        this.targetNodeClass = targetNodeClass;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    /**
     * User to process this task as
     *
     * @return
     */
    @Override
    public User getUser() {
        return user;
    }

    /**
     * Node to be converted
     *
     * @return
     */
    public AbstractNode getSourceNode() {
        return sourceNode;
    }

    /**
     * Class of target node
     *
     * @return
     */
    public Class getTargetNodeClass() {
        return targetNodeClass;
    }

    @Override
    public Set<AbstractNode> getNodes() {
        return Collections.emptySet();
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Date getScheduledTime() {
        return null;
    }

    @Override
    public Date getCreationTime() {
        return null;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return 0;
    }

    @Override
    public int compareTo(Delayed o) {
        Long d1 = new Long(this.getDelay(TimeUnit.MILLISECONDS));
        Long d2 = new Long(o.getDelay(TimeUnit.MILLISECONDS));

        return (d1.compareTo(d2));
    }

    // ----- interface StatusInfo -----
    @Override
    public Object getStatusProperty(String key) {
        // TODO..
        return (null);
    }
}
