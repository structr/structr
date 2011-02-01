package org.structr.core.entity;

import java.util.List;
import org.structr.common.RelType;
import org.structr.core.Command;
import org.structr.core.Services;
import org.structr.core.node.CreateNodeCommand;
import org.structr.core.node.CreateRelationshipCommand;
import org.structr.core.node.FindNodeCommand;
import org.structr.core.node.NodeAttribute;
import org.structr.core.node.NodeFactoryCommand;
import org.structr.core.node.StructrTransaction;
import org.structr.core.node.TransactionCommand;
import org.structr.core.node.XPath;

/**
 * 
 * @author amorgner
 * 
 */
public class SuperUser extends User {

    public SuperUser() {
    }

    @Override
    public long getId() {
        return -1L;
    }

    @Override
    public String getName() {
        return "superuser";
    }

    @Override
    public void setName(final String name) {
        // not supported
    }

    @Override
    public void setPassword(final String passwordValue) {
        // not supported
    }

    @Override
    public String getRealName() {
        return "Super User";
    }

    @Override
    public void setRealName(final String realName) {
        // not supported
    }

    @Override
    public Boolean getBlocked() {
        return false;
    }

    @Override
    public void setBlocked(final Boolean blocked) {
        // not supported
    }

    @Override
    public Boolean isBlocked() {
        return false;
    }

    @Override
    public void block() {
        // not supported
    }
}
