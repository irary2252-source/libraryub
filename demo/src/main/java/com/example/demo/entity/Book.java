package com.example.demo.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "book") // 明确表名
public class Book {
    @Id
    @Column(name = "BookID") // ✅ 数据库里叫 BookID，不是 isbn
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

    // ✅ 重点修改：类型改为 String，默认值为 "在库"
    @Column(name = "Status")
    private String status = "在库";

    // Getters and Setters
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    // ✅ 修改 Getter/Setter 类型为 String
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}