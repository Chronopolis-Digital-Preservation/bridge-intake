package org.chronopolis.common.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * Created by shake on 9/30/14.
 */
@Component
public class DPNSettings {
    private final Logger log = LoggerFactory.getLogger(DPNSettings.class);

    @Value("${dpn.api-key:admin}")
    private String apiKey;

    @Value("${dpn.node:chron}")
    private String dpnNode;

    private List<String> dpnEndpoints;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

    public List<String> getDPNEndpoints() {
        return dpnEndpoints;
    }

    @Value("${dpn.endpoints:http://localhost}")
    public void setDpnEndpoints(String args) {
        log.debug("Splitting dpn endpoints");
        String[] endpoints = args.split(",");
        log.debug("Found {} endpoints: {}", endpoints.length, endpoints);
        this.dpnEndpoints = new ArrayList<>();

        // TODO: Replace string with HttpUrl?
        for (String endpoint : endpoints) {
            if (!endpoint.endsWith("/")) {
                endpoint += "/";
            }

            dpnEndpoints.add(endpoint);
        }
    }

    public String getDpnNode() {
        return dpnNode;
    }

    public DPNSettings setDpnNode(String dpnNode) {
        this.dpnNode = dpnNode;
        return this;
    }
}