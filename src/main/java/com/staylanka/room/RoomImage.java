package com.staylanka.room;

import com.staylanka.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "room_images")
public class RoomImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "storage_path", nullable = false, unique = true, length = 255)
    private String storagePath;

    @Column(name = "primary_image", nullable = false)
    private boolean primaryImage;

    protected RoomImage() {
    }

    public RoomImage(
            Room room,
            String fileName,
            String contentType,
            String storagePath,
            boolean primaryImage
    ) {
        this.room = room;
        this.fileName = fileName;
        this.contentType = contentType;
        this.storagePath = storagePath;
        this.primaryImage = primaryImage;
    }

    public Room getRoom() {
        return room;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public boolean isPrimaryImage() {
        return primaryImage;
    }

    public void setPrimaryImage(boolean primaryImage) {
        this.primaryImage = primaryImage;
    }
}