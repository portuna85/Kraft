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

    // List(Page 아님) 반환 — Spring Data JPA는 Pageable을 받아도 반환형이
    // List/Slice면 count(*) 쿼리를 생략한다. 통계용 top-N 조회처럼 총
    // 개수가 필요 없는 호출에 사용(WinningNumberQueryService의 페이지네이션
    // 목록은 totalElements가 필요하므로 위 Page 메서드를 그대로 사용한다).
    List<WinningNumber> findByOrderByRoundDesc(Pageable pageable);

    @Query("select w.round from WinningNumber w order by w.round asc")
    List<Integer> findAllRoundsOrderByRoundAsc();
}
