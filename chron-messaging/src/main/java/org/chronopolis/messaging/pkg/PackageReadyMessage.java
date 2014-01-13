/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.pkg;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;

import static org.chronopolis.messaging.MessageConstant.PACKAGE_NAME;
import static org.chronopolis.messaging.MessageConstant.LOCATION;
import static org.chronopolis.messaging.MessageConstant.DEPOSITOR;
import static org.chronopolis.messaging.MessageConstant.SIZE;

/**
 * Relay the state of the collection
 *
 * @author shake
 */
public class PackageReadyMessage extends ChronMessage {
    
    public PackageReadyMessage() {
        super(MessageType.PACKAGE_INGEST_READY);
        this.body = new ChronBody(type);
    }

    public void setPackageName(String packageName) {
        body.addContent(PACKAGE_NAME.toString(), packageName);
    }
    
    public String getPackageName() {
        return (String)body.get(PACKAGE_NAME.toString());
    }
    
    public void setDepositor(String depositor) {
        body.addContent(DEPOSITOR.toString(), depositor);
    }
    
    public String getDepositor() {
        return (String)body.get(DEPOSITOR.toString());
    }
    
    public void setLocation(String location) {
        body.addContent(LOCATION.toString(), location);
    }
    
    public String getLocation() {
        return (String)body.get(LOCATION.toString());
    }
    
    public void setSize(long size) {
        body.addContent(SIZE.toString(), size);
    }
    
    public long getSize() {
        return (long)body.get(SIZE.toString());
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("package-name : ");
        sb.append(getPackageName());
        sb.append(", depositor : ");
        sb.append(getDepositor());
        sb.append(", location : ");
        sb.append(getLocation());
        sb.append(", size : ");
        sb.append(getSize());
        return sb.toString();
    }
    
}
