/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.structr.core.cloud;

/**
 * An instance address represents the connection information for a specific
 * structr instance.
 * 
 * @author axel
 */
public class InstanceAddress {
    
    private String host;
    private String tcpPort;
    private String udpPort;
    private int timeout;

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the tcpPort
     */
    public String getTcpPort() {
        return tcpPort;
    }

    /**
     * @param tcpPort the tcpPort to set
     */
    public void setTcpPort(String tcpPort) {
        this.tcpPort = tcpPort;
    }

    /**
     * @return the udpPort
     */
    public String getUdpPort() {
        return udpPort;
    }

    /**
     * @param udpPort the udpPort to set
     */
    public void setUdpPort(String udpPort) {
        this.udpPort = udpPort;
    }

    /**
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }



}
