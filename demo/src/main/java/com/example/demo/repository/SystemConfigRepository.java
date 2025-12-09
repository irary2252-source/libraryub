package com.example.demo.repository;
import com.example.demo.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    // 辅助方法：安全获取配置，如果没找到则返回默认值
    default String getValue(String key, String defaultValue) {
        return findById(key).map(SystemConfig::getConfigValue).orElse(defaultValue);
    }
}