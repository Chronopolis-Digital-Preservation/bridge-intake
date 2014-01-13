/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.bagit.Bag;
import org.chronopolis.bagit.ManifestProcessor;
import org.chronopolis.bagit.ManifestProcessor.ManifestError;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shake
 */
public class PackageReadyProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(PackageReadyProcessor.class);
    private ChronProducer producer;
    private IngestProperties props;


    public PackageReadyProcessor(ChronProducer producer, IngestProperties props) {
        this.producer = producer;
        this.props = props;
    }

    /* Once we've confirmed that a package is in our staging area we want to do
     * a few things:
     * 1 - Check if the package is a bag
     *   .5 - If not, create a bag (this is probably going to be done by intake)
     * 2 - Check manifests
     * 3 - Create ACE Tokens 
     * 4 - Send out the collection init message
     * 
     */
    @Override
    public void process(ChronMessage chronMessage) {
        System.out.println("Processing " + chronMessage.getType().toString());
        if ( !(chronMessage instanceof PackageReadyMessage)) {
            // Error out
        }

        PackageReadyMessage msg = (PackageReadyMessage) chronMessage;

        String location = msg.getLocation();
        String filename = msg.getPackageName();
        Path toBag = Paths.get(props.getStage(), location);
        Path manifest = null;

        Bag validator = new Bag(toBag);
        Future<Boolean> f = validator.getValidManifest();
        try {
            Boolean valid = f.get();

            if ( valid ) {
                log.info("Bag is valid; continuing to make manifest");
                //manifest = validator.getAceManifest(Paths.get(props.getTokenStage()));
                //log.info("Manifest is located at " + manifest.toString());
            } else {
                ManifestProcessor mv = validator.getManifestValidator();
                StringBuilder err = new StringBuilder("Invalid bag (bad digests)\n");
                for (ManifestError e : mv.getCorruptedFiles()) {
                    err.append(e.getPath()).append(" { ").append(e.getExpected())
                       .append(" !=  ").append(e.getFound()).append(" }").append("\n");
                }
                throw new RuntimeException(err.toString());
            }

            // Bag.validateFormat(toBag);

            // Bag.validateManifestAndGetTokens(toBag);
        } catch (InterruptedException | ExecutionException ex) {
            log.error("Error occured {} ", ex);
        }

        if ( manifest == null ) {
            throw new RuntimeException("Error creating manifest");
        }

        
        // Things to do:
        
        // String protocol = getProtocol();
        
        // Should end up being the location for a download
        //String tokenStore = "https://chron-monitor.umiacs.umd.edu/tokenStore001";
        // Send message
        StringBuilder tokenStore = new StringBuilder("http://localhost/tokens/");
        tokenStore.append(manifest.getFileName().toString());
        CollectionInitMessage response = MessageFactory.DefaultCollectionInitMessage();
        response.setCollection(msg.getPackageName());
        response.setTokenStore(tokenStore.toString());
        response.setDate(msg.getDepositor());
        response.setAuditPeriod(120);
        

        // Sending the next message will be done in the ingest consumer?
        // CollectionInitMessage collectionInitRequest = new CollectionInitMessage();
        // collectionInitRequest.setAuditPeriod("somedefinedperiod");
        // collectionInitRequest.setCollection(getPackageName());
        // collectionInitRequest.setDepositor(getDepositor());
        // collectionInitRequest.setTokenStore(tokenStore);


        // Hold the routing key here temporarily
        // Will be from the properties soon
        response.setReturnKey("collection.ingest.umiacs");
        producer.send(response, "collection.init.broadcast");
    }

}
