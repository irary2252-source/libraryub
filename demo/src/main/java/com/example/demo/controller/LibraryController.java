package com.example.demo.controller;

import com.example.demo.entity.Book;
import com.example.demo.entity.Reader;
import com.example.demo.repository.BookRepository;
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

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookRepo.findAll();
    }

    @PostMapping("/book")
    public Book addBook(@RequestBody Book book) {
        if (book.getStatus() == null) book.setStatus("在库");
        return bookRepo.save(book);
    }

    // ✅ 修改：接收 Map，处理单位输入
    @PostMapping("/reader")
    public Object addReader(@RequestBody Map<String, Object> payload) {
        try {
            // 1. 手动提取字段构建 Reader 对象
            Reader reader = new Reader();
            reader.setCardId((String) payload.get("cardId"));
            reader.setName((String) payload.get("name"));
            reader.setSex((String) payload.get("sex"));
            reader.setType((String) payload.get("type"));
            reader.setLevel((String) payload.get("level"));

            // 2. 提取单位输入 (可能是数字 1，也可能是字符串 "计算机系")
            String deptInput = String.valueOf(payload.get("deptId"));

            // 3. 调用 Service 的新方法
            return libraryService.createReader(reader, deptInput);

        } catch (RuntimeException e) {
            // 捕获“单位不存在”等业务错误并返回给前端
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "注册失败，请检查卡号是否重复");
        }
    }

    @PostMapping("/borrow")
    public String borrow(@RequestBody Map<String, String> request) {
        return libraryService.borrowBook(request.get("cardId"), request.get("isbn"));
    }

    @PostMapping("/return")
    public String returnBook(@RequestBody Map<String, String> request) {
        return libraryService.returnBook(request.get("isbn"));
    }
}