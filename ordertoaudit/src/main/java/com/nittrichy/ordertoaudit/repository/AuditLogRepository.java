package com.nittrichy.ordertoaudit.repository;

import com.nittrichy.ordertoaudit.entity.AuditLog;
import com.nittrichy.ordertoaudit.enums.AuditAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityNameAndEntityIdOrderByTimestampDesc(String entityName, Long entityId);
    List<AuditLog> findByPerformedByOrderByTimestampDesc(String performedBy);
    List<AuditLog> findByAction(AuditAction action);
    List<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
}
