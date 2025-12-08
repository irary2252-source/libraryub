package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
public class Reader {
    @Id
    private String cardId;
    private String name;
    private String gender;
    private String unit;
    private String readerType;
    private Integer level;
    private Boolean isActive = true;

    // Getters and Setters
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getName() { return name; } // 补上 getName
    public void setName(String name) { this.name = name; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Boolean getActive() { return isActive; }
    public void setActive(Boolean active) { isActive = active; }
}