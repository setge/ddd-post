package com.setge.dddpost.global.jpa;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.Querydsl;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public abstract class Querydsl4RepositorySupport {

  private final Class domainClass;
  private Querydsl querydsl;
  private EntityManager entityManager;
  private JPAQueryFactory queryFactory;


  protected Querydsl4RepositorySupport(Class<?> domainClass) {
    Assert.notNull(domainClass, "Domain class must not be null");
    this.domainClass = domainClass;
  }

  @Autowired
  public void setEntityManager(EntityManager entityManager) {
    Assert.notNull(entityManager, "EntityManager must not be null");
    JpaEntityInformation entityInformation = JpaEntityInformationSupport
        .getEntityInformation(domainClass, entityManager);
    SimpleEntityPathResolver resolver = SimpleEntityPathResolver.INSTANCE;
    EntityPath path = resolver.createPath(entityInformation.getJavaType());
    this.entityManager = entityManager;
    this.querydsl = new Querydsl(entityManager,
        new PathBuilder<Object>(path.getType(), path.getMetadata()));
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @PostConstruct
  public void validate() {
    Assert.notNull(entityManager, "EntityManager must not be null");
    Assert.notNull(querydsl, "Querydsl must not be null");
    Assert.notNull(queryFactory, "QueryFactory must not be null");
  }

  protected JPAQueryFactory getQueryFactory() {
    return queryFactory;
  }

  protected Querydsl getQuerydsl() {
    return querydsl;
  }

  protected EntityManager getEntityManager() {
    return entityManager;
  }

  protected JPAQuery select(Expression expression) { // 식을 가져온다.
    return getQueryFactory().select(expression);
  }

  protected JPAQuery selectForm(EntityPath from) { // 경로에서 QueryFactory를 가져온다.
    return getQueryFactory().select(from);
  }

  protected <T> Page<T> applyPagination(
      Pageable pageable,
      Function<JPAQueryFactory, JPAQuery> contentQuery
  ) {
    JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
    List<T> content = getQuerydsl().applyPagination(pageable, jpaQuery).fetch();
    return PageableExecutionUtils.getPage(content, pageable, jpaQuery::fetchCount);
  }

  protected <T> Page<T> applyPagination(
      Pageable pageable,
      Function<JPAQueryFactory, JPAQuery> contentQuery,
      Function<JPAQueryFactory, JPAQuery> countQuery
  ) {
    JPAQuery jpaContentQuery = contentQuery.apply(getQueryFactory());
    List<T> content = getQuerydsl().applyPagination(pageable, jpaContentQuery).fetch();
    JPAQuery countResult = countQuery.apply(getQueryFactory());
    return PageableExecutionUtils.getPage(content, pageable, countResult::fetchCount);
  }

  protected <T> Page<T> applyPagination(
      Pageable pageable,
      Function<JPAQueryFactory, JPAQuery> contentQuery,
      List<String> excludeOrderProperties
  ) {
    JPAQuery jpaQuery = contentQuery.apply(getQueryFactory());
    Sort newSort = Sort.by(pageable.getSort()
        .get()
        .filter(order -> !excludeOrderProperties.contains(order.getProperty()))
        .collect(Collectors.toList()));

    pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), newSort);
    List<T> content = getQuerydsl().applyPagination(pageable, jpaQuery).fetch();
    return PageableExecutionUtils.getPage(content, pageable, jpaQuery::fetchCount);
  }

}
