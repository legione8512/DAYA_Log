package ro.daya.dayalog.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ro.daya.dayalog.entity.AppointmentParticipant;

public interface AppointmentParticipantRepository extends JpaRepository<AppointmentParticipant, UUID> {

    Optional<AppointmentParticipant> findByAppointmentIdAndClientId(UUID appointmentId, UUID clientId);
}