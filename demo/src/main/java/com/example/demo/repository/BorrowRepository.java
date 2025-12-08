package com.example.demo.repository;
import com.example.demo.entity.Borrow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BorrowRepository extends JpaRepository<Borrow, Long> {
    List<Borrow> findByCardIdAndReturnTimeIsNull(String cardId);
    Borrow findByIsbnAndReturnTimeIsNull(String isbn);
}