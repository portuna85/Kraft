package com.kraft.winningnumber;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WinningNumberRepository extends JpaRepository<WinningNumber, Long> {

    Optional<WinningNumber> findTopByOrderByRoundDesc();

    Optional<WinningNumber> findByRound(Integer round);

    Page<WinningNumber> findAllByOrderByRoundDesc(Pageable pageable);

    @Query("select w.round from WinningNumber w order by w.round asc")
    List<Integer> findAllRoundsOrderByRoundAsc();
}
