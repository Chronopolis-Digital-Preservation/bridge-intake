package org.chronopolis.intake.duracloud.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Serialize BaggingHistory objects in a way that the bridge can understand
 *
 * Created by shake on 11/13/15.
 */
public class BaggingHistorySerializer implements JsonSerializer<BaggingHistory> {

    @Override
    public JsonElement serialize(BaggingHistory baggingHistory,
                                 Type type,
                                 JsonSerializationContext context) {
        JsonObject snapshotAction = new JsonObject();
        JsonObject snapshotId = new JsonObject();
        JsonObject bagIds = new JsonObject();
        JsonObject checksums = new JsonObject();

        JsonObject object = new JsonObject();
        JsonArray historyArray = new JsonArray();

        snapshotAction.add("snapshot-action", new JsonPrimitive(baggingHistory.getSnapshotAction()));
        snapshotId.add("snapshot-id", new JsonPrimitive(baggingHistory.getSnapshotId()));

        JsonArray bagIdArray = new JsonArray();
        JsonArray checksumArray = new JsonArray();

        for (BagReceipt receipt : baggingHistory.getHistory()) {
            bagIdArray.add(new JsonPrimitive(receipt.getName()));
            checksumArray.add(new JsonPrimitive(receipt.getReceipt()));
        }

        bagIds.add("bag-ids", bagIdArray);
        checksums.add("manifest-checksums", checksumArray);

        historyArray.add(snapshotAction);
        historyArray.add(snapshotId);
        historyArray.add(bagIds);
        historyArray.add(checksums);

        String history = historyArray.toString()
                                     .replace("\"", "'");

        // serialize the history
        object.add("history", new JsonPrimitive(history));
        object.add("alternate", new JsonPrimitive(baggingHistory.getAlternate()));
        return object;
    }
}
