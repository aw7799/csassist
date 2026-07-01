package com.csassist.service.ticket;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TicketStatus {
    OPEN, IN_PROGRESS, RESOLVED, CLOSED;

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OPEN, EnumSet.of(IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(IN_PROGRESS, EnumSet.of(OPEN, RESOLVED));
        ALLOWED_TRANSITIONS.put(RESOLVED, EnumSet.of(IN_PROGRESS, CLOSED));
        ALLOWED_TRANSITIONS.put(CLOSED, EnumSet.of(IN_PROGRESS));
    }

    public boolean canTransitionTo(TicketStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public Set<TicketStatus> allowedNext() {
        return Set.copyOf(ALLOWED_TRANSITIONS.get(this));
    }
}
