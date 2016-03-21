package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import okhttp3.OkHttpClient;
import org.chronopolis.common.ace.OkBasicInterceptor;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.repository.BagService;
import org.chronopolis.ingest.support.PageImpl;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import retrofit2.Call;
import retrofit2.Retrofit;

import static org.junit.Assert.assertEquals;


/**
 * Tests for the staging API
 *
 */
@WebIntegrationTest("server.port:0")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBags.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteBags.sql")
})
public class StagingControllerTest extends IngestTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    BagService bagService;

    // @Test
    public void testSerial() throws Exception {
       ResponseEntity<Bag> entity = new TestRestTemplate("umiacs", "umiacs")
               .getForEntity("http://localhost:" + port + "/api/bags/10", Bag.class);

        Bag bag = bagService.findBag((long) 10);
        System.out.println(bag.getReplicatingNodes());
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OkBasicInterceptor("umiacs", "umiacs"))
                .build();

        Retrofit adapter = new Retrofit.Builder()
                .baseUrl("http://localhost:" + port)
                .client(client)
                // .setRequestInterceptor(new CredentialRequestInterceptor("umiacs", "umiacs"))
                // .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        IngestAPI api = adapter.create(IngestAPI.class);
        Call<Bag> call = api.getBag((long) 10);
        Bag bag1 = call.execute().body();
        System.out.println(bag1.getTokenLocation());
    }

    @Test
    public void testGetBags() throws Exception {
        ResponseEntity<PageImpl> entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/bags", PageImpl.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(10, entity.getBody().getTotalElements());
    }

    @Test
    public void testGetBag() throws Exception {
        ResponseEntity<Bag> entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/bags/1", Bag.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals("bag-0", entity.getBody().getName());
    }

    @Test
    public void testNonExistentBag() throws Exception {
        ResponseEntity entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/bags/12015851", Object.class);

        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
    }

    @Test
    public void testStageExistingBag() throws Exception {
        // need admin credentials for creating resources
        TestRestTemplate template = new TestRestTemplate("admin", "admin");
        IngestRequest request = new IngestRequest();
        // All defined the createBags.sql
        request.setName("bag-0");
        request.setDepositor("test-depositor");
        request.setLocation("bags/test-bag-0");

        ResponseEntity<Bag> bag = template.postForEntity(
                "http://localhost:" + port + "/api/bags",
                request,
                Bag.class);

        assertEquals(Long.valueOf(1), bag.getBody().getId());
    }

    @Test
    public void testStageBagWithoutReplications() throws Exception {
        // need admin credentials for creating resources
        TestRestTemplate template = new TestRestTemplate("admin", "admin");
        IngestRequest request = new IngestRequest();

        request.setName("new-bag-1");
        request.setDepositor("test-depositor");
        request.setLocation("test-depositor/new-bag-1");

        ResponseEntity<Bag> bag = template.postForEntity(
                "http://localhost:" + port + "/api/bags",
                request,
                Bag.class);

        assertEquals(Long.valueOf(11), bag.getBody().getId());
        // This now happens after the initial commit of information to the repository
        // assertEquals(Long.valueOf(3), Long.valueOf(bag.getBody().getTotalFiles()));
    }

    @Test
    /**
     * Test with specifying replicating nodes to ensure they get saved
     *
     */
    public void testStageBagWithReplications() throws Exception {
        // need admin credentials for creating resources
        TestRestTemplate template = new TestRestTemplate("admin", "admin");
        IngestRequest request = new IngestRequest();

        request.setName("new-bag-1-1");
        request.setDepositor("test-depositor");
        request.setLocation("test-depositor/new-bag-1");
        request.setReplicatingNodes(ImmutableList.of("umiacs"));
        request.setRequiredReplications(1);

        ResponseEntity<Bag> bag = template.postForEntity(
                "http://localhost:" + port + "/api/bags",
                request,
                Bag.class);

        // Pull from the database to check the distribution record
        Bag fromDb = bagService.findBag(bag.getBody().getId());
        assertEquals(1, fromDb.getDistributions().size());
    }


}