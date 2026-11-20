package io.github.snipperxx.applyledger.application;

import io.github.snipperxx.applyledger.common.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
class ApplicationController {

    private final ApplicationService service;

    ApplicationController(ApplicationService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<ApplicationResponse> create(@Valid @RequestBody ApplicationRequest request) {
        ApplicationResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/applications/" + created.id())).body(created);
    }

    /**
     * Example: {@code GET /api/applications?status=APPLIED&status=INTERVIEWING&q=backend&sort=appliedOn,desc}.
     * Sortable by any field of the response that maps to a column, e.g. appliedOn, updatedAt, roleTitle.
     */
    @GetMapping
    PageResponse<ApplicationResponse> search(
            @RequestParam(required = false) Set<ApplicationStatus> status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Boolean open,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(new ApplicationSearch(status, companyId, open, q), pageable);
    }

    @GetMapping("/{id}")
    ApplicationResponse get(@PathVariable long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    ApplicationResponse update(@PathVariable long id, @Valid @RequestBody ApplicationRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/status")
    ApplicationResponse changeStatus(@PathVariable long id, @Valid @RequestBody StatusChangeRequest request) {
        return service.changeStatus(id, request);
    }

    @GetMapping("/{id}/history")
    List<StatusChangeResponse> history(@PathVariable long id) {
        return service.history(id);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
