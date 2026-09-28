package server.model;

import java.io.Serializable;

public class Book implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String title;
    private final String author;
    private final int pages;
    private final String genre;
    private final int year;
    private final double price;

    public Book(String id, String title, String author, int pages, String genre, int year, double price) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.pages = pages;
        this.genre = genre;
        this.year = year;
        this.price = price;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getPages() { return pages; }
    public String getGenre() { return genre; }
    public int getYear() { return year; }
    public double getPrice() { return price; }

    public boolean isUnfinishable() {
        return "goethe".equalsIgnoreCase(safe(author)) || safe(title).contains("faust") || safe(title).contains("فاوست");
    }

    private static String safe(String s) { return s == null ? "" : s.toLowerCase(); }
}
