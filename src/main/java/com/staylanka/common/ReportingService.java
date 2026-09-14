package com.staylanka.common;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
@Service
public class ReportingService {
    private final EntityManager em;public ReportingService(EntityManager em){this.em=em;}
    @Transactional(readOnly=true) public Map<String,Object> summary(){
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("Today's arrivals",em.createNativeQuery("SELECT COUNT(*) FROM reservations WHERE status='CONFIRMED' AND check_in_date=?1").setParameter(1,LocalDate.now()).getSingleResult());
        result.put("Today's departures",em.createNativeQuery("SELECT COUNT(*) FROM stays s JOIN reservations r ON r.id=s.reservation_id WHERE s.actual_check_out IS NULL AND s.voided=FALSE AND r.check_out_date=?1").setParameter(1,LocalDate.now()).getSingleResult());
        result.put("Maintenance rooms",em.createNativeQuery("SELECT COUNT(*) FROM rooms r WHERE r.status='MAINTENANCE' OR EXISTS(SELECT 1 FROM maintenance_blocks b WHERE b.room_id=r.id AND b.active=TRUE AND b.start_date<=?1 AND b.end_date>?1)").setParameter(1,LocalDate.now()).getSingleResult());
        result.put("Approved review average",em.createNativeQuery("SELECT COALESCE(ROUND(AVG(rating),1),0) FROM reviews WHERE status='APPROVED'").getSingleResult());return result;
    }
}
