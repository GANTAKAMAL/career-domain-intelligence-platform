package com.careerdomain.controller;

import com.careerdomain.entity.Domain;
import com.careerdomain.entity.JobRole;
import com.careerdomain.repository.DomainRepository;
import com.careerdomain.repository.JobRoleRepository;
import com.careerdomain.service.CareerExplorerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class DomainExplorerController {

    private final DomainRepository domainRepository;
    private final JobRoleRepository jobRoleRepository;
    private final CareerExplorerService careerExplorerService;

    public DomainExplorerController(
            DomainRepository domainRepository,
            JobRoleRepository jobRoleRepository,
            CareerExplorerService careerExplorerService
    ) {
        this.domainRepository = domainRepository;
        this.jobRoleRepository = jobRoleRepository;
        this.careerExplorerService = careerExplorerService;
    }

    /**
     * Returns every career domain with its current role count.
     */
    @GetMapping("/domains")
    public ResponseEntity<List<Map<String, Object>>> getDomains() {

        List<Domain> domains = domainRepository.findAll();

        return ResponseEntity.ok(
                domains.stream()
                        .map(this::toDomainResponse)
                        .toList()
        );
    }

    /**
     * Returns a single domain.
     */
    @GetMapping("/domains/{id}")
    public ResponseEntity<?> getDomain(
            @PathVariable Long id
    ) {

        return domainRepository.findById(id)
                .<ResponseEntity<?>>map(domain ->
                        ResponseEntity.ok(toDomainResponse(domain))
                )
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    /**
     * Returns all roles belonging to a domain.
     */
    @GetMapping("/domains/{domainId}/roles")
    public ResponseEntity<List<Map<String, Object>>> getRolesByDomain(
            @PathVariable Long domainId
    ) {

        if (!domainRepository.existsById(domainId)) {
            return ResponseEntity.notFound().build();
        }

        List<JobRole> roles =
                jobRoleRepository.findByDomainId(domainId);

        return ResponseEntity.ok(
                roles.stream()
                        .map(careerExplorerService::toJobRoleResponse)
                        .toList()
        );
    }

    /**
     * Returns all job roles.
     *
     * Optional search:
     * /job-roles?search=developer
     */
    @GetMapping("/job-roles")
    public ResponseEntity<List<Map<String, Object>>> getJobRoles(
            @RequestParam(required = false) String search
    ) {

        List<JobRole> roles;

        if (search == null || search.isBlank()) {
            roles = jobRoleRepository.findAll();
        } else {
            roles = jobRoleRepository
                    .findByNameContainingIgnoreCase(
                            search.trim()
                    );
        }

        return ResponseEntity.ok(
                roles.stream()
                        .map(careerExplorerService::toJobRoleResponse)
                        .toList()
        );
    }

    /**
     * Returns one job role.
     */
    @GetMapping("/job-roles/{id}")
    public ResponseEntity<?> getJobRole(
            @PathVariable Long id
    ) {

        return jobRoleRepository.findById(id)
                .<ResponseEntity<?>>map(role ->
                        ResponseEntity.ok(
                                careerExplorerService
                                        .toDetailedJobRoleResponse(role)
                        )
                )
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    /**
     * Returns the ESCO/knowledge-graph skills required by a role.
     */
    @GetMapping("/job-roles/{id}/skills")
    public ResponseEntity<?> getJobRoleSkills(
            @PathVariable Long id
    ) {

        return jobRoleRepository.findById(id)
                .<ResponseEntity<?>>map(role ->
                        ResponseEntity.ok(
                                careerExplorerService
                                        .getRoleSkills(role)
                        )
                )
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    private Map<String, Object> toDomainResponse(
            Domain domain
    ) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "id",
                domain.getId()
        );

        response.put(
                "name",
                domain.getName()
        );

        response.put(
                "description",
                domain.getDescription()
        );

        response.put(
                "roleCount",
                jobRoleRepository
                        .findByDomainId(domain.getId())
                        .size()
        );

        return response;
    }
}