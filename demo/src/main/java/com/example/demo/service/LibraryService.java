package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LibraryService {
    @Autowired private BookRepository bookRepo;
    @Autowired private ReaderRepository readerRepo;
    @Autowired private BorrowRepository borrowRepo;
    @Autowired private SystemConfigRepository configRepo;
    @Autowired private DepartmentRepository deptRepo;
    @Autowired private AdminRepository adminRepo;
    @Autowired private PurchaseRequestRepository requestRepo; // ✅ 新增：荐购Repo注入

    /**
     * 管理员登录
     */
    public boolean loginAdmin(String username, String password) {
        return adminRepo.login(username, password) != null;
    }

    /**
     * 读者登录
     */
    public boolean loginReader(String cardId, String password) {
        return readerRepo.login(cardId, password) != null;
    }

    /**
     * 辅助方法：智能解析单位输入
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
     * 创建读者
     */
    @Transactional
    public Reader createReader(Reader reader, String deptInput) {
        Integer finalDeptId = resolveDeptId(deptInput);
        reader.setDeptId(finalDeptId);

        if (reader.getPassword() == null || reader.getPassword().trim().isEmpty()) {
            reader.setPassword("123456");
        }

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
        borrow.setIsPaid(true);
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

        List<Borrow> allHistory = borrowRepo.findAll();
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

    /**
     * 修改读者密码
     */
    @Transactional
    public String updateReaderPassword(String cardId, String oldPass, String newPass) {
        Reader reader = readerRepo.findById(cardId).orElse(null);
        if (reader == null) return "用户不存在";

        if (!reader.getPassword().equals(oldPass)) {
            throw new RuntimeException("旧密码错误");
        }

        reader.setPassword(newPass);
        readerRepo.save(reader);
        return "密码修改成功，请重新登录";
    }

    /**
     * 修改管理员密码
     */
    @Transactional
    public String updateAdminPassword(String username, String oldPass, String newPass) {
        Admin admin = adminRepo.login(username, oldPass);
        if (admin == null) {
            throw new RuntimeException("旧密码错误或用户不存在");
        }

        admin.setPassword(newPass);
        adminRepo.save(admin);
        return "密码修改成功，请重新登录";
    }

    // =====================================================================
    //  ✅ 读者自助与查询功能
    // =====================================================================

    /**
     * 获取读者当前借阅列表
     */
    public List<Map<String, Object>> getReaderBorrowings(String cardId) {
        List<Borrow> borrows = borrowRepo.findByCardIdAndReturnTimeIsNull(cardId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Borrow b : borrows) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("isbn", b.getIsbn());
            map.put("borrowTime", b.getBorrowTime());
            map.put("dueTime", b.getDueDate());

            Book book = bookRepo.findById(b.getIsbn()).orElse(null);
            map.put("title", book != null ? book.getTitle() : "未知图书");

            long overdue = 0;
            if (LocalDateTime.now().isAfter(b.getDueDate())) {
                overdue = Duration.between(b.getDueDate(), LocalDateTime.now()).toDays();
            }
            map.put("overdueDays", overdue);

            result.add(map);
        }
        return result;
    }

    /**
     * 获取读者未缴纳罚款的记录
     */
    public List<Map<String, Object>> getReaderFines(String cardId) {
        List<Borrow> borrows = borrowRepo.findUnpaidFines(cardId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Borrow b : borrows) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getId());
            map.put("isbn", b.getIsbn());
            map.put("returnTime", b.getReturnTime());
            map.put("fineAmount", b.getFineAmount());

            Book book = bookRepo.findById(b.getIsbn()).orElse(null);
            map.put("title", book != null ? book.getTitle() : "未知图书");

            result.add(map);
        }
        return result;
    }

    /**
     * 读者自助还书 (根据 BorrowID 还书)
     */
    @Transactional
    public String returnBookById(Long borrowId) {
        Borrow borrow = borrowRepo.findById(borrowId).orElse(null);
        if (borrow == null) return "借阅记录不存在";
        if (borrow.getReturnTime() != null) return "该书已归还，无需重复操作";

        return returnBookLogic(borrow);
    }

    /**
     * 缴纳罚款
     */
    @Transactional
    public String payFine(Long borrowId) {
        Borrow borrow = borrowRepo.findById(borrowId).orElse(null);
        if (borrow == null) return "记录不存在";
        if (Boolean.TRUE.equals(borrow.getIsPaid())) return "该费用已结清";
        if (borrow.getFineAmount() == null || borrow.getFineAmount().doubleValue() <= 0) return "无罚款需缴纳";

        borrow.setIsPaid(true);
        borrowRepo.save(borrow);
        return "缴费成功！";
    }

    /**
     * 管理员还书接口 (复用公共逻辑)
     */
    @Transactional
    public String returnBook(String isbn) {
        Borrow borrow = borrowRepo.findByIsbnAndReturnTimeIsNull(isbn);
        if (borrow == null) return "未找到该书的在借记录";

        String result = returnBookLogic(borrow);

        if (borrow.getFineAmount() != null && borrow.getFineAmount().doubleValue() > 0) {
            result += " (⚠️ 注意：请向读者收取罚款 " + borrow.getFineAmount() + " 元)";
        }
        return result;
    }

    /**
     * 🔧 [核心逻辑] 还书处理
     * ✅ 已更新：罚金不超过书本价格
     */
    private String returnBookLogic(Borrow borrow) {
        borrow.setReturnTime(LocalDateTime.now());

        Book book = bookRepo.findById(borrow.getIsbn()).orElse(null);

        long overdueDays = 0;
        if (LocalDateTime.now().isAfter(borrow.getDueDate())) {
            overdueDays = Duration.between(borrow.getDueDate(), LocalDateTime.now()).toDays();
        }
        borrow.setOverdueDays((int) overdueDays);

        if (overdueDays > 0) {
            String fineRateStr = configRepo.getValue("overdue_fine_rate", "1.00");
            BigDecimal dailyFineRate = new BigDecimal(fineRateStr);
            BigDecimal fine = dailyFineRate.multiply(BigDecimal.valueOf(overdueDays));

            // 罚金封顶逻辑
            if (book != null && book.getPrice() != null) {
                if (fine.compareTo(book.getPrice()) > 0) {
                    fine = book.getPrice();
                }
            }

            borrow.setFineAmount(fine);
            borrow.setIsPaid(false);
        } else {
            borrow.setFineAmount(BigDecimal.ZERO);
            borrow.setIsPaid(true);
        }
        borrowRepo.save(borrow);

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
        if (overdueDays > 0) {
            msg = "您已逾期 " + overdueDays + " 天，产生罚款 " + borrow.getFineAmount() + " 元";
        }
        return msg;
    }

    // =====================================================================
    //  ✅ [新增] 图书荐购功能 (完整版)
    // =====================================================================

    /**
     * 1. 读者提交荐购
     */
    @Transactional
    public String addRecommendation(PurchaseRequest req) {
        if (bookRepo.existsById(req.getIsbn())) {
            return "提交失败：馆内已有此书，无需荐购";
        }
        req.setStatus("待处理");
        req.setRequestDate(LocalDateTime.now());
        requestRepo.save(req);
        return "荐购提交成功，请等待管理员审核";
    }

    /**
     * 2. 获取荐购列表
     * readerId 不为空则查该读者的，为空则查所有待处理的(管理员用)
     */
    public List<PurchaseRequest> getRecommendations(String readerId) {
        if (readerId != null && !readerId.isEmpty()) {
            return requestRepo.findByReaderIdOrderByRequestDateDesc(readerId);
        } else {
            return requestRepo.findByStatusOrderByRequestDateDesc("待处理");
        }
    }

    /**
     * 3. 管理员处理荐购 (批准/驳回)
     */
    @Transactional
    public String handleRecommendation(Integer requestId, boolean isApproved) {
        PurchaseRequest req = requestRepo.findById(requestId).orElse(null);
        if (req == null) return "记录不存在";
        if (!"待处理".equals(req.getStatus())) return "该请求已处理";

        if (isApproved) {
            // ✅ 批准逻辑：自动将书加入馆藏
            if (bookRepo.existsById(req.getIsbn())) {
                req.setStatus("已批准(已有)");
            } else {
                Book newBook = new Book();
                newBook.setIsbn(req.getIsbn());
                newBook.setTitle(req.getTitle());
                newBook.setAuthor(req.getAuthor());
                newBook.setPublisher(req.getPublisher());
                newBook.setPrice(req.getPrice());
                newBook.setStatus("在库");
                newBook.setCategory("荐购新书"); // 默认分类
                bookRepo.save(newBook);

                req.setStatus("已批准");
            }
        } else {
            req.setStatus("已驳回");
        }

        requestRepo.save(req);
        return isApproved ? "已批准并自动入库" : "已驳回该请求";
    }
}