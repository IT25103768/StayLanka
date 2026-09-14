package com.staylanka.request;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.common.NotFoundException;
import com.staylanka.common.ReferenceGenerator;
import com.staylanka.customer.CustomerProfile;
import com.staylanka.reservation.Reservation;
import com.staylanka.reservation.ReservationService;
import com.staylanka.security.CurrentUserService;
import com.staylanka.user.AppUser;
import com.staylanka.user.AppUserRepository;
import com.staylanka.user.Role;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class GuestRequestService {
    @org.springframework.beans.factory.annotation.Autowired private com.staylanka.common.AuditService audit;
    @org.springframework.beans.factory.annotation.Autowired private com.staylanka.common.NotificationService notices;
    @org.springframework.beans.factory.annotation.Autowired private com.staylanka.common.InputRules rules;
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS = transitions();
    private static final Set<Role> REQUEST_STAFF_ROLES = EnumSet.of(Role.INQUIRY_REQUEST_MANAGER, Role.STAFF, Role.ADMIN);

    private final GuestRequestRepository requestRepository;
    private final RequestResponseRepository responseRepository;
    private final RequestHistoryRepository historyRepository;
    private final CurrentUserService currentUserService;
    private final ReservationService reservationService;
    private final AppUserRepository userRepository;
    private final ReferenceGenerator referenceGenerator;
    private final ApplicationEventPublisher eventPublisher;

    public GuestRequestService(GuestRequestRepository requestRepository,
                               RequestResponseRepository responseRepository,
                               RequestHistoryRepository historyRepository,
                               CurrentUserService currentUserService,
                               ReservationService reservationService,
                               AppUserRepository userRepository,
                               ReferenceGenerator referenceGenerator,
                               ApplicationEventPublisher eventPublisher) {
        this.requestRepository = requestRepository;
        this.responseRepository = responseRepository;
        this.historyRepository = historyRepository;
        this.currentUserService = currentUserService;
        this.reservationService = reservationService;
        this.userRepository = userRepository;
        this.referenceGenerator = referenceGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public GuestRequest create(Authentication authentication, GuestRequestForm form) {
        rules.validate(form);
        CustomerProfile customer = currentUserService.customer(authentication);
        Reservation reservation = linkedReservation(authentication, form.getReservationId());
        GuestRequest request = requestRepository.save(new GuestRequest(uniqueReference(), customer, reservation,
                form.getCategory().trim(), form.getSubject().trim(), form.getDescription().trim(),
                form.getType(), form.getPriority()));
        eventPublisher.publishEvent(new RequestStatusChangedEvent(request, customer.getUser(), null,
                RequestStatus.SUBMITTED, "Request submitted"));
        audit.record(request, "CREATE");
        notices.operations(Role.INQUIRY_REQUEST_MANAGER, "New request " + request.getRequestReference(), "/staff/requests/"+request.getId());
        return request;
    }

    @Transactional
    public void updateOwn(Authentication authentication, Long id, GuestRequestForm form) {
        rules.validate(form);
        GuestRequest request = own(authentication, id);
        if (request.getStatus() != RequestStatus.SUBMITTED) {
            throw new BusinessRuleException("Only submitted requests can be edited.");
        }
        Reservation reservation = linkedReservation(authentication, form.getReservationId());
        request.updateCustomerFields(reservation, form.getCategory().trim(), form.getSubject().trim(),
                form.getDescription().trim(), form.getType(), form.getPriority());
        audit.record(request, "UPDATE");
        eventPublisher.publishEvent(new RequestStatusChangedEvent(request, currentUserService.user(authentication), request.getStatus(), request.getStatus(), "Request details updated"));
    }

    @Transactional
    public void cancelOwn(Authentication authentication, Long id) {
        GuestRequest request = own(authentication, id);
        if (!EnumSet.of(RequestStatus.SUBMITTED, RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS)
                .contains(request.getStatus())) {
            throw new BusinessRuleException("This request can no longer be cancelled.");
        }
        transition(request, currentUserService.user(authentication), RequestStatus.CANCELLED,
                "Cancelled by customer", null);
    }

    @Transactional(readOnly = true)
    public Page<GuestRequest> ownRequests(Authentication authentication, int page) {
        return requestRepository.findByCustomerUserEmailIgnoreCase(authentication.getName(),
                PageRequest.of(Math.max(page, 0), 12, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public GuestRequest own(Authentication authentication, Long id) {
        GuestRequest request = detailed(id);
        if (!request.getCustomer().getUser().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new NotFoundException("Request was not found.");
        }
        return request;
    }

    @Transactional(readOnly = true)
    public GuestRequest detailed(Long id) {
        return requestRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Request was not found."));
    }

    @Transactional(readOnly = true)
    public Page<GuestRequest> search(String term, RequestStatus status, RequestPriority priority,
                                     RequestType type, int page) {
        return requestRepository.search(term == null ? "" : term.trim(), status, priority, type,
                PageRequest.of(Math.max(page, 0), 15, Sort.by(Sort.Direction.DESC, "updatedAt")));
    }

    @Transactional(readOnly = true)
    public List<RequestResponse> responses(Long requestId) {
        return responseRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
    }

    @Transactional(readOnly = true)
    public List<RequestHistory> history(Long requestId) {
        return historyRepository.findByRequestIdOrderByCreatedAtAsc(requestId);
    }

    @Transactional(readOnly = true)
    public List<AppUser> activeStaff() {
        return userRepository.findByRoleInAndActiveTrueOrderByEmailAsc(REQUEST_STAFF_ROLES);
    }

    @Transactional
    public void assign(Authentication authentication, Long id, Long staffId) {
        AppUser actor = currentUserService.user(authentication);
        AppUser staff = userRepository.findById(staffId)
                .filter(user -> REQUEST_STAFF_ROLES.contains(user.getRole()) && user.isActive())
                .orElseThrow(() -> new NotFoundException("Active request staff account was not found."));
        GuestRequest request = detailed(id);
        if (EnumSet.of(RequestStatus.CLOSED, RequestStatus.CANCELLED).contains(request.getStatus())) {
            throw new BusinessRuleException("A closed or cancelled request cannot be assigned.");
        }
        request.assign(staff);
        audit.record(request, "ASSIGN");
        notices.send(staff,"Request assigned: "+request.getRequestReference(),"/staff/requests/"+id);
        if (request.getStatus() == RequestStatus.SUBMITTED) {
            transition(request, actor, RequestStatus.ASSIGNED, "Assigned to " + staff.getEmail(), null);
        } else {
            eventPublisher.publishEvent(new RequestStatusChangedEvent(request, actor, request.getStatus(),
                    request.getStatus(), "Reassigned to " + staff.getEmail()));
        }
    }

    @Transactional
    public void updatePriority(Authentication authentication, Long id, RequestPriority priority) {
        GuestRequest request = detailed(id);
        if (request.getStatus() == RequestStatus.CLOSED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessRuleException("Priority cannot be changed for this request.");
        }
        RequestPriority old = request.getPriority();
        if (priority == null) throw new BusinessRuleException("Choose a priority.");
        request.setPriority(priority);
        audit.record(request, "PRIORITY_CHANGE");
        eventPublisher.publishEvent(new RequestStatusChangedEvent(request, currentUserService.user(authentication),
                request.getStatus(), request.getStatus(), "Priority changed from " + old + " to " + priority));
    }

    @Transactional
    public void addResponse(Authentication authentication, Long id, String message) {
        GuestRequest request = detailed(id);
        if (request.getStatus() == RequestStatus.CLOSED || request.getStatus() == RequestStatus.CANCELLED) {
            throw new BusinessRuleException("Responses cannot be added to a closed or cancelled request.");
        }
        if (message == null || message.isBlank() || message.length() > 2000) throw new BusinessRuleException("Response must contain 1 to 2000 characters.");
        responseRepository.save(new RequestResponse(request, currentUserService.user(authentication), message.trim()));
        audit.record(request, "RESPONSE");
        notices.send(request.getCustomer().getUser(), "A response was added to " + request.getRequestReference(), "/customer/requests/" + id);
        eventPublisher.publishEvent(new RequestStatusChangedEvent(request, currentUserService.user(authentication), request.getStatus(), request.getStatus(), "Staff response added"));
    }

    @Transactional
    public void start(Authentication authentication, Long id) {
        transition(detailed(id), currentUserService.user(authentication), RequestStatus.IN_PROGRESS,
                "Work started", null);
    }

    @Transactional
    public void resolve(Authentication authentication, Long id, String resolution) {
        if (resolution == null || resolution.isBlank()) {
            throw new BusinessRuleException("Resolution details are required.");
        }
        transition(detailed(id), currentUserService.user(authentication), RequestStatus.RESOLVED,
                "Request resolved", resolution.trim());
    }

    @Transactional
    public void close(Authentication authentication, Long id) {
        transition(detailed(id), currentUserService.user(authentication), RequestStatus.CLOSED,
                "Request closed", null);
    }

    @Transactional
    public void reopen(Authentication authentication, Long id) {
        transition(detailed(id), currentUserService.user(authentication), RequestStatus.IN_PROGRESS,
                "Request reopened", null);
    }

    @Transactional
    public void archive(Authentication authentication, Long id) {
        GuestRequest request = detailed(id);
        if (!Set.of(RequestStatus.CLOSED, RequestStatus.CANCELLED).contains(request.getStatus()))
            throw new BusinessRuleException("Close or cancel the request before archiving.");
        request.setArchived(true);
        audit.record(request, "ARCHIVE");
        eventPublisher.publishEvent(new RequestStatusChangedEvent(request,currentUserService.user(authentication),request.getStatus(),request.getStatus(),"Request archived"));
    }

    @Transactional(readOnly = true)
    public long openCount() {
        return requestRepository.countOpen();
    }

    private Reservation linkedReservation(Authentication authentication, Long id) {
        return id == null ? null : reservationService.own(authentication, id);
    }

    private void transition(GuestRequest request, AppUser actor, RequestStatus target,
                            String note, String resolution) {
        RequestStatus current = request.getStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BusinessRuleException("Request cannot move from " + current + " to " + target + ".");
        }
        if (request.isArchived()) throw new BusinessRuleException("Archived requests cannot be changed.");
        if (target == RequestStatus.IN_PROGRESS && request.getAssignedStaff() == null) throw new BusinessRuleException("Assign an owner before processing.");
        request.transitionTo(target, resolution);
        audit.record("GuestRequest", request.getId(), "STATUS_CHANGE", current + " -> " + target);
        notices.send(request.getCustomer().getUser(), "Request " + request.getRequestReference() + ": " + target, "/customer/requests/" + request.getId());
        eventPublisher.publishEvent(new RequestStatusChangedEvent(request, actor, current, target, note));
    }

    private String uniqueReference() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String reference = referenceGenerator.next("REQ");
            if (!requestRepository.existsByRequestReference(reference)) {
                return reference;
            }
        }
        throw new ConflictException("A request reference could not be generated. Please retry.");
    }

    private static Map<RequestStatus, Set<RequestStatus>> transitions() {
        Map<RequestStatus, Set<RequestStatus>> map = new EnumMap<>(RequestStatus.class);
        map.put(RequestStatus.SUBMITTED, EnumSet.of(RequestStatus.ASSIGNED, RequestStatus.CANCELLED));
        map.put(RequestStatus.ASSIGNED, EnumSet.of(RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED));
        map.put(RequestStatus.IN_PROGRESS, EnumSet.of(RequestStatus.RESOLVED, RequestStatus.CANCELLED));
        map.put(RequestStatus.CLOSED, EnumSet.of(RequestStatus.IN_PROGRESS));
        map.put(RequestStatus.RESOLVED, EnumSet.of(RequestStatus.CLOSED, RequestStatus.IN_PROGRESS));
        return Map.copyOf(map);
    }
}
