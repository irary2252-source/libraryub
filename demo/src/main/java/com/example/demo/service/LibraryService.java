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
    @Autowired private SystemConfigRepository configRepo;
    @Autowired private DepartmentRepository deptRepo;
    @Autowired private AdminRepository adminRepo; // ✅ 新增：注入管理员Repo

    /**
     * ✅ 新增功能：管理员登录
     */
    public boolean loginAdmin(String username, String password) {
        // 调用 Repository 的 SQL 方法
        return adminRepo.login(username, password) != null;
    }

    /**
     * ✅ 新增功能：读者登录
     */
    public boolean loginReader(String cardId, String password) {
        // 调用 Repository 的 SQL 方法
        return readerRepo.login(cardId, password) != null;
    }

    /**
     * 辅助方法：智能解析单位输入 (支持 ID 或 名称)
     */
    public Integer resolveDeptId(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new RuntimeException("单位不能为空");
        }
        try {
            int id = Integer.parseInt(input);
            if (deptRepo.existsById(id)) return id;
        } catch (NumberFormatException e) {
            // 忽略非数字异常
        }
        Department dept = deptRepo.findByDeptName(input);
        if (dept != null) return dept.getId();
        throw new RuntimeException("单位不存在: " + input);
    }

    /**
     * 创建读者 (已更新：支持密码设置)
     */
    @Transactional
    public Reader createReader(Reader reader, String deptInput) {
        Integer finalDeptId = resolveDeptId(deptInput);
        reader.setDeptId(finalDeptId);

        // ✅ 新增：密码处理 (如果为空，设为默认 123456)
        if (reader.getPassword() == null || reader.getPassword().trim().isEmpty()) {
            reader.setPassword("123456");
        }

        // 身份规则解析
        String prefix = "undergrad";
        String type = reader.getType();
        String level = reader.getLevel();

        if ("教师".equals(type) ||
                (level != null && (level.contains("教师") || level.contains("讲师") || level.contains("教授") || level.contains("博士")))) {
            prefix = "teacher";
        } else if (level != null && (level.contains("研究生") || level.contains("硕士"))) {
            prefix = "grad";
        }

        String maxStr = configRepo.getValue(prefix + "_max_borrow", "5");
        String daysStr = configRepo.getValue(prefix + "_borrow_days", "30");

        reader.setMaxBorrow(Integer.parseInt(maxStr));
        reader.setBorrowDays(Integer.parseInt(daysStr));
        reader.setCurrentBorrow(0);
        if (reader.getActive() == null) reader.setActive(true);

        return readerRepo.save(reader);
    }

    /**
     * 借书逻辑
     */
    @Transactional
    public String borrowBook(String cardId, String isbn) {
        Reader reader = readerRepo.findById(cardId).orElse(null);
        if (reader == null) return "读者不存在";
        if (!Boolean.TRUE.equals(reader.getActive())) return "借书证已失效";

        int current = reader.getCurrentBorrow() == null ? 0 : reader.getCurrentBorrow();
        int max = reader.getMaxBorrow() == null ? 5 : reader.getMaxBorrow();

        if (current >= max) return "借书数量已达上限 (" + max + "本)";

        Book book = bookRepo.findById(isbn).orElse(null);
        if (book == null || "借出".equals(book.getStatus())) return "图书不存在或已借出";

        Borrow borrow = new Borrow();
        borrow.setCardId(cardId);
        borrow.setIsbn(isbn);
        borrow.setBorrowTime(LocalDateTime.now());
        borrow.setIsPaid(true); // 借书时默认无罚款
        borrow.setOverdueDays(0);

        int days = reader.getBorrowDays() == null ? 30 : reader.getBorrowDays();
        borrow.setDueDate(LocalDateTime.now().plusDays(days));

        borrowRepo.save(borrow);

        book.setStatus("借出");
        bookRepo.save(book);

        reader.setCurrentBorrow(current + 1);
        readerRepo.save(reader);

        return "借阅成功";
    }

    /**
     * 还书逻辑 (含罚款计算)
     */
    @Transactional
    public String returnBook(String isbn) {
        Borrow borrow = borrowRepo.findByIsbnAndReturnTimeIsNull(isbn);
        if (borrow == null) return "未找到该书的在借记录";

        borrow.setReturnTime(LocalDateTime.now());

        long overdueDays = 0;
        if (LocalDateTime.now().isAfter(borrow.getDueDate())) {
            overdueDays = Duration.between(borrow.getDueDate(), LocalDateTime.now()).toDays();
        }

        borrow.setOverdueDays((int) overdueDays);

        if (overdueDays > 0) {
            String fineRateStr = configRepo.getValue("overdue_fine_rate", "1.00");
            BigDecimal dailyFineRate = new BigDecimal(fineRateStr);
            BigDecimal fine = dailyFineRate.multiply(BigDecimal.valueOf(overdueDays));

            borrow.setFineAmount(fine);
            borrow.setIsPaid(false); // 有罚款，未支付
        } else {
            borrow.setFineAmount(BigDecimal.ZERO);
            borrow.setIsPaid(true);  // 无罚款，已结清
        }

        borrowRepo.save(borrow);

        Book book = bookRepo.findById(isbn).orElse(null);
        if (book != null) {
            book.setStatus("在库");
            bookRepo.save(book);
        }

        Reader reader = readerRepo.findById(borrow.getCardId()).orElse(null);
        if (reader != null) {
            int current = reader.getCurrentBorrow() == null ? 1 : reader.getCurrentBorrow();
            if (current > 0) {
                reader.setCurrentBorrow(current - 1);
                readerRepo.save(reader);
            }
        }

        String msg = "归还成功";
        if (borrow.getFineAmount() != null && borrow.getFineAmount().doubleValue() > 0) {
            msg += "，已逾期 " + overdueDays + " 天，产生罚款：" + borrow.getFineAmount() + "元";
        }
        return msg;
    }

    /**
     * 注销读者
     */
    @Transactional
    public String cancelReader(String cardId) {
        Reader reader = readerRepo.findById(cardId).orElse(null);
        if (reader == null) throw new RuntimeException("读者不存在");

        var unreturnedBooks = borrowRepo.findByCardIdAndReturnTimeIsNull(cardId);
        if (!unreturnedBooks.isEmpty()) {
            throw new RuntimeException("注销失败：该读者仍有 " + unreturnedBooks.size() + " 本书未归还！");
        }

        List<Borrow> allHistory = borrowRepo.findAll(); // 简单实现，实际可用SQL优化
        boolean hasUnpaidFine = allHistory.stream()
                .filter(b -> b.getCardId().equals(cardId))
                .anyMatch(b -> Boolean.FALSE.equals(b.getIsPaid()) &&
                        b.getFineAmount() != null &&
                        b.getFineAmount().doubleValue() > 0);

        if (hasUnpaidFine) {
            throw new RuntimeException("注销失败：该读者有未缴纳的罚款！");
        }

        reader.setActive(false);
        readerRepo.save(reader);

        return "读者 " + reader.getName() + " (" + cardId + ") 已成功注销";
    }

    /**
     * 更新配置
     */
    @Transactional
    public String updateConfig(String key, String value) {
        SystemConfig config = configRepo.findById(key).orElse(new SystemConfig());
        if(config.getConfigKey() == null) {
            config.setConfigKey(key);
            config.setDescription("User Setting");
        }
        config.setConfigValue(value);
        configRepo.save(config);
        return "配置已更新";
    }
}