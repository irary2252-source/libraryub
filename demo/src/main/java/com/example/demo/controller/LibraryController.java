package com.example.demo.controller;

import com.example.demo.entity.Book;
import com.example.demo.entity.Reader;
import com.example.demo.repository.BookRepository;
import com.example.demo.repository.ReaderRepository;
import com.example.demo.service.LibraryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LibraryController {

    @Autowired private LibraryService libraryService;
    @Autowired private BookRepository bookRepo;
    @Autowired private ReaderRepository readerRepo;

    // 1. 获取所有图书列表
    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookRepo.findAll();
    }

    // 2. 添加图书 (图书登记)
    @PostMapping("/book")
    public Book addBook(@RequestBody Book book) {
        return bookRepo.save(book);
    }

    // 3. 添加读者 (读者登记)
    @PostMapping("/reader")
    public Reader addReader(@RequestBody Reader reader) {
        // ✅ 现在的代码非常干净，直接保存即可
        // 如果前端没传性别，Reader 实体类里默认就是 "男"
        return readerRepo.save(reader);
    }

    // 4. 借书操作
    // 前端发送 JSON: { "cardId": "1001", "isbn": "978-7-111" }
    @PostMapping("/borrow")
    public String borrow(@RequestBody Map<String, String> request) {
        String cardId = request.get("cardId");
        String isbn = request.get("isbn");
        return libraryService.borrowBook(cardId, isbn);
    }

    // 5. 还书操作
    // 前端发送 JSON: { "isbn": "978-7-111" }
    @PostMapping("/return")
    public String returnBook(@RequestBody Map<String, String> request) {
        String isbn = request.get("isbn");
        return libraryService.returnBook(isbn);
    }
}