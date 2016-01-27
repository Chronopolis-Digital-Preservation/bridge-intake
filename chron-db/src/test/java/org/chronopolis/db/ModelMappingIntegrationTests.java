/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.db;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.chronopolis.db.JPAAssertions.assertTableExists;
import static org.chronopolis.db.JPAAssertions.assertTableHasColumn;

/**
 *
 * @author shake
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JPATestConfiguration.class})
@Rollback
@Transactional
// @TransactionConfiguration(defaultRollback = true)
public class ModelMappingIntegrationTests {
    
    @Autowired
    EntityManager entityManager;

    @Test
    public void thatItemCustomMappingWorks() throws Exception {
        assertTableExists(entityManager, "collection");

        assertTableHasColumn(entityManager, "collection", "CORRELATION_ID");
        assertTableHasColumn(entityManager, "collection", "TO_DPN");

        assertTableExists(entityManager, "status");
        assertTableHasColumn(entityManager, "status", "id");
        assertTableHasColumn(entityManager, "status", "collection_name");
        assertTableHasColumn(entityManager, "status", "depositor");
    }
    
}
