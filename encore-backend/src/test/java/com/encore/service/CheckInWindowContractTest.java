package com.encore.service;

import cn.dev33.satoken.stp.StpUtil;
import com.encore.common.ErrorCode;
import com.encore.dto.CheckInResponse;
import com.encore.entity.ShowEntity;
import com.encore.entity.ShowSchedule;
import com.encore.entity.TicketItem;
import com.encore.entity.TicketOrder;
import com.encore.entity.UserAccount;
import com.encore.exception.BusinessException;
import com.encore.mapper.ShowMapper;
import com.encore.mapper.ShowScheduleMapper;
import com.encore.mapper.TicketItemMapper;
import com.encore.mapper.TicketOrderMapper;
import com.encore.mapper.UserAccountMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Independent trial contract from PROJECT_MEMORY.md: inclusive from two hours
// before the show starts until its end. No production constants are consulted.
class CheckInWindowContractTest {
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDateTime START = LocalDateTime.of(2026, 5, 24, 19, 30);
    private static final LocalDateTime END = LocalDateTime.of(2026, 5, 24, 22, 0);
    private static final LocalDateTime OPEN = LocalDateTime.of(2026, 5, 24, 17, 30);

    record Boundary(String id, LocalDateTime now, boolean allowed, String error) {
        @Override public String toString() { return id; }
    }

    static Stream<Boundary> boundaries() {
        return Stream.of(
                new Boundary("before-open-1ns", OPEN.minusNanos(1), false, "未到检票时间，开演前 2 小时开放检票"),
                new Boundary("at-open", OPEN, true, null),
                new Boundary("after-open-1ns", OPEN.plusNanos(1), true, null),
                new Boundary("at-show-start", START, true, null),
                new Boundary("before-end-1ns", END.minusNanos(1), true, null),
                new Boundary("at-end", END, true, null),
                new Boundary("after-end-1ns", END.plusNanos(1), false, "演出已结束，无法检票")
        );
    }

    static Stream<Arguments> verificationCases() {
        return boundaries().flatMap(row -> Stream.of(Arguments.of(row, false), Arguments.of(row, true)));
    }

    private static class Fixture {
        final TicketItemMapper tickets = mock(TicketItemMapper.class);
        final TicketOrderMapper orders = mock(TicketOrderMapper.class);
        final ShowScheduleMapper schedules = mock(ShowScheduleMapper.class);
        final ShowMapper shows = mock(ShowMapper.class);
        final UserAccountMapper users = mock(UserAccountMapper.class);
        final DashboardRefreshPublisher refresh = mock(DashboardRefreshPublisher.class);
        final CheckInService service;
        final ShowSchedule schedule = new ShowSchedule();
        final TicketItem ticket = new TicketItem();

        Fixture(LocalDateTime now) {
            service = new CheckInService(tickets, orders, schedules, shows, users,
                    Clock.fixed(now.atZone(ZONE).toInstant(), ZONE), refresh);
            UserAccount checker = new UserAccount();
            checker.setId("trial-checker"); checker.setRole("checker"); checker.setStatus("ACTIVE");
            when(users.selectById("trial-checker")).thenReturn(checker);
            schedule.setId("trial-schedule"); schedule.setShowId("trial-show");
            schedule.setStatus("ON_SALE"); schedule.setStartTime(START); schedule.setEndTime(END);
            schedule.setTheaterName("Trial venue");
            ShowEntity show = new ShowEntity(); show.setId("trial-show"); show.setTitle("Trial show");
            when(shows.selectById("trial-show")).thenReturn(show);
            when(schedules.selectById("trial-schedule")).thenReturn(schedule);
            when(schedules.selectList(any())).thenReturn(List.of(schedule));
            ticket.setId("trial-ticket"); ticket.setTicketCode("TRIAL-CODE"); ticket.setOrderId("trial-order");
            ticket.setScheduleId("trial-schedule"); ticket.setSeatId("trial-seat"); ticket.setStatus("UNUSED");
            TicketOrder order = new TicketOrder(); order.setId("trial-order"); order.setStatus("PAID");
            when(tickets.selectOne(any())).thenReturn(ticket);
            when(orders.selectById("trial-order")).thenReturn(order);
            when(tickets.markCheckedInIfUnused(eq("trial-ticket"), eq(now))).thenReturn(1);
        }
    }

    @ParameterizedTest(name = "{0}, bound={1}")
    @MethodSource("verificationCases")
    void verifyEnforcesInclusiveWindow(Boundary row, boolean bound) {
        Fixture f = new Fixture(row.now());
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsString).thenReturn("trial-checker");
            if (row.allowed()) {
                CheckInResponse[] response = new CheckInResponse[1];
                assertThatCode(() -> response[0] = f.service.verify("TRIAL-CODE", bound ? "trial-schedule" : null))
                        .doesNotThrowAnyException();
                assertThat(response[0].status()).isEqualTo("CHECKED_IN");
                assertThat(response[0].checkedInAt()).isEqualTo(row.now());
                verify(f.tickets).markCheckedInIfUnused("trial-ticket", row.now());
                verify(f.refresh).publish("TICKET_CHECKED_IN", "trial-ticket");
            } else {
                assertThatThrownBy(() -> f.service.verify("TRIAL-CODE", bound ? "trial-schedule" : null))
                        .isInstanceOf(BusinessException.class).hasMessage(row.error())
                        .extracting("code").isEqualTo(ErrorCode.CONFLICT);
                verify(f.tickets, never()).markCheckedInIfUnused(any(), any());
                verify(f.tickets, never()).updateById(any(TicketItem.class));
                assertThat(f.ticket.getStatus()).isEqualTo("UNUSED");
                verifyNoInteractions(f.refresh);
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("boundaries")
    void scheduleAvailabilityUsesTheSameWindow(Boundary row) {
        Fixture f = new Fixture(row.now());
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsString).thenReturn("trial-checker");
            var listed = f.service.listCheckInSchedules();
            assertThat(listed).hasSize(1);
            assertThat(listed.get(0).id()).isEqualTo("trial-schedule");
            assertThat(listed.get(0).checkInOpen()).isEqualTo(row.allowed());
            verify(f.tickets, never()).markCheckedInIfUnused(any(), any());
            verifyNoInteractions(f.refresh);
        }
    }
}
