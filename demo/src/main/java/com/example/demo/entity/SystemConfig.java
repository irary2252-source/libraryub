package com.example.demo.entity;
import jakarta.persistence.*;

@Entity
@Table(name = "systemconfig")
public class SystemConfig {
    @Id @Column(name = "ConfigKey") private String configKey;
    @Column(name = "ConfigValue") private String configValue;
    @Column(name = "Description") private String description;
    // Getters
    public String getConfigValue() { return configValue; }
}