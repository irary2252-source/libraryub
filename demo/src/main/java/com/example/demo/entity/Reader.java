package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "reader")
public class Reader {
    @Id
    @Column(name = "CardID") // ✅ 对应数据库 CardID
    private String cardId;

    @Column(name = "Name")
    private String name;

    @Column(name = "Level")
    private String level; // ✅ 改为 String，对应数据库 '本科生'

    @Column(name = "IsActive")
    private Boolean isActive = true;

    // Getters and Setters
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // ✅ 修改 Getter/Setter
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }
}