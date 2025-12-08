package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reader")
public class Reader {
    @Id
    @Column(name = "CardID")
    private String cardId;

    @Column(name = "name")
    private String name;

    @Column(name = "Sex")
    private String sex = "男"; // 有默认值

    // ✅ 新增：映射数据库的 DeptID 列
    @Column(name = "DeptID")
    private Integer deptId;

    // ✅ 新增：映射数据库的 Type 列
    @Column(name = "Type")
    private String type = "学生"; // 给一个默认值

    @Column(name = "Level")
    private String level;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    // --- Getters and Setters ---
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    // ✅ 新增 Getter/Setter
    public Integer getDeptId() { return deptId; }
    public void setDeptId(Integer deptId) { this.deptId = deptId; }

    // ✅ 新增 Getter/Setter
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }
}