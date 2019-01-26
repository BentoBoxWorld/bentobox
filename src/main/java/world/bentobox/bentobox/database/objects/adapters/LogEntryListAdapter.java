package world.bentobox.bentobox.database.objects.adapters;

import org.eclipse.jdt.annotation.NonNull;
import world.bentobox.bentobox.api.logs.LogEntry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Poslovitch
 */
/*  The serialization might look a bit weird here, as I'm using JSON's array of object.
    This means that, once serialized, the data will look like this (on YAML):
        history:
            - timestamp: 0
              type: "test"
              data:
                player: "uuid"
                action: "ISLAND_LEVEL_UPDATED"
                value: 45
            - timestamp: 4181
              type: "lol"
              data:
                help: "yep"
 */
public class LogEntryListAdapter implements AdapterInterface<List<LogEntry>, List<Map<String, Object>>> {

    private static final String TIMESTAMP = "timestamp";
    private static final String TYPE = "type";
    private static final String DATA = "data";

    @SuppressWarnings("unchecked")
    @Override
    public List<LogEntry> deserialize(Object object) {
        List<LogEntry> result = new LinkedList<>();
        if (object == null) {
            return result;
        }

        List<Map<String, Object>> serialized = (List<Map<String, Object>>) object;
        for (Map<String, Object> entry : serialized) {
            long timestamp = (long) entry.get(TIMESTAMP);
            String type = (String) entry.get(TYPE);
            Map<String, Object> data = (Map<String, Object>) entry.get(DATA);

            result.add(new LogEntry.Builder(type).timestamp(timestamp).data(data).build());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Map<String, Object>> serialize(Object object) {
        List<Map<String, Object>> result = new LinkedList<>();
        if (object == null) {
            return result;
        }

        List<LogEntry> history = (List<LogEntry>) object;
        history.forEach(logEntry -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put(TIMESTAMP, logEntry.getTimestamp());
            value.put(TYPE, logEntry.getType());

            if (logEntry.getData() != null) {
                value.put(DATA, tidy(logEntry.getData()));
            }

            result.add(value);
        });

        return result;
    }

    /**
     * Returns a "data" map that contains "tidied up" values, as some types are not safe to store 'as is'.
     * See https://github.com/BentoBoxWorld/BentoBox/issues/486
     * @param data the data map to tidy up
     * @return the tidied up data map
     * @since 1.1.1
     */
    @NonNull
    private Map<String, Object> tidy(@NonNull Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof UUID) {
                // UUIDs need some special handling
                result.put(entry.getKey(), entry.getValue().toString());
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
