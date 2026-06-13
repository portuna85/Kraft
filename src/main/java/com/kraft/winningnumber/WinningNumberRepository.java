package com.kraft.winningnumber;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WinningNumberRepository extends JpaRepository<WinningNumber, Long> {

    Optional<WinningNumber> findTopByOrderByRoundDesc();

    Optional<WinningNumber> findByRound(Integer round);

    Page<WinningNumber> findAllByOrderByRoundDesc(Pageable pageable);
}
