package com.csassist.service.ticket;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TicketStatusTransitionMatrixTest {

    private static final Set<String> ALLOWED = Set.of(
            "OPEN->IN_PROGRESS",
            "IN_PROGRESS->OPEN",
            "IN_PROGRESS->RESOLVED",
            "RESOLVED->IN_PROGRESS",
            "RESOLVED->CLOSED",
            "CLOSED->IN_PROGRESS"
    );

    static Stream<Arguments> allPairs() {
        return Stream.of(TicketStatus.values())
                .flatMap(from -> Stream.of(TicketStatus.values())
                        .map(to -> Arguments.of(from, to)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allPairs")
    void matchesSpecMatrix(TicketStatus from, TicketStatus to) {
        boolean expected = ALLOWED.contains(from + "->" + to);
        assertThat(from.canTransitionTo(to))
                .as("%s -> %s".formatted(from, to))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("allPairs")
    void selfTransitionsAreNeverAllowed(TicketStatus from, TicketStatus to) {
        if (from == to) {
            assertThat(from.canTransitionTo(to)).isFalse();
        }
    }

    @org.junit.jupiter.api.Test
    void allowedNextReflectsTheSameMatrix() {
        assertThat(TicketStatus.OPEN.allowedNext()).containsExactlyInAnyOrder(TicketStatus.IN_PROGRESS);
        assertThat(TicketStatus.IN_PROGRESS.allowedNext()).containsExactlyInAnyOrder(TicketStatus.OPEN, TicketStatus.RESOLVED);
        assertThat(TicketStatus.RESOLVED.allowedNext()).containsExactlyInAnyOrder(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED);
        assertThat(TicketStatus.CLOSED.allowedNext()).containsExactlyInAnyOrder(TicketStatus.IN_PROGRESS);
    }
}
