package ro.daya.dayalog.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.common.StatusToggleRequest;
import ro.daya.dayalog.dto.instructor.InstructorRequest;
import ro.daya.dayalog.dto.instructor.InstructorResponse;
import ro.daya.dayalog.entity.Instructor;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.repository.InstructorRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.exception.ConflictException;
import ro.daya.dayalog.exception.NotFoundException;

@Service
public class InstructorManagementService {

    private final InstructorRepository instructorRepository;
    private final AuditLogService auditLogService;

    public InstructorManagementService(InstructorRepository instructorRepository,
                                       AuditLogService auditLogService) {
        this.instructorRepository = instructorRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<InstructorResponse> list(CurrentUserPrincipal principal, String query, Boolean active) {
        String normalizedQuery = normalizeQuery(query);

        List<Instructor> instructors;

        if (normalizedQuery == null) {
            instructors = (active == null)
                    ? instructorRepository.findByStudioIdOrderByLastNameAscFirstNameAsc(principal.getStudioId())
                    : instructorRepository.findByStudioIdAndActiveOrderByLastNameAscFirstNameAsc(principal.getStudioId(), active);
        } else {
            instructors = instructorRepository.searchByStudioIdAndFilters(
                    principal.getStudioId(),
                    normalizedQuery,
                    active
            );
        }

        return instructors.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InstructorResponse create(CurrentUserPrincipal principal, InstructorRequest request) {
        String email = blankToNull(request.email());

        if (email != null && instructorRepository.existsByStudioIdAndEmailIgnoreCase(principal.getStudioId(), email)) {
            throw new ConflictException("INSTRUCTOR_EMAIL_CONFLICT", "Există deja un instructor cu acest email.");
        }

        Instructor instructor = new Instructor();
        instructor.setStudio(studioRef(principal.getStudioId()));
        instructor.setFirstName(request.firstName().trim());
        instructor.setLastName(request.lastName().trim());
        instructor.setEmail(email);
        instructor.setPhone(blankToNull(request.phone()));
        instructor.setActive(true);

        Instructor saved = instructorRepository.save(instructor);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "instructor",
                saved.getId(),
                "CREATE_INSTRUCTOR",
                instructorAuditSummary(saved)
        );

        return toResponse(saved);
    }

    @Transactional
    public InstructorResponse update(CurrentUserPrincipal principal, UUID id, InstructorRequest request) {
        Instructor instructor = instructorRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("INSTRUCTOR_NOT_FOUND", "Instructorul nu a fost găsit."));

        String email = blankToNull(request.email());

        if (email != null && instructorRepository.existsByStudioIdAndEmailIgnoreCaseAndIdNot(principal.getStudioId(), email, id)) {
            throw new ConflictException("INSTRUCTOR_EMAIL_CONFLICT", "Există deja un instructor cu acest email.");
        }

        Map<String, Object> before = instructorAuditSummary(instructor);

        instructor.setFirstName(request.firstName().trim());
        instructor.setLastName(request.lastName().trim());
        instructor.setEmail(email);
        instructor.setPhone(blankToNull(request.phone()));

        Map<String, Object> after = instructorAuditSummary(instructor);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "instructor",
                instructor.getId(),
                "UPDATE_INSTRUCTOR",
                beforeAfterSummary(before, after)
        );

        return toResponse(instructor);
    }

    @Transactional
    public void updateStatus(CurrentUserPrincipal principal, UUID id, StatusToggleRequest request) {
        Instructor instructor = instructorRepository.findByIdAndStudioId(id, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("INSTRUCTOR_NOT_FOUND", "Instructorul nu a fost găsit."));

        Boolean previousActive = instructor.getActive();
        instructor.setActive(request.active());

        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("fullName", instructor.getFullName());
        summary.put("beforeActive", previousActive);
        summary.put("afterActive", instructor.getActive());

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "instructor",
                instructor.getId(),
                "UPDATE_INSTRUCTOR_STATUS",
                summary
        );
    }

    private InstructorResponse toResponse(Instructor instructor) {
        return new InstructorResponse(
                instructor.getId(),
                instructor.getFirstName(),
                instructor.getLastName(),
                instructor.getFullName(),
                instructor.getEmail(),
                instructor.getPhone(),
                instructor.getActive()
        );
    }

    private Map<String, Object> instructorAuditSummary(Instructor instructor) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("firstName", instructor.getFirstName());
        summary.put("lastName", instructor.getLastName());
        summary.put("fullName", instructor.getFullName());
        summary.put("email", instructor.getEmail());
        summary.put("phone", instructor.getPhone());
        summary.put("active", instructor.getActive());
        return summary;
    }

    private Map<String, Object> beforeAfterSummary(Map<String, Object> before,
                                                   Map<String, Object> after) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("before", before);
        summary.put("after", after);
        return summary;
    }

    private Studio studioRef(UUID studioId) {
        Studio studio = new Studio();
        studio.setId(studioId);
        return studio;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeQuery(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}