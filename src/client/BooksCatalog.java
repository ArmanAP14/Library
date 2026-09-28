package client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BooksCatalog {

    private static final Gson gson = new Gson();
    private static final Type OBJECT_TYPE = new TypeToken<Object>() {}.getType();

    private BooksCatalog() {}

    public static List<Map<String, Object>> loadDefault() throws IOException {
        return loadFrom(Paths.get("books.json"));
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> loadFrom(Path path) throws IOException {
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        Object parsed = gson.fromJson(content, OBJECT_TYPE);

        List<Object> items;
        if (parsed instanceof List) {
            items = (List<Object>) parsed;
        } else if (parsed instanceof Map && ((Map<String, Object>) parsed).get("books") instanceof List) {
            items = (List<Object>) ((Map<String, Object>) parsed).get("books");
        } else {
            throw new IOException("books.json must be a JSON array of book objects.");
        }

        List<Map<String, Object>> books = new ArrayList<>();
        for (Object o : items) {
            Map<String, Object> m = (Map<String, Object>) o;
            Map<String, Object> book = new LinkedHashMap<>();
            book.put("id", firstNonNull(m, "id", "bookId", "ID"));
            book.put("title", firstNonNull(m, "title", "name"));
            book.put("author", firstNonNull(m, "author", "writer"));
            book.put("genre", firstNonNull(m, "genre", "category"));
            book.put("pages", intOf(m, "pages", "pageCount", "totalPages"));
            book.put("year", intOf(m, "year", "publishYear", "publicationYear"));
            book.put("price", doubleOf(m, "price", "cost"));
            books.add(book);
        }
        return books;
    }

    private static String firstNonNull(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null) return v.toString();
        }
        return null;
    }

    private static int intOf(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) try { return Integer.parseInt(v.toString()); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private static double doubleOf(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v != null) try { return Double.parseDouble(v.toString()); } catch (NumberFormatException ignored) {}
        }
        return 0.0;
    }
}
