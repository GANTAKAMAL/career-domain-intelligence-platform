package com.careerdomain.repository;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import com.careerdomain.entity.SkillNode;

public interface SkillNodeRepository extends Neo4jRepository<SkillNode, String> {

    /** Explicit Cypher avoids the Spring Data Neo4j derived-query NPE seen on
     * findByName with the current SDN 8.x dependency. */
    @Query("MATCH (s:Skill {name: $name}) RETURN s")
    Optional<SkillNode> findByName(String name);
}
