package com.careerdomain.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.careerdomain.entity.JobNode;

public interface JobNodeRepository extends Neo4jRepository<JobNode, Long> {
}