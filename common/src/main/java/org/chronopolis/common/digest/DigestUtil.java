package org.chronopolis.common.digest;

import edu.umiacs.ace.util.HashValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by shake on 1/28/14.
 */
public final class DigestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(DigestUtil.class);

    private DigestUtil() {
    }

    /**
     * Digest the file and return it in hex format
     *
     * @param file the file to digest
     * @param alg the algorithm to use (sha-256)
     * @return string representation of the digest
     */
    public static String digest(final Path file, final String alg) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(alg);
            DigestInputStream dis = new DigestInputStream(Files.newInputStream(file), md);
            int bufferSize = 1046576; // 1 MB
            byte[] buf = new byte[bufferSize];
            while (dis.read(buf) >= 0) { }
        } catch (NoSuchAlgorithmException e) {
            LOG.error("Error finding algorithm {}", alg, e);
            throw new RuntimeException("Could not digest " + file.toString());
        } catch (IOException e) {
            LOG.error("IO Error for {}", file, e);
            throw new RuntimeException("Could not digest " + file.toString());
        }

        return HashValue.asHexString(md.digest());
    }

}
