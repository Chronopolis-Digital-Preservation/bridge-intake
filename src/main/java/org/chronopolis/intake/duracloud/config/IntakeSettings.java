package org.chronopolis.intake.duracloud.config;

import org.chronopolis.intake.duracloud.config.props.Chron;
import org.chronopolis.intake.duracloud.config.props.DPN;
import org.chronopolis.intake.duracloud.config.props.Duracloud;
import org.chronopolis.intake.duracloud.config.props.Smtp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One day we'll migrate to configuration properties. I swear it.
 *
 * Created by shake on 8/1/14.
 */
@ConfigurationProperties
@SuppressWarnings("UnusedDeclaration")
public class IntakeSettings {
    private final Logger log = LoggerFactory.getLogger(IntakeSettings.class);

    private DPN dpn;
    private Chron chron;
    private Duracloud duracloud;
    private Smtp smtp;

   /**
     * Boolean used to enable cleaner
     */
    private Boolean cleanerEnabled = false;

    /**
     * Boolean used if we want to dry run cleans
     */
    private Boolean cleanDryRun = false;

    /**
     * Boolean used to push snapshots to DPN
     */
    private Boolean pushDPN = true;

    /**
     * Boolean used to push snapshots to Chronopolis
     */
    private Boolean pushChronopolis = true;

    /**
     * Boolean to configure SNI for https connections
     */
    private Boolean disableSNI = false;

    // should these be encapsulated by a dpn class?
    /**
     * String of the member uuid we are dealing with (deprecated)
     */
    private String memberUUID;

    /**
     * String value representing the server used for dpn replicating nodes
     */
    private String dpnReplicationServer;

    public Boolean pushChronopolis() {
        return pushChronopolis;
    }

    public IntakeSettings setPushChronopolis(Boolean pushChronopolis) {
        this.pushChronopolis = pushChronopolis;
        return this;
    }

    public Boolean pushDPN() {
        return pushDPN;
    }

    public IntakeSettings setPushDPN(Boolean pushDPN) {
        this.pushDPN = pushDPN;
        return this;
    }

    public String getMemberUUID() {
        return memberUUID;
    }

    public IntakeSettings setMemberUUID(String memberUUID) {
        this.memberUUID = memberUUID;
        return this;
    }

    public String getDpnReplicationServer() {
        return dpnReplicationServer;
    }

    public IntakeSettings setDpnReplicationServer(String dpnReplicationServer) {
        this.dpnReplicationServer = dpnReplicationServer;
        return this;
    }

    public Boolean getDisableSNI() {
        return disableSNI;
    }

    public IntakeSettings setDisableSNI(Boolean disableSNI) {
        this.disableSNI = disableSNI;
        return this;
    }

    public Chron getChron() {
        return chron;
    }

    public IntakeSettings setChron(Chron chron) {
        this.chron = chron;
        return this;
    }

    public Duracloud getDuracloud() {
        return duracloud;
    }

    public IntakeSettings setDuracloud(Duracloud duracloud) {
        this.duracloud = duracloud;
        return this;
    }

    public DPN getDpn() {
        return dpn;
    }

    public IntakeSettings setDpn(DPN dpn) {
        this.dpn = dpn;
        return this;
    }

    public Boolean isCleanDryRun() {
        return cleanDryRun;
    }

    public IntakeSettings setCleanDryRun(Boolean cleanDryRun) {
        this.cleanDryRun = cleanDryRun;
        return this;
    }

    public Boolean isCleanerEnabled() {
        return cleanerEnabled;
    }

    public IntakeSettings setCleanerEnabled(Boolean cleanerEnabled) {
        this.cleanerEnabled = cleanerEnabled;
        return this;
    }

    public Smtp getSmtp() {
        return smtp;
    }

    public IntakeSettings setSmtp(Smtp smtp) {
        this.smtp = smtp;
        return this;
    }
}
