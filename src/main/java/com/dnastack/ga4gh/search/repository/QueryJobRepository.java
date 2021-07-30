package com.dnastack.ga4gh.search.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QueryJobRepository extends CrudRepository<QueryJob, String> {
    QueryJob save(QueryJob queryJob);
}
