package org.structr.core.entity;

import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import org.neo4j.graphdb.Direction;
import org.structr.common.RelType;

/**
 * 
 * @author amorgner
 * 
 */
public class User extends Person {

    private final static String ICON_SRC = "/images/user.png";

    public final static String REAL_NAME_KEY = "realName";
    public final static String PASSWORD_KEY = "password";
    public final static String BLOCKED_KEY = "blocked";
    public final static String SESSION_ID_KEY = "sessionId";

    @Override
    public String getIconSrc() {
        return ICON_SRC;
    }

    /**
     * Return user's personal root node
     * 
     * @return
     */
    public AbstractNode getRootNode() {
        List<StructrRelationship> outRels = getRelationships(RelType.ROOT_NODE, Direction.OUTGOING);
        if (outRels != null) for (StructrRelationship r : outRels) {
            return r.getEndNode();
        }
        return null;
    }

    public void setPassword(final String passwordValue) {

            // store passwords always as SHA-512 hash
            setProperty(PASSWORD_KEY,
                    DigestUtils.sha512Hex(passwordValue));
    }
    
    public String getPassword() {
        return (String) getProperty(PASSWORD_KEY);
    }

    public String getRealName() {
        return (String) getProperty(REAL_NAME_KEY);
    }

    public void setRealName(final String realName) {
        setProperty(REAL_NAME_KEY, realName);
    }

    public Boolean getBlocked() {
        return (Boolean) getProperty(BLOCKED_KEY);
    }

    public void setBlocked(final Boolean blocked) {
        setProperty(BLOCKED_KEY, blocked);
    }

    public Boolean isBlocked() {
        return Boolean.TRUE.equals(getBlocked());
    }

    public void block() {
        setBlocked(Boolean.TRUE);
    }


}
