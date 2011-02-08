package org.structr.core.entity;

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
