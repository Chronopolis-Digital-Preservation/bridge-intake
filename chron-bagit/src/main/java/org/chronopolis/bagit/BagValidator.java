/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import edu.umiacs.ace.ims.api.IMSService;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import edu.umiacs.ace.ims.api.TokenRequestBatch;
import edu.umiacs.ace.ims.ws.TokenRequest;
import java.util.concurrent.ExecutionException;

/**
 * Immutable variables call me Immutable Variable man
 * TODO: Make list of Validators for easy iteration
 *
 * @author shake
 */
public class BagValidator {
    
    // Files to check for in the bag
    public final String bagInfo = "bag-info.txt";
    public final String manifest = "*manifest-*.txt";
    public final String charset = "UTF-8";
    public final String bagit = "bagit.txt";
    
    private final ExecutorService manifestRunner = Executors.newCachedThreadPool();
    private final Path toBag;
    private TokenWriterCallback callback = null;
    private TokenRequestBatch batch = null;
    // Only SHA-256 digests in here
    // Could probably wrap this in a class and force it to check the size
    private Map<Path, String> validDigests;

    // All our various validators
    private BagInfoValidator bagInfoValidator;
    private BagitValidator bagitValidator;
    private ManifestValidator manifestValidator;
    private Future<Boolean> validManifest;
    
    
    public BagValidator(Path toBag) {
        this.toBag = toBag;
        callback = new TokenWriterCallback(toBag.getFileName().toString());
        validDigests = new HashMap<>();

        bagInfoValidator = new BagInfoValidator(toBag);
        bagitValidator = new BagitValidator(toBag);
        manifestValidator = new ManifestValidator(toBag);
        validManifest = manifestRunner.submit(manifestValidator);
    }
    
    public static boolean ValidateBagFormat(Path toBag) {
        return true;
    }
    
    public Future<Boolean> getValidManifest() {
        return validManifest;
    }

    
    // Need to figure out exactly how we want to do these...
    public void checkBagitFiles() {
        bagInfoValidator.isValid();
        bagitValidator.isValid();
    }

    public void setValidDigests(Map<Path, String> validDigests) {
        this.validDigests = validDigests;
    }
    
    public Map<Path, String> getValidDigests(){
        return validDigests;
    }
    
    // Maybe we should push this into something else as well
    // Since it doesn't have to do much with validation, only runs after
    public Path getManifest(Path stage) throws InterruptedException, 
                                               IOException, 
                                               ExecutionException {
        createIMSConnection();
        callback.setStage(stage);
        Future<Path> manifestPath = manifestRunner.submit(callback);
        
        for ( Map.Entry<Path, String> entry : validDigests.entrySet()) {
            TokenRequest req = new TokenRequest();
            // We want the relative path for ACE so let's get it
            Path full = entry.getKey();
            Path relative = full.subpath(toBag.getNameCount(), full.getNameCount());
            
            req.setName(relative.toString());
            req.setHashValue(entry.getValue());
            batch.add(req);
        }

        return manifestPath.get();
    }
    
    private void createIMSConnection() {
        IMSService ims;
        // TODO: Unhardcode
        ims = IMSService.connect("ims.umiacs.umd.edu", 443, true);
        batch = ims.createImmediateTokenRequestBatch("SHA-256",
                callback,
                1000,
                5000);
    }
    
}
