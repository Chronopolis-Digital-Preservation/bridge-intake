package org.chronopolis.intake.duracloud.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * TODO: Might be able to break out some of this to the HistorySerializer
 *
 * Created by shake on 11/20/15.
 */
public class ReplicationHistorySerializer implements JsonSerializer<ReplicationHistory> {
    @Override
    public JsonElement serialize(ReplicationHistory replicationHistory,
                                 Type type,
                                 JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        JsonObject snapshotAction = new JsonObject();
        JsonObject snapshotId = new JsonObject();
        JsonObject bagIds = new JsonObject();
        JsonObject node = new JsonObject();

        JsonArray historyArray = new JsonArray();

        JsonPrimitive idPrimitive = new JsonPrimitive(replicationHistory.getSnapshotId());
        JsonPrimitive actionPrimitive = new JsonPrimitive(replicationHistory.getSnapshotAction());
        snapshotAction.add("snapshot-action", actionPrimitive);
        snapshotId.add("snapshot-id", idPrimitive);

        JsonArray bagIdArray = new JsonArray();

        for (String bagId : replicationHistory.history.getBagIds()) {
            bagIdArray.add(new JsonPrimitive(bagId));
        }

        bagIds.add("bag-ids", bagIdArray);
        node.add("node", new JsonPrimitive(replicationHistory.history.getNode()));

        historyArray.add(snapshotAction);
        historyArray.add(snapshotId);
        historyArray.add(bagIds);
        historyArray.add(node);
        String history = historyArray.toString()
                                     .replace("\"", "'");

        obj.add("history", new JsonPrimitive(history));
        obj.add("alternate", new JsonPrimitive(replicationHistory.getAlternate()));
        return obj;
    }
}
