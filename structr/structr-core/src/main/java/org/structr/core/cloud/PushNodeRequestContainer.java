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

/**
 * Encapsulates a pull request for a node
 *
 *
 * @author Christian Morgner
 */
public class PushNodeRequestContainer extends DataContainer
{
	private long targetNodeId = 0L;

	public PushNodeRequestContainer()
	{
	}

	public PushNodeRequestContainer(long targetNodeId)
	{
		this.targetNodeId = targetNodeId;

		this.estimatedSize = 1024;
	}

	/**
	 * @return the targetNodeId
	 */
	public long getTargetNodeId()
	{
		return targetNodeId;
	}

	/**
	 * @param targetNodeId the targetNodeId to set
	 */
	public void setTargetNodeId(long targetNodeId)
	{
		this.targetNodeId = targetNodeId;
	}
}
