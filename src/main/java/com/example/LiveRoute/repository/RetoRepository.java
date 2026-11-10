package com.example.LiveRoute.repository;

import com.example.LiveRoute.model.Reto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetoRepository extends JpaRepository<Reto, Long> {
    List<Reto> findByGincanaIdOrderByOrdenAsc(Long gincanaId);
}