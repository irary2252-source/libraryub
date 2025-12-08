package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LibraryService {
    @Autowired private BookRepository bookRepo;
    @Autowired private ReaderRepository readerRepo;
    @Autowired private BorrowRepository borrowRepo;

    private BigDecimal dailyFineRate = new BigDecimal("1.00");

    @Transactional
    public String borrowBook(String cardId, String isbn) {
        Reader reader = readerRepo.findById(cardId).orElse(null);
        if (reader == null || !reader.getActive()) return "读者无效";

        Book book = bookRepo.findById(isbn).orElse(null);
        if (book == null || book.getStatus() == 1) return "图书不可借";

        List<Borrow> currentBorrows = borrowRepo.findByCardIdAndReturnTimeIsNull(cardId);
        if (currentBorrows.size() >= (reader.getLevel() * 3)) return "借书数量超限";

        Borrow borrow = new Borrow();
        borrow.setCardId(cardId);
        borrow.setIsbn(isbn);
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setDueDate(LocalDateTime.now().plusDays(30));
        borrowRepo.save(borrow);

        book.setStatus(1);
        bookRepo.save(book);
        return "借阅成功";
    }

    @Transactional
    public String returnBook(String isbn) {
        Borrow borrow = borrowRepo.findByIsbnAndReturnTimeIsNull(isbn);
        if (borrow == null) return "未找到借阅记录";

        borrow.setReturnTime(LocalDateTime.now());
        // 简单计算罚款逻辑
        if (LocalDateTime.now().isAfter(borrow.getDueDate())) {
            long days = Duration.between(borrow.getDueDate(), LocalDateTime.now()).toDays();
            if(days > 0) borrow.setFineAmount(dailyFineRate.multiply(BigDecimal.valueOf(days)));
        }
        borrowRepo.save(borrow);

        Book book = bookRepo.findById(isbn).get();
        book.setStatus(0);
        bookRepo.save(book);
        return "归还成功";
    }
}