package ro.daya.dayalog.support;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.persistence.EntityManager;
import ro.daya.dayalog.entity.AppUser;
import ro.daya.dayalog.entity.Appointment;
import ro.daya.dayalog.entity.AppointmentParticipant;
import ro.daya.dayalog.entity.Client;
import ro.daya.dayalog.entity.Instructor;
import ro.daya.dayalog.entity.ServiceEntity;
import ro.daya.dayalog.entity.Studio;
import ro.daya.dayalog.entity.enums.AppointmentStatus;
import ro.daya.dayalog.entity.enums.AppointmentType;
import ro.daya.dayalog.entity.enums.ParticipantStatus;
import ro.daya.dayalog.entity.enums.UserRole;
import ro.daya.dayalog.security.CurrentUserPrincipal;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
public abstract class AbstractPostgresIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", POSTGRES::getJdbcUrl);
        registry.add("DB_USERNAME", POSTGRES::getUsername);
        registry.add("DB_PASSWORD", POSTGRES::getPassword);

        registry.add("JWT_ACCESS_SECRET", () -> "test-access-secret-key-1234567890-test");
        registry.add("JWT_REFRESH_SECRET", () -> "test-refresh-secret-key-1234567890-test");
        registry.add("APP_BASE_URL", () -> "http://localhost:8080");
        registry.add("MAIL_FROM", () -> "noreply@test.local");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected EntityManager entityManager;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected String asJson(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    protected RequestPostProcessor adminAuth(AppointmentFixture fixture) {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                fixture.adminUserId(),
                fixture.studioId(),
                fixture.adminEmail(),
                fixture.adminPasswordHash(),
                UserRole.ADMIN,
                false,
                true
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }
    
    protected RequestPostProcessor clientAuth(ClientSelfFixture fixture) {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                fixture.clientUserId(),
                fixture.studioId(),
                fixture.clientEmail(),
                fixture.clientPasswordHash(),
                UserRole.CLIENT,
                false,
                true
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    protected AppointmentFixture createAppointmentFixture() {
        String suffix = UUID.randomUUID().toString().replace("-", "");

        Studio studio = new Studio();
        studio.setName("Test Studio " + suffix);
        studio.setLegalName("Test Studio " + suffix + " SRL");
        studio.setEmail("studio+" + suffix + "@tests.local");
        studio.setPhone("+40" + suffix.substring(0, 8));
        studio.setAddressLine1("Test Street 1");
        studio.setCity("Constanta");
        studio.setCounty("Constanta");
        studio.setPostcode("900001");
        studio.setActive(true);
        entityManager.persist(studio);

        AppUser admin = new AppUser();
        admin.setStudio(studio);
        admin.setEmail("admin+" + suffix + "@tests.local");
        admin.setPasswordHash(passwordEncoder.encode("Admin123!Change"));
        admin.setRole(UserRole.ADMIN);
        admin.setEmailVerified(true);
        admin.setForcePasswordChange(false);
        admin.setActive(true);
        entityManager.persist(admin);

        ServiceEntity service = new ServiceEntity();
        service.setStudio(studio);
        service.setName("Pilates Reformer " + suffix);
        service.setDescription("Test service");
        service.setDefaultDurationMinutes(60);
        service.setActive(true);
        entityManager.persist(service);

        Instructor primaryInstructor = new Instructor();
        primaryInstructor.setStudio(studio);
        primaryInstructor.setFirstName("Ana");
        primaryInstructor.setLastName("Ionescu");
        primaryInstructor.setEmail("ana+" + suffix + "@tests.local");
        primaryInstructor.setPhone("+40" + suffix.substring(0, 7) + "1");
        primaryInstructor.setActive(true);
        entityManager.persist(primaryInstructor);

        Instructor secondaryInstructor = new Instructor();
        secondaryInstructor.setStudio(studio);
        secondaryInstructor.setFirstName("Maria");
        secondaryInstructor.setLastName("Popa");
        secondaryInstructor.setEmail("maria+" + suffix + "@tests.local");
        secondaryInstructor.setPhone("+40" + suffix.substring(0, 7) + "2");
        secondaryInstructor.setActive(true);
        entityManager.persist(secondaryInstructor);

        Client clientOne = new Client();
        clientOne.setStudio(studio);
        clientOne.setFirstName("Elena");
        clientOne.setLastName("Marin");
        clientOne.setEmail("elena+" + suffix + "@tests.local");
        clientOne.setPhone("+40" + suffix.substring(0, 7) + "3");
        clientOne.setGdprConsent(true);
        clientOne.setEmailAllowed(true);
        clientOne.setSmsAllowed(false);
        clientOne.setMarketingAllowed(false);
        clientOne.setActive(true);
        entityManager.persist(clientOne);

        Client clientTwo = new Client();
        clientTwo.setStudio(studio);
        clientTwo.setFirstName("Ioana");
        clientTwo.setLastName("Georgescu");
        clientTwo.setEmail("ioana+" + suffix + "@tests.local");
        clientTwo.setPhone("+40" + suffix.substring(0, 7) + "4");
        clientTwo.setGdprConsent(true);
        clientTwo.setEmailAllowed(true);
        clientTwo.setSmsAllowed(false);
        clientTwo.setMarketingAllowed(false);
        clientTwo.setActive(true);
        entityManager.persist(clientTwo);

        Client clientThree = new Client();
        clientThree.setStudio(studio);
        clientThree.setFirstName("Andreea");
        clientThree.setLastName("Pop");
        clientThree.setEmail("andreea+" + suffix + "@tests.local");
        clientThree.setPhone("+40" + suffix.substring(0, 7) + "5");
        clientThree.setGdprConsent(true);
        clientThree.setEmailAllowed(true);
        clientThree.setSmsAllowed(false);
        clientThree.setMarketingAllowed(false);
        clientThree.setActive(true);
        entityManager.persist(clientThree);

        entityManager.flush();

        return new AppointmentFixture(
                studio.getId(),
                admin.getId(),
                admin.getEmail(),
                admin.getPasswordHash(),
                service.getId(),
                primaryInstructor.getId(),
                secondaryInstructor.getId(),
                clientOne.getId(),
                clientTwo.getId(),
                clientThree.getId()
        );
    }
    
    protected ClientSelfFixture createClientSelfFixture() {
        String suffix = UUID.randomUUID().toString().replace("-", "");

        Studio studio = new Studio();
        studio.setName("Client Test Studio " + suffix);
        studio.setLegalName("Client Test Studio " + suffix + " SRL");
        studio.setEmail("studio-client+" + suffix + "@tests.local");
        studio.setPhone("+40" + suffix.substring(0, 8));
        studio.setAddressLine1("Client Test Street 1");
        studio.setCity("Constanta");
        studio.setCounty("Constanta");
        studio.setPostcode("900001");
        studio.setActive(true);
        entityManager.persist(studio);

        AppUser clientUser = new AppUser();
        clientUser.setStudio(studio);
        clientUser.setEmail("client+" + suffix + "@tests.local");
        clientUser.setPasswordHash(passwordEncoder.encode("Client123!Change"));
        clientUser.setRole(UserRole.CLIENT);
        clientUser.setEmailVerified(true);
        clientUser.setForcePasswordChange(false);
        clientUser.setActive(true);
        entityManager.persist(clientUser);

        Client client = new Client();
        client.setStudio(studio);
        client.setUser(clientUser);
        client.setFirstName("Client");
        client.setLastName("User");
        client.setEmail(clientUser.getEmail());
        client.setPhone("+40" + suffix.substring(0, 7) + "7");
        client.setGdprConsent(true);
        client.setEmailAllowed(true);
        client.setSmsAllowed(false);
        client.setMarketingAllowed(false);
        client.setActive(true);
        entityManager.persist(client);

        entityManager.flush();

        return new ClientSelfFixture(
                studio.getId(),
                clientUser.getId(),
                clientUser.getEmail(),
                clientUser.getPasswordHash(),
                client.getId()
        );
    }

    protected UUID createAppointment(AppointmentFixture fixture,
                                     UUID instructorId,
                                     OffsetDateTime startAt,
                                     OffsetDateTime endAt,
                                     AppointmentType appointmentType,
                                     AppointmentStatus status,
                                     int capacity,
                                     List<UUID> participantClientIds) {

        Appointment appointment = new Appointment();
        appointment.setStudio(entityManager.getReference(Studio.class, fixture.studioId()));
        appointment.setAppointmentType(appointmentType);
        appointment.setService(entityManager.getReference(ServiceEntity.class, fixture.serviceId()));
        appointment.setInstructor(entityManager.getReference(Instructor.class, instructorId));
        appointment.setStartAt(startAt);
        appointment.setEndAt(endAt);
        appointment.setStatus(status);
        appointment.setCapacity(capacity);
        appointment.setNotes("Seeded appointment");
        appointment.setCreatedBy(entityManager.getReference(AppUser.class, fixture.adminUserId()));
        appointment.setUpdatedBy(entityManager.getReference(AppUser.class, fixture.adminUserId()));

        for (UUID clientId : participantClientIds) {
            AppointmentParticipant participant = new AppointmentParticipant();
            participant.setAppointment(appointment);
            participant.setClient(entityManager.getReference(Client.class, clientId));
            participant.setParticipationStatus(ParticipantStatus.BOOKED);
            appointment.getParticipants().add(participant);
        }

        entityManager.persist(appointment);
        entityManager.flush();

        return appointment.getId();
    }

    protected record AppointmentFixture(
            UUID studioId,
            UUID adminUserId,
            String adminEmail,
            String adminPasswordHash,
            UUID serviceId,
            UUID primaryInstructorId,
            UUID secondaryInstructorId,
            UUID clientOneId,
            UUID clientTwoId,
            UUID clientThreeId
    ) {
    }
    
    protected record ClientSelfFixture(
            UUID studioId,
            UUID clientUserId,
            String clientEmail,
            String clientPasswordHash,
            UUID clientId
    ) {
    }
    
}