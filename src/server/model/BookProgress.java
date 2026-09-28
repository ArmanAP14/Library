package server.model;

import java.io.Serializable;

public class BookProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { NOT_READ, READING, READ }

    private final String bookId;
    private int lastPage;
    private Status status;

    public BookProgress(String bookId) {
        this.bookId = bookId;
        this.lastPage = 0;
        this.status = Status.NOT_READ;
    }

    public String getBookId() { return bookId; }
    public int getLastPage() { return lastPage; }
    public Status getStatus() { return status; }

    public void setLastPage(int lastPage) { this.lastPage = lastPage; }
    public void setStatus(Status status) { this.status = status; }
}
