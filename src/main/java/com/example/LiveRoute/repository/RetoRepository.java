package com.example.LiveRoute.repository;

import com.example.LiveRoute.model.Reto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetoRepository extends JpaRepository<Reto, Long> {
    List<Reto> findByGincanaIdOrderByOrdenAsc(Long gincanaId);

    Optional<Reto> findByIdAndGincanaId(Long id, Long gincanaId);
}
