package com.staylanka.room;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="maintenance_blocks")
public class MaintenanceBlock {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="room_id",nullable=false) private Room room;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date",nullable=false) private LocalDate endDate;
    @Column(nullable=false,length=500) private String reason;
    @Column(nullable=false) private boolean active=true;
    protected MaintenanceBlock(){}
    public MaintenanceBlock(Room room,LocalDate start,LocalDate end,String reason){this.room=room;startDate=start;endDate=end;this.reason=reason;}
    public Long getId(){return id;} public Room getRoom(){return room;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;}public String getReason(){return reason;}public boolean isActive(){return active;}public void deactivate(){active=false;}
}
