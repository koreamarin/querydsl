package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;

import java.util.List;

// Spring Data JPA를 사용하면 인터페이스만 정의해도 구현체가 자동으로 생성된다.
// MemberRepositoryCustom 인터페이스를 상속받아 MemberRepository 인터페이스를 만들면
// MemberRepositoryCustom 인터페이스의 구현체인 MemberRepositoryImpl도 같이 만들어진다.
public interface MemberRepository
        extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    List<Member> findByUsername(String username);



}
