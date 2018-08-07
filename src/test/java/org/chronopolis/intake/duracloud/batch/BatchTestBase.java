package org.chronopolis.intake.duracloud.batch;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.intake.duracloud.model.BagData;
import org.chronopolis.intake.duracloud.model.BagReceipt;
import org.chronopolis.intake.duracloud.test.TestApplication;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for our tests under this package
 *
 * Has basic methods to create test data and holds
 * our mocked interfaces
 *
 * Todo: don't really need spring for these...
 *
 * Created by shake on 6/2/16.
 */
@SuppressWarnings("ALL")
@SpringBootTest(classes = TestApplication.class)
public class BatchTestBase {
    protected final String MEMBER = "test-member";
    protected final String NAME = "test-name";
    protected final String DEPOSITOR = "test-depositor";
    protected final String SNAPSHOT_ID = "test-snapshot-id";

    @Autowired public IntakeSettings settings;
    @Autowired public IngestAPIProperties ingestProperties;
    @Autowired public BagStagingProperties stagingProperties;

    protected BagData data() {
        BagData data = new BagData("");
        data.setMember(MEMBER);
        data.setName(NAME);
        data.setDepositor(DEPOSITOR);
        data.setSnapshotId(SNAPSHOT_ID);
        return data;
    }

    protected BagReceipt receipt() {
        BagReceipt receipt = new BagReceipt();
        receipt.setName(UUID.randomUUID().toString());
        receipt.setReceipt(UUID.randomUUID().toString());
        return receipt;
    }

    protected List<BagReceipt> receipts() {
        return ImmutableList.of(receipt(), receipt());
    }

    // Chronopolis Entities
    protected Node createChronNode() {
        return new Node(UUID.randomUUID().toString(), UUID.randomUUID().toString());
    }

    protected Bag createChronBag() {
        Bag b = new Bag();
        b.setName(NAME)
         .setDepositor(DEPOSITOR)
         .setStatus(BagStatus.REPLICATING)
         .setId(UUID.randomUUID().getMostSignificantBits());
        return b;
    }

    protected Bag createChronBagPartialReplications() {
        Bag b = createChronBag();
        b.setReplicatingNodes(ImmutableSet.of(UUID.randomUUID().toString()));
        return b;
    }

    protected Bag createChronBagFullReplications() {
        Bag b = createChronBag();
        b.setStatus(BagStatus.PRESERVED);
        b.setReplicatingNodes(ImmutableSet.of(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()));
        return b;
    }

    // DPN Entities
    // todo: move these somewhere else... maybe a DPNTestRoot
    protected org.chronopolis.earth.models.Bag createBagNoReplications() {
        org.chronopolis.earth.models.Bag b = new org.chronopolis.earth.models.Bag();
        b.setUuid(UUID.randomUUID().toString());
        b.setLocalId("local-id");
        b.setFirstVersionUuid(b.getUuid());
        b.setIngestNode("test-node");
        b.setAdminNode("test-node");
        b.setBagType('D');
        b.setMember(MEMBER);
        b.setCreatedAt(ZonedDateTime.now());
        b.setUpdatedAt(ZonedDateTime.now());
        b.setSize(10L);
        b.setVersion(1L);
        b.setInterpretive(new ArrayList<>());
        b.setReplicatingNodes(new ArrayList<>());
        b.setRights(new ArrayList<>());
        return b;
    }

    protected org.chronopolis.earth.models.Bag createBagFullReplications() {
        org.chronopolis.earth.models.Bag b = createBagNoReplications();
        b.setReplicatingNodes(ImmutableList.of("test-repl-1", "test-repl-2", "test-repl-3"));
        return b;
    }

    protected org.chronopolis.earth.models.Bag createBagPartialReplications() {
        org.chronopolis.earth.models.Bag b = createBagNoReplications();
        b.setReplicatingNodes(ImmutableList.of("test-repl-1"));
        return b;
    }

}
