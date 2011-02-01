/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.structr.core.node;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

/**
 *
 * @author cmorgner
 */
public class TransactionCommand extends NodeServiceCommand {

    @Override
    public Object execute(Object... parameters) {

        Object ret = null;
        GraphDatabaseService graphDb = (GraphDatabaseService) arguments.get("graphDb");

        if (parameters.length > 0 && parameters[0] instanceof StructrTransaction) {

            StructrTransaction transaction = (StructrTransaction) parameters[0];
            Transaction tx = graphDb.beginTx();
            try {
                ret = transaction.execute();

                tx.success();

            } catch (Throwable t) {
                t.printStackTrace();

                tx.failure();

            } finally {
                tx.finish();
            }

        } else if (parameters.length > 0 && parameters[0] instanceof BatchTransaction) {

            BatchTransaction transaction = (BatchTransaction) parameters[0];
            Transaction tx = graphDb.beginTx();
            try {
                ret = transaction.execute(tx);

                tx.success();

            } catch (Throwable t) {
                t.printStackTrace();

                tx.failure();

            } finally {
                tx.finish();
            }

        }

        return ret;
    }


}
