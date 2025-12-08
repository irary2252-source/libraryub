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

        // ✅ 修改逻辑：判断状态字符串
        if (book == null || "借出".equals(book.getStatus())) {
            return "图书不可借";
        }

        // 注意：这里简单假设 level 是数字字符串，或者您需要根据实际情况解析
        // 如果 level 存的是 "本科生"，您可能需要一个 map 来转换成可借数量
        int maxBorrow = 5; // 默认值
        // if ("本科生".equals(reader.getLevel())) maxBorrow = 10;

        List<Borrow> currentBorrows = borrowRepo.findByCardIdAndReturnTimeIsNull(cardId);
        if (currentBorrows.size() >= maxBorrow) return "借书数量超限";

        Borrow borrow = new Borrow();
        borrow.setCardId(cardId);
        borrow.setIsbn(isbn);
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setDueDate(LocalDateTime.now().plusDays(30));
        borrowRepo.save(borrow);

        // ✅ 修改状态设置：设为 "借出"
        book.setStatus("借出");
        bookRepo.save(book);
        return "借阅成功";
    }

    @Transactional
    public String returnBook(String isbn) {
        Borrow borrow = borrowRepo.findByIsbnAndReturnTimeIsNull(isbn);
        if (borrow == null) return "未找到借阅记录";

        borrow.setReturnTime(LocalDateTime.now());
        if (LocalDateTime.now().isAfter(borrow.getDueDate())) {
            long days = Duration.between(borrow.getDueDate(), LocalDateTime.now()).toDays();
            if(days > 0) borrow.setFineAmount(dailyFineRate.multiply(BigDecimal.valueOf(days)));
        }
        borrowRepo.save(borrow);

        Book book = bookRepo.findById(isbn).get();
        // ✅ 修改状态设置：设为 "在库"
        book.setStatus("在库");
        bookRepo.save(book);
        return "归还成功";
    }
}