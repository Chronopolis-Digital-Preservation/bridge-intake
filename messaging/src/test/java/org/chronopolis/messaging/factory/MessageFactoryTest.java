package org.chronopolis.messaging.factory;

import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.chronopolis.messaging.pkg.PackageReadyReplyMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * All we want is for the tests to run without error. This is to make sure the
 * setters in the messages are in sync with the defined params in MessageType
 *
 * As we add more methods to the messageFactory for each type of message, we will
 * need to add a new test here
 *
 * Created by shake on 2/7/14.
 */
@RunWith(JUnit4.class)
public class MessageFactoryTest{
    GenericProperties props;
    MessageFactory messageFactory;


    @Before
    public void setUp() throws Exception {
        props = new GenericProperties(
                "node",
                "stage",
                "exchange",
                "inboundKey",
                "broadcastKey"
        );

        messageFactory = new MessageFactory(props);
    }

    @Test
    public void testCollectionInitMessage() throws Exception {
        CollectionInitMessage message = messageFactory.collectionInitMessage(
                10,
                "test-collection",
                "test-depositor",
                "token-store",
                Digest.SHA_256
        );
    }

    @Test
    public void testPackageReadyMessage() throws Exception {
        PackageReadyMessage message = messageFactory.packageReadyMessage(
                "test-depositor",
                Digest.SHA_256,
                "test-location",
                "test-package-name",
                10
        );
    }

    @Test
    public void testPackageReadyReplyMessage() throws Exception {
        PackageReadyReplyMessage message = messageFactory.packageReadyReplyMessage(
                "test-package-name",
                Indicator.ACK,
                "test-correlation-id"
        );
    }
}