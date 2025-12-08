package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "book")
public class Book {
    @Id
    @Column(name = "BookID") // 对应数据库 BookID
    private String isbn;

    @Column(name = "Title")
    private String title;

    @Column(name = "Author")
    private String author;

    @Column(name = "Price")
    private BigDecimal price;

    @Column(name = "Publisher")
    private String publisher;

    @Column(name = "Summary")
    private String summary;

    // ✅ 修正：类型为 String，默认值为 "在库"
    @Column(name = "Status")
    private String status = "在库";

    // --- Getters and Setters ---
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    // ✅ 修正 Getter/Setter 类型
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}