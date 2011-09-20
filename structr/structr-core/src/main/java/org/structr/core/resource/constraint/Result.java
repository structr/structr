/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.resource.constraint;

import java.util.LinkedList;
import java.util.List;
import org.structr.core.entity.AbstractNode;

/**
 * Encapsulates the result of a ResourceConstraint. This can be either a
 * single element or a List of elements.
 *
 * @author Christian Morgner
 */
public class Result<T extends AbstractNode> {

	private List<T> results = null;
	private String queryTime = null;

	public Result(T singleResult) {
		this.results = new LinkedList<T>();
		this.results.add(singleResult);
	}

	public Result(List<T> listResult) {
		this.results = listResult;
	}

	public List<T> getResults() {
		return results;
	}

	public String getQueryTime() {
		return queryTime;
	}
}
