package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class LibraryService {
    @Autowired private BookRepository bookRepo;
    @Autowired private ReaderRepository readerRepo;
    @Autowired private BorrowRepository borrowRepo;
    @Autowired private SystemConfigRepository configRepo; // 配置表
    @Autowired private DepartmentRepository deptRepo;     // 单位表

    /**
     * ✅ 辅助方法：智能解析单位输入 (支持 ID 或 名称)
     */
    public Integer resolveDeptId(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new RuntimeException("单位不能为空");
        }

        // 1. 尝试解析为数字 ID
        try {
            int id = Integer.parseInt(input);
            if (deptRepo.existsById(id)) {
                return id;
            }
        } catch (NumberFormatException e) {
            // 忽略异常，说明输入的不是纯数字，继续按名称查找
        }

        // 2. 尝试按名称查找
        Department dept = deptRepo.findByDeptName(input);
        if (dept != null) {
            return dept.getId();
        }

        // 3. 既不是有效ID也不是有效名称
        throw new RuntimeException("单位不存在: " + input);
    }

    /**
     * ✅ 核心功能：创建读者
     * 逻辑更新：博士生待遇 = 教师待遇
     */
    @Transactional
    public Reader createReader(Reader reader, String deptInput) {
        // 1. 解析单位
        Integer finalDeptId = resolveDeptId(deptInput);
        reader.setDeptId(finalDeptId);

        // 2. 确定配置规则前缀 (默认本科生)
        String prefix = "undergrad";

        String type = reader.getType();   // 下拉框: 学生 / 教师
        String level = reader.getLevel(); // 下拉框: 本科生 / 研究生 / 博士生 ...

        // 3. 智能识别身份规则
        // 优先级 A: 教师组
        // 包含情况：类型选了"教师" OR 级别里包含 "教师", "讲师", "教授", "博士"
        if ("教师".equals(type) ||
                (level != null && (level.contains("教师") || level.contains("讲师") || level.contains("教授") || level.contains("博士")))) {
            prefix = "teacher";
        }
        // 优先级 B: 研究生组
        // 包含情况：级别里包含 "研究生", "硕士"
        else if (level != null && (level.contains("研究生") || level.contains("硕士"))) {
            prefix = "grad";
        }
        // 其他情况（如"本科生"）默认使用 "undergrad"

        // 4. 从数据库读取规则 (SystemConfig)
        // 比如博士生会读取 teacher_max_borrow (20) 和 teacher_borrow_days (90)
        String maxStr = configRepo.getValue(prefix + "_max_borrow", "5");
        String daysStr = configRepo.getValue(prefix + "_borrow_days", "30");

        // 5. 将规则写入该读者的记录中
        reader.setMaxBorrow(Integer.parseInt(maxStr));
        reader.setBorrowDays(Integer.parseInt(daysStr));

        // 6. 初始化其他状态
        reader.setCurrentBorrow(0);
        if (reader.getActive() == null) {
            reader.setActive(true);
        }

        // 7. 保存并返回
        return readerRepo.save(reader);
    }

    /**
     * ✅ 借书逻辑
     */
    @Transactional
    public String borrowBook(String cardId, String isbn) {
        // 1. 基础检查
        Reader reader = readerRepo.findById(cardId).orElse(null);
        if (reader == null) return "读者不存在";
        if (!Boolean.TRUE.equals(reader.getActive())) return "借书证已失效";

        // 2. 检查借阅上限 (从 reader 表读取该人的专属限额)
        int current = reader.getCurrentBorrow() == null ? 0 : reader.getCurrentBorrow();
        int max = reader.getMaxBorrow() == null ? 5 : reader.getMaxBorrow();

        if (current >= max) {
            return "借书数量已达上限 (" + max + "本)";
        }

        // 3. 检查图书状态
        Book book = bookRepo.findById(isbn).orElse(null);
        if (book == null || "借出".equals(book.getStatus())) {
            return "图书不存在或已借出";
        }

        // 4. 创建借阅记录
        Borrow borrow = new Borrow();
        borrow.setCardId(cardId);
        borrow.setIsbn(isbn);
        borrow.setBorrowTime(LocalDateTime.now());

        // 动态设置应还日期：当前时间 + 读者可借天数
        int days = reader.getBorrowDays() == null ? 30 : reader.getBorrowDays();
        borrow.setDueDate(LocalDateTime.now().plusDays(days));

        borrowRepo.save(borrow);

        // 5. 更新图书状态 -> "借出"
        book.setStatus("借出");
        bookRepo.save(book);

        // 6. 更新读者当前借阅量 (+1)
        reader.setCurrentBorrow(current + 1);
        readerRepo.save(reader);

        return "借阅成功";
    }

    /**
     * ✅ 还书逻辑
     */
    @Transactional
    public String returnBook(String isbn) {
        // 1. 查找未还记录
        Borrow borrow = borrowRepo.findByIsbnAndReturnTimeIsNull(isbn);
        if (borrow == null) return "未找到该书的在借记录";

        // 2. 设置归还时间
        borrow.setReturnTime(LocalDateTime.now());

        // 3. 动态读取罚款率 (默认 1.00 元/天)
        String fineRateStr = configRepo.getValue("overdue_fine_rate", "1.00");
        BigDecimal dailyFineRate = new BigDecimal(fineRateStr);

        // 4. 计算逾期罚款
        if (LocalDateTime.now().isAfter(borrow.getDueDate())) {
            long overdueDays = Duration.between(borrow.getDueDate(), LocalDateTime.now()).toDays();
            // 如果逾期天数 > 0，则计算罚金
            if(overdueDays > 0) {
                borrow.setFineAmount(dailyFineRate.multiply(BigDecimal.valueOf(overdueDays)));
            }
        }
        borrowRepo.save(borrow);

        // 5. 更新图书状态 -> "在库"
        Book book = bookRepo.findById(isbn).orElse(null);
        if (book != null) {
            book.setStatus("在库");
            bookRepo.save(book);
        }

        // 6. 更新读者当前借阅量 (-1)
        Reader reader = readerRepo.findById(borrow.getCardId()).orElse(null);
        if (reader != null) {
            int current = reader.getCurrentBorrow() == null ? 1 : reader.getCurrentBorrow();
            if (current > 0) {
                reader.setCurrentBorrow(current - 1);
                readerRepo.save(reader);
            }
        }

        // 7. 返回结果信息
        String msg = "归还成功";
        if (borrow.getFineAmount() != null && borrow.getFineAmount().doubleValue() > 0) {
            msg += "，已逾期，产生罚款：" + borrow.getFineAmount() + "元";
        }
        return msg;
    }
}