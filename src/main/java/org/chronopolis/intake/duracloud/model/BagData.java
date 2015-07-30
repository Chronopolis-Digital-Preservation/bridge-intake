package org.chronopolis.intake.duracloud.model;

/**
 * Class to encapsulate some of the data we need when making bags
 *
 * Created by shake on 7/30/15.
 */
public class BagData {

    private String snapshotId;
    private String name;
    private String depositor;

    public BagData() {
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public BagData setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
        return this;
    }

    public String getName() {
        return name;
    }

    public BagData setName(String name) {
        this.name = name;
        return this;
    }

    public String getDepositor() {
        return depositor;
    }

    public BagData setDepositor(String depositor) {
        this.depositor = depositor;
        return this;
    }

}
