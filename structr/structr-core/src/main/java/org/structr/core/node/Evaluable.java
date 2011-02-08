/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.node;

import org.neo4j.graphdb.traversal.Evaluator;

/**
 *
 * @author Christian Morgner
 */
public interface Evaluable
{
	public void addEvaluator(Evaluator e);
	public void removeEvaluator(Evaluator e);
}
