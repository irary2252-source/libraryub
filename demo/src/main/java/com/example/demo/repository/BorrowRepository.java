package com.example.demo.repository;

import com.example.demo.entity.Borrow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BorrowRepository extends JpaRepository<Borrow, Long> {

    // ✅ 修改：使用 SQL 查询某人未还的书
    // 对应数据库字段: CardID, ReturnTime
    @Query(value = "SELECT * FROM borrow WHERE CardID = :cardId AND ReturnTime IS NULL", nativeQuery = true)
    List<Borrow> findByCardIdAndReturnTimeIsNull(@Param("cardId") String cardId);

    // ✅ 修改：使用 SQL 查询某本书当前的借阅记录
    // 对应数据库字段: BookID, ReturnTime
    @Query(value = "SELECT * FROM borrow WHERE BookID = :isbn AND ReturnTime IS NULL", nativeQuery = true)
    Borrow findByIsbnAndReturnTimeIsNull(@Param("isbn") String isbn);
}