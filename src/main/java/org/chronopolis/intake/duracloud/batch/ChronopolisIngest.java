package org.chronopolis.intake.duracloud.batch;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.earth.SimpleCallback;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.intake.duracloud.config.props.Chron;
import org.chronopolis.intake.duracloud.model.BagData;
import org.chronopolis.intake.duracloud.model.BagReceipt;
import org.chronopolis.rest.api.BagService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.api.StagingService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.storage.Fixity;
import org.chronopolis.rest.models.storage.FixityCreate;
import org.chronopolis.rest.service.IngestRequestSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;


/**
 * Ingest a bag into Chronopolis
 *
 * Created by shake on 5/31/16.
 */
public class ChronopolisIngest implements Runnable {
    private final Logger log = LoggerFactory.getLogger(ChronopolisIngest.class);

    private IntakeSettings settings;
    private IngestSupplierFactory factory;
    private BagStagingProperties stagingProperties;

    private BagData data;
    private List<BagReceipt> receipts;

    private BagService bags;
    private StagingService staging;

    public ChronopolisIngest(BagData data,
                             List<BagReceipt> receipts,
                             ServiceGenerator generator,
                             IntakeSettings settings,
                             BagStagingProperties stagingProperties) {
        this(data, receipts, generator, settings, stagingProperties, new IngestSupplierFactory());
    }

    @VisibleForTesting
    protected ChronopolisIngest(BagData data,
                             List<BagReceipt> receipts,
                             ServiceGenerator generator,
                             IntakeSettings settings,
                             BagStagingProperties stagingProperties,
                             IngestSupplierFactory supplierFactory) {
        this.data = data;
        this.receipts = receipts;
        this.settings = settings;
        this.stagingProperties = stagingProperties;
        this.factory = supplierFactory;

        this.bags = generator.bags();
        this.staging = generator.staging();
    }

    @Override
    public void run() {
        if (settings.pushChronopolis()) {
            receipts.forEach(this::chronopolis);
        }
    }

    private BagReceipt chronopolis(BagReceipt receipt) {
        Chron chronSettings = settings.getChron();
        String name = receipt.getName();
        String prefix = chronSettings.getPrefix();
        String depositor = Strings.isNullOrEmpty(prefix) ? data.depositor() : prefix + data.depositor();

        // I'm sure there's a better way to handle this... but for now this should be ok
        String bag = settings.pushDPN() ? name + ".tar" : name;
        Path stage = Paths.get(stagingProperties.getPosix().getPath());
        Path location = stage.resolve(data.depositor()).resolve(bag);

        Optional<PageImpl<Bag>> bagPage = getBag(depositor, name);
        Boolean create = bagPage.map(page -> page.getTotalElements() == 0)
                .orElse(false);
        if (create) {
            log.info("[{}] Building ingest request for chronopolis", name);
            log.info("[depositor, {}]", depositor);
            IngestRequestSupplier supplier = factory.supplier(location, stage, depositor, name);
            supplier.get().ifPresent(this::pushRequest);
        }
        return receipt;
    }

    private Optional<PageImpl<Bag>> getBag(String depositor, String name) {
        SimpleCallback<PageImpl<Bag>> cb = new SimpleCallback<>();
        Call<PageImpl<Bag>> call = bags.get(ImmutableMap.of("depositor", depositor, "name", name));
        call.enqueue(cb);
        return cb.getResponse();
    }

    private void pushRequest(IngestRequest ingestRequest) {
        Chron chronSettings = settings.getChron();
        List<String> replicatingNodes = chronSettings.getReplicatingTo();
        ingestRequest.setStorageRegion(stagingProperties.getPosix().getId());
        ingestRequest.setRequiredReplications(replicatingNodes.size());
        ingestRequest.setReplicatingNodes(replicatingNodes);

        SimpleCallback<Bag> cb = new SimpleCallback<>();
        Call<Bag> stageCall = bags.deposit(ingestRequest);
        stageCall.enqueue(cb);
        cb.getResponse().ifPresent(this::registerFixity);
    }

    private void registerFixity(Bag bag) {
        String resource = bag.getDepositor() + "::" + bag.getName();
        String tag = "tagmanifest-sha256.txt";
        String algorithm = "sha-256";
        FixityCreate fixity = new FixityCreate();

        String root = stagingProperties.getPosix().getPath();
        Path manifest = Paths.get(root, bag.getDepositor(), bag.getName(), tag);
        HashCode hash;

        try {
            hash = Files.asByteSource(manifest.toFile())
                                 .hash(Hashing.sha256());
        } catch (IOException e) {
            log.error("[{}] Unable to digest tagmanifest! Not registering fixity for bag!", resource);
            return;
        }

        fixity.setAlgorithm(algorithm);
        fixity.setValue(hash.toString());
        Call<Fixity> call = staging.createFixityForBag(bag.getId(), "BAG", fixity);

        try {
            Response<Fixity> response = call.execute();
            if (response.isSuccessful()) {
                log.info("[{}] Successfully registered bag + fixity with Chronopolis", resource);
            } else {
                log.warn("[{}] Unable to register fixity! code={}, message={}", resource,
                        response.code(),
                        response.errorBody().string());
            }
        } catch (IOException e) {
            log.error("[{}] Error communicating with the ingest server!", resource, e);
        }

    }

    /**
     * Delegate class so that we can use a Mock supplier
     */
    public static class IngestSupplierFactory {
        public IngestRequestSupplier supplier(Path location, Path stage, String depositor, String name) {
            return new IngestRequestSupplier(location, stage, depositor, name);
        }
    }
}
