/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.pkg;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronMessage;

/**
 *
 * @author shake
 */
public class PackageIngestStatusQueryMessage extends ChronMessage {

    public PackageIngestStatusQueryMessage() {
        super(MessageType.PACKAGE_INGEST_STATUS_QUERY);
    }

}