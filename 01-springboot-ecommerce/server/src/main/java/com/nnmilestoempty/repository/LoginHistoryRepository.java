package com.nnmilestoempty.repository;

import com.nnmilestoempty.model.dao.auth.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
    @Query("SELECT lh FROM LoginHistory lh WHERE (:principal is null or lh.principal = :principal) and (:type is null"
            + " or lh.type = :type) and lh.eventTime > :eventTime")
    List<LoginHistory> findLoginHistoryByPrincipalAndTypeAndEventTime(String principal, Instant eventTime, String type);
}
