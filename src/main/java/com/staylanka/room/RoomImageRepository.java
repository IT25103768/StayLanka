package com.staylanka.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {
    List<RoomImage> findByRoomIdOrderByPrimaryImageDescIdAsc(Long roomId);

    @Query("""
            select image from RoomImage image
            where image.room.id in :roomIds
            order by image.room.id asc, image.primaryImage desc, image.id asc
            """)
    List<RoomImage> findForRooms(@Param("roomIds") List<Long> roomIds);

    boolean existsByRoomId(Long roomId);
}
