package org.chronopolis.intake.duracloud.batch;

import org.chronopolis.intake.duracloud.batch.support.CallWrapper;
import com.google.common.collect.ImmutableList;
import org.chronopolis.intake.duracloud.model.BagData;
import org.chronopolis.intake.duracloud.model.BagReceipt;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.service.IngestRequestSupplier;
import org.chronopolis.rest.support.BagConverter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Created by shake on 6/2/16.
 */
@SuppressWarnings("ALL")
@RunWith(SpringJUnit4ClassRunner.class)
public class ChronopolisIngestTest extends BatchTestBase {

    @Mock IngestAPI api;
    @Mock IngestRequestSupplier supplier;
    @Mock ChronopolisIngest.IngestSupplierFactory factory;

    private BagData data;
    private String prefix = "prefix-";

    @Before
    public void setup() {
        settings.setPushChronopolis(true);
        MockitoAnnotations.initMocks(this);
        data = data();
    }

    @Test
    public void withoutPrefix() throws Exception {
        settings.setPushDPN(true);
        BagReceipt receipt = receipt();
        ChronopolisIngest ingest = new ChronopolisIngest(data, ImmutableList.of(receipt), api, settings, stagingProperties, factory);

        IngestRequest request = request(receipt, data.depositor(), ".tar");
        when(factory.supplier(any(Path.class), any(Path.class), eq(data.depositor()), eq(receipt.getName()))).thenReturn(supplier);
        when(supplier.get()).thenReturn(Optional.of(request));
        when(api.stageBag(eq(request))).thenReturn(new CallWrapper<Bag>(BagConverter.toBagModel(createChronBag())));

        ingest.run();

        verify(factory, times(1)).supplier(any(Path.class), any(Path.class), eq(data.depositor()), eq(receipt.getName()));
        verify(supplier, times(1)).get();
        verify(api, times(1)).stageBag(eq(request));
    }

    @Test
    public void withPrefix() throws Exception {
        settings.setPushDPN(false);
        BagReceipt receipt = receipt();
        settings.getChron().setPrefix(prefix);
        ChronopolisIngest ingest = new ChronopolisIngest(data, ImmutableList.of(receipt), api, settings, stagingProperties, factory);

        String depositor = prefix + data.depositor();
        IngestRequest request = request(receipt, depositor, null);
        when(factory.supplier(any(Path.class), any(Path.class), eq(depositor), eq(receipt.getName()))).thenReturn(supplier);
        when(supplier.get()).thenReturn(Optional.of(request));
        when(api.stageBag(eq(request))).thenReturn(new CallWrapper<>(BagConverter.toBagModel(createChronBag())));
        ingest.run();
        verify(api, times(1)).stageBag(eq(request));
        verify(factory, times(1)).supplier(any(Path.class), any(Path.class), eq(depositor), eq(receipt.getName()));
        verify(supplier, times(1)).get();

        settings.getChron().setPrefix("");
    }

    private IngestRequest request(BagReceipt receipt, String depostior, String fileType) {
        String bag = fileType == null ? receipt.getName() : receipt.getName() + fileType;
        Path location = Paths.get(stagingProperties.getPosix().getPath(), data.depositor(), bag);
        List<String> nodes = settings.getChron().getReplicatingTo();
        IngestRequest request = new IngestRequest();
        request.setSize(1L);
        request.setTotalFiles(1L);
        request.setDepositor(depostior);
        request.setName(receipt.getName());
        request.setLocation(location.toString());
        request.setReplicatingNodes(nodes);
        request.setRequiredReplications(nodes.size());
        request.setStorageRegion(stagingProperties.getPosix().getId());
        return request;
    }

}