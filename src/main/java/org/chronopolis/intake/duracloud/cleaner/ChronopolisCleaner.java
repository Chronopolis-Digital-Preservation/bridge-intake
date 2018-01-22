package org.chronopolis.intake.duracloud.cleaner;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.earth.SimpleCallback;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.storage.ActiveToggle;
import org.chronopolis.rest.models.storage.StagingStorageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Class to remove data under a directory for Chronopolis content. Once data has been removed,
 * the StagingStorageModel of the Bag will be deactivated in the Ingest Server.
 * <p>
 * todo: might want to use the relative path given by the ingest server when  issuing the rm. while
 * everything should be the same, in the event of a schema change later down the line for storing
 * bags, it would make less work. If we do this there's some extra logic in checking that the
 * bagStorage is not null but that should be pretty easy. Maybe targeted for 2.1.1.
 *
 * @author shake
 */
public class ChronopolisCleaner extends Cleaner {

    private final Logger log = LoggerFactory.getLogger(ChronopolisCleaner.class);

    private final ServiceGenerator generator;
    private final BagStagingProperties properties;

    /**
     * Name of the bag
     */
    private String name;

    /**
     * Name of the depositor
     */
    private String depositor;

    /**
     * The bag
     */
    private Bag bag;

    /**
     * Create a ChronopolisCleaner which will attempt to remove a bag from
     * staging.
     */
    public ChronopolisCleaner(Path relative,
                              BagStagingProperties properties,
                              ServiceGenerator generator,
                              Bag bag) {
        super(relative, properties);
        this.generator = generator;
        this.properties = properties;
        this.bag = bag;
    }

    /**
     * Create a ChronopolisCleaner which will attempt to clean a bag from
     * staging given its name and depositor. A query  will be executed in order
     * to retrieve the bag from the Ingest Server in order to validate that the
     * bag can be removed.
     */
    public ChronopolisCleaner(Path relative,
                              BagStagingProperties properties,
                              ServiceGenerator generator,
                              String name,
                              String depositor) {
        super(relative, properties);
        if (name == null || depositor == null) {
            throw new IllegalArgumentException("Depositor and Bag Name are not allowed to be null");
        }


        this.generator = generator;
        this.bag = null;
        this.name = name;
        this.depositor = depositor;
        this.properties = properties;
    }

    @Override
    public Boolean call() {
        Optional<Bag> option = Optional.ofNullable(this.bag);

        return option.map(this::fromBag)
                .orElseGet(this::fromQuery);
    }

    private boolean fromBag(Bag bag) {
        log.info("[Cleaner] Removing content for {} {}", bag.getDepositor(), bag.getName());
        boolean success = false;
        Path root = Paths.get(properties.getPosix().getPath());
        Path full = root.resolve(bag.getDepositor()).resolve(bag.getName());

        if (bag.getStatus() == BagStatus.PRESERVED) {
            success = rm(full) && deactivate(bag);
        }

        return success;
    }

    private boolean fromQuery() {
        if (depositor == null && name == null) {
            throw new IllegalArgumentException("Depositor and Bag Name are not allowed to be null");
        }

        log.info("[Cleaner] Removing content for {} {}", depositor, name);
        // create the full path to the bag for use later
        final Path root = Paths.get(properties.getPosix().getPath());
        final Path full = root.resolve(depositor).resolve(name);

        // create the http request to retrieve the bag
        Call<PageImpl<Bag>> call = generator.bags()
                .get(ImmutableMap.of("depositor", depositor, "name", name));
        SimpleCallback<PageImpl<Bag>> callback = new SimpleCallback<>();
        call.enqueue(callback);

        return callback.getResponse()
                .filter(response -> response.getSize() == 1)           // make sure we're only operating on the bag we expect
                .map(response -> response.getContent().get(0))         // pop the head
                .filter(bag -> bag.getStatus() == BagStatus.PRESERVED) // only continue if the bag is preserved
                .map(bag -> rm(full) && deactivate(bag)).orElse(false);
    }


    /**
     * Deactivate the bag storage for a given bag
     *
     * @param bag the bag to deactivate storage for
     */
    private boolean deactivate(Bag bag) {
        log.info("[Cleaner] Deactivating storage for {} {}", bag.getDepositor(), bag.getName());
        SimpleCallback<StagingStorageModel> stagingCB = new SimpleCallback<>();
        Call<StagingStorageModel> call = generator.staging()
                .toggleStorage(bag.getId(), "BAG", new ActiveToggle(false));
        call.enqueue(stagingCB);
        return stagingCB.getResponse()
                .map(staging -> !staging.isActive()) // make sure the staging model is NOT active
                .orElse(false);
    }
}
