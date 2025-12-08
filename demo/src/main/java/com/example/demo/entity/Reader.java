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

    // ✅ 修改重点 1：添加 Sex 字段映射
    // ✅ 修改重点 2：直接在 Java 里赋值 "男"，防止 Hibernate 传 null 导致报错
    @Column(name = "Sex")
    private String sex = "男";

    @Column(name = "Level")
    private String level;

    @Column(name = "IsActive")
    private Boolean isActive = true;

    // --- Getters and Setters ---
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // ✅ 记得添加 Sex 的 Getter/Setter
    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }
}