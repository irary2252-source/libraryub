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

    // --- ✅ 1. 登录接口 ---

    @PostMapping("/login/admin")
    public Map<String, Object> loginAdmin(@RequestBody Map<String, String> payload) {
        boolean success = libraryService.loginAdmin(payload.get("username"), payload.get("password"));
        if (success) {
            return Map.of("success", true, "msg", "登录成功");
        }
        return Map.of("success", false, "msg", "用户名或密码错误");
    }

    @PostMapping("/login/reader")
    public Map<String, Object> loginReader(@RequestBody Map<String, String> payload) {
        boolean success = libraryService.loginReader(payload.get("cardId"), payload.get("password"));
        if (success) {
            return Map.of("success", true, "msg", "登录成功");
        }
        return Map.of("success", false, "msg", "卡号或密码错误");
    }

    // --- ✅ 2. 图书管理接口 ---

    @GetMapping("/books")
    public List<Book> getAllBooks() {
        return bookRepo.findAll();
    }

    @GetMapping("/books/search")
    public List<Book> searchBooks(@RequestParam String keyword) {
        return bookRepo.searchByTitle(keyword);
    }

    @PostMapping("/book")
    public Book addBook(@RequestBody Book book) {
        if (book.getStatus() == null) book.setStatus("在库");
        return bookRepo.save(book);
    }

    // --- ✅ 3. 读者管理接口 ---

    @PostMapping("/reader")
    public Object addReader(@RequestBody Map<String, Object> payload) {
        try {
            Reader reader = new Reader();
            reader.setCardId((String) payload.get("cardId"));
            reader.setName((String) payload.get("name"));
            // 接收密码
            reader.setPassword((String) payload.get("password"));
            reader.setSex((String) payload.get("sex"));
            reader.setType((String) payload.get("type"));
            reader.setLevel((String) payload.get("level"));

            String deptInput = String.valueOf(payload.get("deptId"));
            return libraryService.createReader(reader, deptInput);
        } catch (RuntimeException e) {
            return Map.of("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "注册失败，请检查卡号是否重复");
        }
    }

    @PostMapping("/reader/cancel")
    public Map<String, Object> cancelReader(@RequestBody Map<String, String> payload) {
        try {
            String msg = libraryService.cancelReader(payload.get("cardId"));
            return Map.of("success", true, "msg", msg);
        } catch (Exception e) {
            return Map.of("success", false, "msg", e.getMessage());
        }
    }

    // --- ✅ 4. 业务办理接口 ---

    @PostMapping("/borrow")
    public String borrow(@RequestBody Map<String, String> request) {
        return libraryService.borrowBook(request.get("cardId"), request.get("isbn"));
    }

    @PostMapping("/return")
    public String returnBook(@RequestBody Map<String, String> request) {
        return libraryService.returnBook(request.get("isbn"));
    }

    @PostMapping("/config/update")
    public Map<String, Object> updateConfig(@RequestBody Map<String, String> payload) {
        try {
            String msg = libraryService.updateConfig(payload.get("key"), payload.get("value"));
            return Map.of("success", true, "msg", msg);
        } catch (Exception e) {
            return Map.of("success", false, "msg", "更新失败：" + e.getMessage());
        }
    }


}