package com.nnmilestoempty.actuator;

import com.nnmilestoempty.model.dao.auth.LoginHistory;
import com.nnmilestoempty.repository.LoginHistoryRepository;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
public class LoginHistoryAuditRepository implements AuditEventRepository {
    private final LoginHistoryRepository repository;

    public LoginHistoryAuditRepository(LoginHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void add(AuditEvent event) {
        LoginHistory loginHistory = new LoginHistory();
        loginHistory.setPrincipal(event.getPrincipal());
        loginHistory.setType(event.getType());
        loginHistory.setEventTime(event.getTimestamp());
        loginHistory.setIpAddress((String) event.getData().get("ip"));
        repository.save(loginHistory);
    }

    @Override
    public List<AuditEvent> find(String principal, Instant after, String type) {
        if (after == null) {
            after = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        List<LoginHistory> results = repository.findLoginHistoryByPrincipalAndTypeAndEventTime(principal, after, type);
        List<AuditEvent> found = new ArrayList<>(results.size());
        for (LoginHistory result : results) {
            AuditEvent event = new AuditEvent(result.getPrincipal(), result.getType(), "ip=" + result.getIpAddress());
            found.add(event);
        }

        return found;
    }
}
