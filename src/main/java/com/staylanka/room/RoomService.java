package com.staylanka.room;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.common.ConflictException;
import com.staylanka.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomImageRepository imageRepository;
    private final FileStorageService fileStorageService;

    public RoomService(RoomRepository roomRepository, RoomTypeRepository roomTypeRepository,
                       RoomImageRepository imageRepository, FileStorageService fileStorageService) {
        this.roomRepository = roomRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.imageRepository = imageRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public Page<Room> browse(String term, int page) {
        return roomRepository.search(term == null ? "" : term.trim(),
                PageRequest.of(Math.max(page, 0), 9, Sort.by("roomNumber").ascending()));
    }

    @Transactional(readOnly = true)
    public Page<Room> browsePublic(String term, int page) {
        return roomRepository.searchPublic(term == null ? "" : term.trim(),
                PageRequest.of(Math.max(page, 0), 9, Sort.by("roomNumber").ascending()));
    }

    @Transactional(readOnly = true)
    public Page<Room> available(RoomSearchForm form, int page) {
        validateSearch(form);
        return roomRepository.findAvailable(form.getCheckIn(), form.getCheckOut(), form.getGuests(),
                form.getRoomTypeId(), form.getMaxPrice(),
                PageRequest.of(Math.max(page, 0), 9, Sort.by("nightlyPrice").ascending()));
    }

    public void validateSearch(RoomSearchForm form) {
        if (form.getCheckIn() == null || form.getCheckOut() == null) {
            throw new BusinessRuleException("Both check-in and check-out dates are required.");
        }
        if (form.getCheckIn().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Check-in cannot be in the past.");
        }
        if (!form.getCheckOut().isAfter(form.getCheckIn())) {
            throw new BusinessRuleException("Check-out must be later than check-in.");
        }
    }

    @Transactional(readOnly = true)
    public Room get(Long id) {
        return roomRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Room was not found."));
    }

    @Transactional(readOnly = true)
    public Room getPublic(Long id) {
        return roomRepository.findPublicDetailedById(id)
                .orElseThrow(() -> new NotFoundException("Room was not found."));
    }

    @Transactional(readOnly = true)
    public List<RoomImage> images(Long roomId) {
        return imageRepository.findByRoomIdOrderByPrimaryImageDescIdAsc(roomId);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> primaryImagePaths(List<Room> rooms) {
        Map<Long, String> paths = new LinkedHashMap<>();
        for (Room room : rooms) {
            imageRepository.findByRoomIdOrderByPrimaryImageDescIdAsc(room.getId()).stream()
                    .findFirst()
                    .ifPresent(image -> paths.put(room.getId(), image.getStoragePath()));
        }
        return paths;
    }

    @Transactional(readOnly = true)
    public List<RoomType> activeTypes() {
        return roomTypeRepository.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<RoomType> allTypes() {
        return roomTypeRepository.findAll(Sort.by("name").ascending());
    }

    @Transactional(readOnly = true)
    public RoomType getType(Long id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room type was not found."));
    }

    @Transactional
    public RoomType createType(RoomTypeForm form) {
        if (roomTypeRepository.existsByNameIgnoreCase(form.getName())) {
            throw new ConflictException("A room type with this name already exists.");
        }
        return roomTypeRepository.save(new RoomType(form.getName().trim(), trimToNull(form.getDescription()),
                form.getCapacity(), form.getBedInformation().trim(), form.getBasePrice(),
                trimToNull(form.getAmenities())));
    }

    @Transactional
    public void updateType(Long id, RoomTypeForm form) {
        RoomType type = getType(id);
        if (roomTypeRepository.existsByNameIgnoreCaseAndIdNot(form.getName(), id)) {
            throw new ConflictException("A room type with this name already exists.");
        }
        type.update(form.getName().trim(), trimToNull(form.getDescription()), form.getCapacity(),
                form.getBedInformation().trim(), form.getBasePrice(), trimToNull(form.getAmenities()));
    }

    @Transactional
    public void toggleType(Long id) {
        RoomType type = getType(id);
        type.setActive(!type.isActive());
    }

    @Transactional
    public Room create(RoomForm form) {
        if (roomRepository.existsByRoomNumberIgnoreCase(form.getRoomNumber())) {
            throw new ConflictException("This room number is already in use.");
        }
        RoomType type = getType(form.getRoomTypeId());
        Room room = roomRepository.save(new Room(form.getRoomNumber().trim(), type,
                trimToNull(form.getDescription()), form.getNightlyPrice(), form.getStatus()));
        saveImageIfPresent(room, form);
        return room;
    }

    @Transactional
    public void update(Long id, RoomForm form) {
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Room was not found."));
        if (roomRepository.existsByRoomNumberIgnoreCaseAndIdNot(form.getRoomNumber(), id)) {
            throw new ConflictException("This room number is already in use.");
        }
        RoomType type = getType(form.getRoomTypeId());
        room.update(form.getRoomNumber().trim(), type, trimToNull(form.getDescription()),
                form.getNightlyPrice(), form.getStatus());
        saveImageIfPresent(room, form);
    }

    @Transactional
    public void deactivate(Long id) {
        Room room = roomRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Room was not found."));
        if (room.getStatus() == RoomStatus.OCCUPIED) {
            throw new BusinessRuleException("An occupied room cannot be deactivated.");
        }
        room.setStatus(RoomStatus.INACTIVE);
    }

    @Transactional(readOnly = true)
    public long countAvailable() {
        return roomRepository.countByStatus(RoomStatus.AVAILABLE);
    }

    private void saveImageIfPresent(Room room, RoomForm form) {
        if (form.getImage() == null || form.getImage().isEmpty()) {
            return;
        }
        FileStorageService.StoredFile stored = fileStorageService.storeRoomImage(form.getImage());
        boolean primary = !imageRepository.existsByRoomId(room.getId());
        imageRepository.save(new RoomImage(room, stored.originalName(), stored.contentType(),
                stored.relativePath(), primary));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
