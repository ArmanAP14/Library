package server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import server.model.Book;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class BookCatalog {

    private static final Gson gson = new Gson();
    private static final Type OBJECT_TYPE = new TypeToken<Object>() {}.getType();

    private final Map<String, Book> booksById = new LinkedHashMap<>();

    @SuppressWarnings("unchecked")
    public static BookCatalog loadFrom(String path) throws IOException {
        BookCatalog catalog = new BookCatalog();
        String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        Object parsed = gson.fromJson(content, OBJECT_TYPE);

        List<Object> items;
        if (parsed instanceof List) {
            items = (List<Object>) parsed;
        } else if (parsed instanceof Map && ((Map<String, Object>) parsed).get("books") instanceof List) {
            items = (List<Object>) ((Map<String, Object>) parsed).get("books");
        } else {
            throw new IOException("books.json must be a JSON array of book objects (or an object with a 'books' array).");
        }

        for (Object o : items) {
            Map<String, Object> m = (Map<String, Object>) o;
            String id = firstNonNull(m, "id", "bookId", "ID");
            String title = firstNonNull(m, "title", "name");
            String author = firstNonNull(m, "author", "writer");
            String genre = firstNonNull(m, "genre", "category");
            int pages = intOf(m, "pages", "pageCount", "totalPages");
            int year = intOf(m, "year", "publishYear", "publicationYear");
            double price = doubleOf(m, "price", "cost");
            catalog.booksById.put(id, new Book(id, title, author, pages, genre, year, price));
        }
        return catalog;
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

    public Book get(String id) { return booksById.get(id); }
    public Collection<Book> all() { return booksById.values(); }
    public boolean exists(String id) { return booksById.containsKey(id); }
}
