/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging;

/**
 *
 * @author shake
 */
public enum MessageConstant {
    // Headers
    ORIGIN("origin"),
    RETURN_KEY("returnKey"),
    CORRELATION_ID("correlationId"),
    DATE("date"),

    // Message vals
    DEPOSITOR("depositor"),
    COLLECTION("collection"),
    TOKEN_STORE("token-store"),
    AUDIT_PERIOD("audit-period"),
    DIGEST("digest"),
    DIGEST_TYPE("digest-type"),
    PACKAGE_NAME("package-name"),
    FILENAME("filename"),
    LOCATION("location"),
    PROTOCOL("protocol"),
    SIZE("size"),
    STATUS("status"),
    FAILED_ITEMS("failed-items"),

    // Misc 
    STATUS_SUCCESS("success"),
    STATUS_FAIL("failed"),
    ;
    
    private final String text;

    MessageConstant(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
