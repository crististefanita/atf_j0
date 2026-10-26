package atf.validation.ai;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Internal defensive-copy utility; it is deliberately not part of the public AI API. */
final class AiEvaluationValues {
    private AiEvaluationValues() { }

    static Object immutable(Object value) {
        if (value == null || value instanceof String || value instanceof Boolean || value instanceof Enum<?>
                || value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long
                || value instanceof Float || value instanceof Double || value instanceof BigInteger || value instanceof BigDecimal) {
            return value;
        }
        if (value instanceof Map<?, ?> source) {
            var copy = new LinkedHashMap<String, Object>();
            source.forEach((key, item) -> {
                if (!(key instanceof String text)) throw new IllegalArgumentException("AI field objects require string keys");
                copy.put(text, immutable(item));
            });
            return Collections.unmodifiableMap(copy);
        }
        if (value instanceof List<?> source) {
            var copy = new ArrayList<Object>(source.size());
            source.forEach(item -> copy.add(immutable(item)));
            return Collections.unmodifiableList(copy);
        }
        throw new IllegalArgumentException("unsupported AI field value type: " + value.getClass().getName());
    }
}
