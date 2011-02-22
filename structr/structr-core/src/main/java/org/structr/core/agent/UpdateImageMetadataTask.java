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
import org.structr.core.entity.StructrNode;
import org.structr.core.entity.User;

/**
 * Use this interface for tasks that convert a given node into another
 * type.
 *
 * @author amorgner
 */
public class UpdateImageMetadataTask implements Task {

    public UpdateImageMetadataTask() {
    }

    @Override
    public Set<StructrNode> getNodes() {
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
	public Object getStatusProperty(String key)
	{
		// TODO..
		return(null);
	}

    @Override
    public User getUser() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

}
