package com.staylanka.room;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findByRoomIdOrderByPrimaryImageDescIdAsc(Long roomId);
    boolean existsByRoomId(Long roomId);
}

