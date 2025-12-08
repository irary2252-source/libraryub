package com.example.demo.repository;
import com.example.demo.entity.Reader;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReaderRepository extends JpaRepository<Reader, String> {}