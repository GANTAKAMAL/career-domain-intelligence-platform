package com.careerdomain.controller;

import com.careerdomain.service.DomainClassificationService;
import com.careerdomain.service.EscoDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping
public class EscoDataController {

    private final DomainClassificationService domainClassificationService;
    private final EscoDataService escoDataService;
    private final String administratorEmail;

    public EscoDataController(
            DomainClassificationService domainClassificationService,
            EscoDataService escoDataService,
            @Value("${app.admin.email}") String administratorEmail
    ) {
        this.domainClassificationService = domainClassificationService;
        this.escoDataService = escoDataService;
        this.administratorEmail = administratorEmail.trim().toLowerCase();
    }

    /**
     * Reclassify all imported ESCO occupations into the platform's
     * career domains using deterministic, explainable rules.
     */
    @PostMapping("/admin/knowledge-base/reclassify-domains")
    public ResponseEntity<Map<String, Object>> reclassifyDomains(
            @AuthenticationPrincipal UserDetails principal) {
        requireAdministrator(principal);
        return ResponseEntity.ok(
                domainClassificationService.reclassifyAllRoles()
        );
    }

    /**
     * Performs a deliberate, authenticated ESCO refresh. It is never invoked
     * on application startup, so existing graph data remains available when
     * the external ESCO service is unavailable.
     */
    @PostMapping("/admin/knowledge-base/refresh-esco")
    public ResponseEntity<Map<String, Object>> refreshEsco(
            @AuthenticationPrincipal UserDetails principal) {
        requireAdministrator(principal);
        return ResponseEntity.ok(escoDataService.refreshKnowledgeBase());
    }

    private void requireAdministrator(UserDetails principal) {
        if (principal == null
                || !administratorEmail.equalsIgnoreCase(principal.getUsername())) {
            throw new AccessDeniedException("Administrator access is required.");
        }
    }
}
