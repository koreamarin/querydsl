package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void setUp() {
        // given
        queryFactory = new JPAQueryFactory(em);

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
    }

    @DisplayName("JPQL로 객체 조회")
    @Test
    public void startJPQL() {
        // member1을 찾아라
        String qlString =
                "select m from Member m " +
                "where m.username = :username";

        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("Querydsl로 객체 조회")
    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))    // 파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("")
    @Test
    void search() {
        Member findMember = queryFactory
                .selectFrom(member) // select와 from의 elias가 같을 경우 이렇게 압축 가능
                .where(
                        member.username.eq("member1")
                        .and(member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("")
    @Test
    void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member) // select와 from의 elias가 같을 경우 이렇게 압축 가능
                .where(
                        member.username.eq("member1"),      // 콤마로 and 조건 처리 가능
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @DisplayName("결과 조회 - 리스트 조회, 단 건 조회, 페이징 조회 등")
    @Test
    public void resultFetchTest() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();     // 리스트 조회
//
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();  // 단 건 조회, 결과가 없으면 null 반환, 결과가 둘 이상이면 NonUniqueResultException 발생
//
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchFirst();    // limit(1).fetchOne() 과 동일

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();    // 페이징 정보 포함. count 쿼리까지 같이 날림

        results.getTotal(); // 전체 조회 수
        List<Member> content = results.getResults();


        long total = queryFactory
                .selectFrom(member)
                .fetchCount();// count 쿼리만 날림
    }

    @DisplayName("정렬 테스트- 1.회원 나이 내림차순(desc), 2.회원 이름 올림차순(asc), 2에서 회원 이름이 없으면 마지막에 null 출력")
    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @DisplayName("offset과 limit를 이용하여 페이징 기능 이용하기1")
    @Test
    void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 첫번째 행을 제외하고 2번 째 행부터 조회
                .limit(2)  // 최대 2건 조회
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @DisplayName("offset과 limit를 이용하여 페이징 기능 이용하기2 - fetchResults는 실무에서 잘 안쓰일 수도 있음")
    @Test
    void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) // 첫번째 행을 제외하고 2번 째 행부터 조회
                .limit(2)  // 최대 2건 조회
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);   // 전체 조회 수
        assertThat(queryResults.getLimit()).isEqualTo(2);   // limit
        assertThat(queryResults.getOffset()).isEqualTo(1);  // offset
        assertThat(queryResults.getResults().size()).isEqualTo(2);  // 조회된 데이터 수
    }

    @DisplayName("집합 함수 - count, sum, avg, max, min")
    @Test
    void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @DisplayName("그룹핑 하기 - 팀의 이름과 각 팀의 평균 연령 구하기")
    @Test
    void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);  // (10 + 20) / 2 = 15

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);  // (30 + 40) / 2 = 35
    }

    @DisplayName("Join - 팀 A에 소속된 모든 회원 찾기")
    @Test
    void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    @DisplayName("세타 조인(연관관계가 없는 필드로 조인) - 회원의 이름이 팀 이름과 같은 회원 조회")
    @Test
    void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    @DisplayName("Join, On - 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회" +
            " / JPQL : select m, t from Member m left join m.team t on t.name = 'teamA' ")
    @Test
    void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @DisplayName("연관관계 없는 엔티티 외부 조인, 세타 조인 - 회원의 이름이 팀 이름과 같은 대상 외부 조인")
    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @DisplayName("패치 조인 미적용")
    @Test
    void fetchJoinNo() {
        em.flush(); // 영속성 컨텍스트에 있는 쿼리를 DB에 반영
        em.clear(); // 영속성 컨텍스트 초기화

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @DisplayName("패치 조인 적용")
    @Test
    void fetchJoinUse() {
        em.flush(); // 영속성 컨텍스트에 있는 쿼리를 DB에 반영
        em.clear(); // 영속성 컨텍스트 초기화

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }

    @DisplayName("서브쿼리 - 나이가 가장 많은 회원을 조회")
    @Test
    void subQuery() {
        QMember memberSub = new QMember("memberSub");   // alias가 중복되지 않도록 별도로 생성

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        // JPAExpressions를 사용하면 JPQL의 subquery를 사용할 수 있음
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    @DisplayName("서브쿼리 - 나이가 평균 이상인 회원을 조회")
    @Test
    void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");   // alias가 중복되지 않도록 별도로 생성

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        // JPAExpressions를 사용하면 JPQL의 subquery를 사용할 수 있음
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    @DisplayName("서브쿼리 - in절 처리 - 나이가 10살 초과인 회원을 조회")
    @Test
    void subQueryIn() {
        QMember memberSub = new QMember("memberSub");   // alias가 중복되지 않도록 별도로 생성

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        // JPAExpressions를 사용하면 JPQL의 subquery를 사용할 수 있음
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @DisplayName("select 절 안의 Subquery - 나이가 평균 이상인 회원을 조회")
    @Test
    void selectSubquery() {
        
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @DisplayName("Case문 - 나이가 10살이면 '열살', 20살이면 '스무살', 나머지는 '기타'로 표시")
    @Test
    void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for(String s : result) {
            System.out.println("s = " + s);
        }
    }

    @DisplayName("Case문 - 복잡한 Case문 - 0~20살이면 '0~20살', 21~30살이면 '21~30살', 나머지는 '기타'로 표시")
    @Test
    void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @DisplayName("상수 - select 절에 상수를 추가하기. sql문에서는 'A'를 조회하기 위한 문법이 사용되지는 않지만, 결과에만 표시됨.")
    @Test
    void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @DisplayName("ConCat (문자 더하기) - username_age로 조회하기")
    @Test
    void concat() {
        // {username}_{age}
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @DisplayName("프로젝션 대상이 하나일 때 - username만 조회하기")
    @Test
    void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @DisplayName("프로젝션 대상이 둘 이상일 때 - username, age를 조회하기")
    @Test
    void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @DisplayName("JPQL을 사용하여 DTO로 조회하기")
    @Test
    void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("QueryDSL을 사용하여 DTO로 조회하기 - 프로퍼티 접근(DTO에 기본생성자 필요, 기본생성자로 생성한 후 setter로 값 주입하는 방식)")
    @Test
    void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(
                        Projections.bean(MemberDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("QueryDSL을 사용하여 DTO로 조회하기 - 필드 직접 접근(DTO에 기본생성자 필요, 필드 직접 접근 방식)")
    @Test
    void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(
                        Projections.fields(MemberDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("QueryDSL을 사용하여 DTO로 조회하기 - constructor를 이용한 생성자 접근 방식(DTO에 모든 필드 생성자 필요, 생성자로 생성하는 방식)")
    @Test
    void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(
                        Projections.constructor(MemberDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("QueryDSL을 사용하여 DTO로 조회하기 - 필드 접근 방식으로, entity의 필드와 DTO의 필드명이 다를 때, alias 사용하여 매칭하기")
    @Test
    void findUserDtoByField1() {
        List<UserDto> result = queryFactory
                .select(
                        Projections.fields(UserDto.class,
                                member.username.as("name"),
                                member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @DisplayName("QueryDSL을 사용하여 DTO로 조회하기 - 필드 접근 방식으로, ExpressionUtils를 사용하여 DTO에 매핑하기. 서브쿼리는 무조건 ExpressionUtils를 사용하여 alias를 지정해주어야 함")
    @Test
    void findUserDtoByField2() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(
                        Projections.fields(UserDto.class,
                                ExpressionUtils.as(member.username, "name"),    // 이와 같이 ExpressionUtils를 사용하여 alias를 지정할 수도 있음.읽기가 어려우므로 권장되지는 않음

                                ExpressionUtils.as(JPAExpressions               // 서브쿼리는 무조건 ExpressionUtils를 사용하여 alias를 지정해주어야 함
                                        .select(memberSub.age.max())
                                            .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @DisplayName("QueryDSL을 사용하여 DTO로 조회하기 - constructor를 이용한 생성자 사용(DTO에 모든 필드 생성자 필요, 생성자로 생성하는 방식)")
    @Test
    void findUserDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(
                        Projections.constructor(UserDto.class,
                                member.username,
                                member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @DisplayName("QueryDSL을 사용하여 DTO로 조회하기 - QueryProjection을 사용하여 DTO로 조회하기")
    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @DisplayName("distinct - 중복 제거하기")
    @Test
    void distinct() {
        List<String> result = queryFactory
                .select(member.username).distinct()
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    // 동적쿼리

    @DisplayName("동적쿼리 - BooleanBuilder 사용하기")
    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
//         BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));   // username이 null이 아닌 것이 확실 할 때, 초기값으로 넣어줄 수 있음
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) { // username이 null이 아닌 경우
            builder.and(member.username.eq(usernameCond));  // member.username == usernameCond 조건 추가
        }
        if (ageCond != null) {  // age가 null이 아닌 경우
            builder.and(member.age.eq(ageCond));    // member.age == ageCond 조건 추가
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @DisplayName("동적쿼리 - Where 다중 파라미터 사용하기")
    @Test
    void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))    // usernameEq, ageEq 메서드를 사용하여 조건을 추가, null이 들어가면 그 조건은 무시된다.
//                .where(allEq(usernameCond, ageCond))    // 이런 방식으로 조건을 합쳐서 동적 쿼리로 사용할 수도 있다.
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @DisplayName("벌크 연산 - 나이가 28살 이하인 회원의 나이를 20살로 변경하기")
    @Test
//    @Commit // 이 테스트를 수행할 때에는 commit을 해서 DB와 영속성컨텍스트 비교하기, Transactional에 의해 원래는 초기화 되지만, commit을 붙이면 DB에 반영이 됨.
    void bulkUpdate() {
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        // bulk연산은 영속성 컨텍스트를 무시하고 DB에 바로 쿼리를 날림
        /** member1 = 10 -> DB 비회원      영속성 컨텍스트 member1
         * member2 = 20 -> DB 비회원       영속성 컨텍스트 member2
         * member3 = 30 -> DB member3     영속성 컨텍스트 member3
         * member4 = 40 -> DB member4     영속성 컨텍스트 member4
         */

        // 이때 영속성 컨텍스트와 DB의 데이터가 다르기 때문에 영속성 컨텍스트를 초기화해주어야 함.
        // 영속성 컨텍스트에 있는 데이터가 우선권을 가지기 때문에 DB에서 다시 불러와도 영속성 컨텍스트에 있는 데이터가 우선권을 가져서 반영되지 않음.
        List<Member> result1 = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : result1) {
            System.out.println("member1 = " + member1);
        }
        /**
         *  member1 = Member(id=1, username=member1, age=10)
         *  member2 = Member(id=2, username=member2, age=20)
         *  member3 = Member(id=3, username=member3, age=30)
         *  member4 = Member(id=4, username=member4, age=40)
         */


        // 그러기 때문에 영속성 컨텍스트를 초기화해주어야 함.
        em.flush();
        em.clear();

        // 초기화 이후 DB에서 다시 조회
        List<Member> result2 = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result2) {
            System.out.println("member1 = " + member1);
        }
        /**
         *  member1 = Member(id=1, username=비회원, age=10)
         *  member2 = Member(id=2, username=비회원, age=20)
         *  member3 = Member(id=3, username=member3, age=30)
         *  member4 = Member(id=4, username=member4, age=40)
         */
    }

    @DisplayName("벌크 연산 - 모든 회원 나이에 1을 더하기")
    @Test
    void builkAdd() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @DisplayName("벌크 연산 - 모든 회원 나이에 2을 곱하기")
    @Test
    void builkMultiply() {
        long count = queryFactory
                .update(member)
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @DisplayName("벌크 연산 - 18살 미만인 회원 모두 삭제하기")
    @Test
    public void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.lt(18))
                .execute();
    }

    @DisplayName("sqlFunction - 유저이름에서 member가 들어있다면 M으로 바꿔서 조회하기")
    @Test
    void sqlFunction() {
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate(
                                "function('replace', {0}, {1}, {2})",   // {0} = member.username, {1} = "member", {2} = "M"
                                member.username, "member", "M"))

                .from(member)
                .fetch();


        for (String s : result) {
            System.out.println("s = " + s);
        }
        /**
         * s = M1
         * s = M2
         * s = M3
         * s = M4
         */
    }

    @DisplayName("sqlFunction - lower 함수 사용하기")
    @Test
    void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }










}
