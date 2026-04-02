package ro.daya.dayalog.service;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ro.daya.dayalog.dto.instructor.InstructorWorkingHoursEntryRequest;
import ro.daya.dayalog.dto.instructor.InstructorWorkingHoursEntryResponse;
import ro.daya.dayalog.dto.instructor.InstructorWorkingHoursRequest;
import ro.daya.dayalog.dto.instructor.InstructorWorkingHoursResponse;
import ro.daya.dayalog.entity.Instructor;
import ro.daya.dayalog.entity.InstructorWorkingHours;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.exception.ConflictException;
import ro.daya.dayalog.exception.NotFoundException;
import ro.daya.dayalog.repository.InstructorRepository;
import ro.daya.dayalog.repository.InstructorWorkingHoursRepository;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import ro.daya.dayalog.exception.BadRequestException;

@Service
public class InstructorWorkingHoursService {

    private final InstructorRepository instructorRepository;
    private final InstructorWorkingHoursRepository instructorWorkingHoursRepository;
    private final AuditLogService auditLogService;

    public InstructorWorkingHoursService(InstructorRepository instructorRepository,
                                         InstructorWorkingHoursRepository instructorWorkingHoursRepository,
                                         AuditLogService auditLogService) {
        this.instructorRepository = instructorRepository;
        this.instructorWorkingHoursRepository = instructorWorkingHoursRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public InstructorWorkingHoursResponse getWorkingHours(CurrentUserPrincipal principal, UUID instructorId) {
        Instructor instructor = instructorRepository.findByIdAndStudioId(instructorId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("INSTRUCTOR_NOT_FOUND", "Instructorul nu a fost găsit."));

        List<InstructorWorkingHours> entries = loadSortedEntries(principal.getStudioId(), instructorId);

        return toResponse(instructor, entries);
    }

    @Transactional
    public InstructorWorkingHoursResponse replaceWorkingHours(CurrentUserPrincipal principal,
                                                              UUID instructorId,
                                                              InstructorWorkingHoursRequest request) {
        Instructor instructor = instructorRepository.findByIdAndStudioId(instructorId, principal.getStudioId())
                .orElseThrow(() -> new NotFoundException("INSTRUCTOR_NOT_FOUND", "Instructorul nu a fost găsit."));

        List<NormalizedWorkingHoursSlot> normalizedSlots = normalizeAndValidate(request.entries());

        List<InstructorWorkingHours> beforeEntries = loadSortedEntries(principal.getStudioId(), instructorId);

        instructorWorkingHoursRepository.deleteByInstructorIdAndStudioId(instructorId, principal.getStudioId());

        List<InstructorWorkingHours> toSave = new ArrayList<>();

        for (NormalizedWorkingHoursSlot slot : normalizedSlots) {
            InstructorWorkingHours entry = new InstructorWorkingHours();
            entry.setStudio(studioRef(principal.getStudioId()));
            entry.setInstructor(instructor);
            entry.setDayOfWeek(slot.dayOfWeek());
            entry.setStartTime(slot.startTime());
            entry.setEndTime(slot.endTime());
            entry.setActive(true);
            toSave.add(entry);
        }

        instructorWorkingHoursRepository.saveAll(toSave);

        List<InstructorWorkingHours> afterEntries = loadSortedEntries(principal.getStudioId(), instructorId);

        auditLogService.log(
                principal.getStudioId(),
                principal.getId(),
                "instructor",
                instructor.getId(),
                "UPDATE_INSTRUCTOR_WORKING_HOURS",
                beforeAfterSummary(toAuditEntries(beforeEntries), toAuditEntries(afterEntries))
        );

        return toResponse(instructor, afterEntries);
    }

    private List<InstructorWorkingHours> loadSortedEntries(UUID studioId, UUID instructorId) {
        return instructorWorkingHoursRepository.findByInstructorIdAndStudioIdAndActiveTrue(instructorId, studioId)
                .stream()
                .sorted(Comparator
                        .comparing((InstructorWorkingHours item) -> item.getDayOfWeek().getValue())
                        .thenComparing(InstructorWorkingHours::getStartTime))
                .toList();
    }

    private List<NormalizedWorkingHoursSlot> normalizeAndValidate(List<InstructorWorkingHoursEntryRequest> entries) {
        List<NormalizedWorkingHoursSlot> normalized = entries.stream()
                .map(this::normalizeEntry)
                .sorted(Comparator
                        .comparing((NormalizedWorkingHoursSlot item) -> item.dayOfWeek().getValue())
                        .thenComparing(NormalizedWorkingHoursSlot::startTime))
                .toList();

        for (int i = 0; i < normalized.size(); i++) {
            NormalizedWorkingHoursSlot current = normalized.get(i);

            if (!current.startTime().isBefore(current.endTime())) {
                throw new ConflictException(
                        "INSTRUCTOR_WORKING_HOURS_INVALID_RANGE",
                        "Ora de sfârșit trebuie să fie după ora de început."
                );
            }

            if (i == 0) {
                continue;
            }

            NormalizedWorkingHoursSlot previous = normalized.get(i - 1);

            if (previous.dayOfWeek() == current.dayOfWeek()
                    && previous.endTime().isAfter(current.startTime())) {
                throw new ConflictException(
                        "INSTRUCTOR_WORKING_HOURS_OVERLAP",
                        "Intervalele de lucru pentru aceeași zi nu se pot suprapune."
                );
            }
        }

        return normalized;
    }

    private NormalizedWorkingHoursSlot normalizeEntry(InstructorWorkingHoursEntryRequest request) {
        DayOfWeek dayOfWeek;

        try {
            dayOfWeek = DayOfWeek.valueOf(request.dayOfWeek().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
        	throw new BadRequestException(
        	        "INSTRUCTOR_WORKING_HOURS_DAY_INVALID",
        	        "Zi invalidă pentru programul instructorului."
        	);        }

        return new NormalizedWorkingHoursSlot(
                dayOfWeek,
                request.startTime(),
                request.endTime()
        );
    }

    private InstructorWorkingHoursResponse toResponse(Instructor instructor, List<InstructorWorkingHours> entries) {
        List<InstructorWorkingHoursEntryResponse> responseEntries = entries.stream()
                .map(entry -> new InstructorWorkingHoursEntryResponse(
                        entry.getDayOfWeek().name(),
                        entry.getStartTime(),
                        entry.getEndTime()
                ))
                .toList();

        return new InstructorWorkingHoursResponse(
                instructor.getId(),
                instructor.getFullName(),
                responseEntries
        );
    }

    private List<Map<String, Object>> toAuditEntries(List<InstructorWorkingHours> entries) {
        return entries.stream()
                .<Map<String, Object>>map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("dayOfWeek", entry.getDayOfWeek().name());
                    item.put("startTime", entry.getStartTime().toString());
                    item.put("endTime", entry.getEndTime().toString());
                    return item;
                })
                .toList();
    }

    private Map<String, Object> beforeAfterSummary(Object before, Object after) {
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

    private record NormalizedWorkingHoursSlot(
            DayOfWeek dayOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    ) {
    }
}