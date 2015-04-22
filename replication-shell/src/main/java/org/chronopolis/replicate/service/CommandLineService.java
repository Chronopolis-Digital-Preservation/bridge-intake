package org.chronopolis.replicate.service;

import org.chronopolis.replicate.batch.ReplicationJobStarter;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by shake on 2/23/15.
 */
@Component
@Profile("development")
public class CommandLineService implements ReplicationService {
    private final Logger log = LoggerFactory.getLogger(CommandLineService.class);

    @Autowired
    ApplicationContext context;

    @Autowired
    ReplicationSettings settings;

    @Autowired
    IngestAPI ingestAPI;

    @Autowired
    ReplicationJobStarter jobStarter;


    @Override
    public void replicate() {
        boolean done = false;
        while (!done) {
            OPTION option = inputOption();
            if (option.equals(OPTION.RESTFUL_QUERY)) {
                log.info("Query for active replications");
                query(ReplicationStatus.STARTED, false);

                log.info("Query for new replications");
                query(ReplicationStatus.PENDING, true);
            } else if (option.equals(OPTION.QUIT)) {
                log.info("Quitting");
                done = true;
            }

        }

        SpringApplication.exit(context);
    }

    private OPTION inputOption() {
        OPTION option = OPTION.UNKNOWN;
        while (option.equals(OPTION.UNKNOWN)) {
            StringBuilder sb = new StringBuilder("Enter Option: ");
            String sep = " | ";
            for (OPTION value : OPTION.values()) {
                if (!value.equals(OPTION.UNKNOWN)) {
                    sb.append(value.name());
                    sb.append(" [");
                    sb.append(value.name().charAt(0));
                    sb.append("]");
                    sb.append(sep);
                }
            }

            //The one difference, mwahhaha
            sb.replace(sb.length() - sep.length(), sb.length(), " -> ");
            System.out.println(sb.toString());
            option = OPTION.fromString(readLine().trim());
        }
        return option;
    }

    private void query(ReplicationStatus status, boolean update) {
        List<Replication> replications = ingestAPI.getReplications(status);
        log.debug("Found {} replications", replications.size());

        for (Replication replication : replications) {
            log.info("Starting job for replication id {}", replication.getID());
            if (update) {
                log.info("Updating replication");
                replication.setStatus(ReplicationStatus.STARTED);
                ingestAPI.updateReplication(replication.getID(), replication);
            }
            jobStarter.addJobFromRestful(replication);
        }
    }

    private String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }

    private enum OPTION {
        RESTFUL_QUERY, QUIT, UNKNOWN;

        private static OPTION fromString(String text) {
            switch (text) {
                case "R":
                case "r":
                    return RESTFUL_QUERY;
                case "Q":
                case "q":
                    return QUIT;
                default:
                    return UNKNOWN;
            }
        }
    }

}