package com.staylanka.room;
import com.staylanka.common.*;
import com.staylanka.reservation.ReservationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDate;
import java.util.List;
@Service
public class MaintenanceService {
    private final EntityManager em;private final RoomRepository rooms;private final ReservationRepository bookings;private final AuditService audit;
    public MaintenanceService(EntityManager em,RoomRepository rooms,ReservationRepository bookings,AuditService audit){this.em=em;this.rooms=rooms;this.bookings=bookings;this.audit=audit;}
    @Transactional(readOnly=true) public boolean blocked(Long room,LocalDate start,LocalDate end){return em.createQuery("select count(b) from MaintenanceBlock b where b.room.id=:room and b.active=true and b.startDate<:end and b.endDate>:start",Long.class).setParameter("room",room).setParameter("start",start).setParameter("end",end).getSingleResult()>0;}
    @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ROOM_MANAGER','ADMIN')")
    public List<MaintenanceBlock> all(){return em.createQuery("select b from MaintenanceBlock b join fetch b.room order by b.startDate desc",MaintenanceBlock.class).setMaxResults(100).getResultList();}
    @Transactional @PreAuthorize("hasAnyRole('ROOM_MANAGER','ADMIN')")
    public void create(Long roomId,LocalDate start,LocalDate end,String reason){
        if(start==null||end==null||start.isBefore(LocalDate.now())||!end.isAfter(start)||reason==null||reason.isBlank()||reason.length()>500)throw new BusinessRuleException("Provide future valid dates and a reason of 1–500 characters.");
        Room room=rooms.findByIdForUpdate(roomId).orElseThrow(()->new NotFoundException("Room not found."));
        if(bookings.countBlockingOverlaps(roomId,start,end,null)>0||(room.getStatus()==RoomStatus.OCCUPIED&&!start.isAfter(LocalDate.now())))throw new BusinessRuleException("Maintenance overlaps an active booking or stay.");
        MaintenanceBlock block=new MaintenanceBlock(room,start,end,reason.trim());em.persist(block);audit.record("MaintenanceBlock",block.getId(),"CREATE","Room "+roomId+": "+start+" to "+end);
    }
    @Transactional @PreAuthorize("hasAnyRole('ROOM_MANAGER','ADMIN')") public void release(Long id){MaintenanceBlock block=em.find(MaintenanceBlock.class,id);if(block==null)throw new NotFoundException("Maintenance block not found.");rooms.findByIdForUpdate(block.getRoom().getId()).orElseThrow();block.deactivate();audit.record("MaintenanceBlock",id,"DEACTIVATE","Maintenance released; history retained");}
}
