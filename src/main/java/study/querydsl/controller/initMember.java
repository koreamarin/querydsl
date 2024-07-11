package study.querydsl.controller;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

@Profile("local")   // local일 때만 실행
@Component
@RequiredArgsConstructor
public class initMember {   // 데이터 초기화용 클래스

    private final InitMemberService initMemberService;

    @PostConstruct  // 이 클래스 빈이 등록이 되면 이 메서드가 한번만 실행되도록 하는 어노테이션
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {
        @Autowired
        private EntityManager em;

        @Transactional
        public void init() {
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for (int i=0; i<100; i++) {
                Team selectedTeam = i % 2 == 0 ? teamA : teamB;
                em.persist(new Member("member"+i, i, selectedTeam));
            }
        }
    }
}
