package com.example.demo.repository;

import com.example.demo.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BookRepository extends JpaRepository<Book, String> {

    @Override
    @Query(value = "SELECT * FROM book", nativeQuery = true)
    List<Book> findAll();

    // ✅ 新增：使用 SQL 进行模糊搜索 (书名包含关键字)
    @Query(value = "SELECT * FROM book WHERE Title LIKE CONCAT('%', :keyword, '%')", nativeQuery = true)
    List<Book> searchByTitle(@Param("keyword") String keyword);
}