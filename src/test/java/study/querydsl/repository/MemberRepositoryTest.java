package study.querydsl.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberRepositoryTest {

    @Autowired
    EntityManager em;

    @Autowired
    MemberRepository memberRepository;

    @DisplayName("Spring Data JPA 기본 테스트")
    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberRepository.save(member);

        Member findMember = memberRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);
    }

    @DisplayName("Spring Data JPA에서 QueryDSL 테스트")
    @Test
    void searchTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(40);
        condition.setTeamName("teamB");

        List<MemberTeamDto> result = memberRepository.search(condition);
        assertThat(result).extracting("username").containsExactly( "member4");
    }

    @DisplayName("Spring Data JPA에서 QueryDSL을 fetchResult를 사용한 페이징 테스트")
    @Test
    void searchPageSimpleTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();

        // test1
        PageRequest pageRequest1 = PageRequest.of(0, 3);

        Page<MemberTeamDto> result1 = memberRepository.searchPageSimple(condition, pageRequest1);

        assertThat(result1.getSize()).isEqualTo(3);
        assertThat(result1.getContent()).extracting("username").containsExactly("member1", "member2", "member3");

        // test2
        PageRequest pageRequest2 = PageRequest.of(1, 3);

        Page<MemberTeamDto> result2 = memberRepository.searchPageSimple(condition, pageRequest2);

        assertThat(result2.getSize()).isEqualTo(3);
        assertThat(result2.getContent()).extracting("username").containsExactly("member4");
    }


    @DisplayName("Spring Data JPA에서 QueryDSL을 사용하고, fetchResult를 사용하지 않은 페이징 테스트")
    @Test
    void searchPageComplexTest() {
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();

        // test1
        PageRequest pageRequest1 = PageRequest.of(0, 3);

        Page<MemberTeamDto> result1 = memberRepository.searchPageComplex(condition, pageRequest1);

        assertThat(result1.getSize()).isEqualTo(3);
        assertThat(result1.getContent()).extracting("username").containsExactly("member1", "member2", "member3");

        // test2
        PageRequest pageRequest2 = PageRequest.of(1, 3);

        Page<MemberTeamDto> result2 = memberRepository.searchPageComplex(condition, pageRequest2);

        assertThat(result2.getSize()).isEqualTo(3);
        assertThat(result2.getContent()).extracting("username").containsExactly("member4");
    }

}