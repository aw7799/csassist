package com.csassist.service.ticket;

import java.util.Set;

public class IllegalTransitionException extends RuntimeException {

    private final TicketStatus currentStatus;
    private final Set<TicketStatus> allowedNext;

    public IllegalTransitionException(TicketStatus currentStatus, Set<TicketStatus> allowedNext) {
        super("Cannot transition from " + currentStatus + "; allowed next states: " + allowedNext);
        this.currentStatus = currentStatus;
        this.allowedNext = allowedNext;
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public Set<TicketStatus> getAllowedNext() {
        return allowedNext;
    }
}
