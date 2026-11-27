package com.elec5619.libraryreservationreviewhub.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * The server keeps the reservation dialog's date rules: pickup from today to 7 days ahead, due at least one day
 * and at most 30 days after the pickup, also when the pickup date changes. "Today" is the library's day (Sydney),
 * as the dialog's is. Book 2 has copies on the shelf.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationDateRulesIntegrationTest {

    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Australia/Sydney"));

    @Autowired
    private MockMvc mockMvc;

    private MockHttpSession reader;

    @BeforeEach
    void signIn() throws Exception {
        reader = (MockHttpSession) mockMvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"hanchen.wang@example.com\",\"userPassword\":\"123456789\"}"))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getRequest().getSession();
    }

    private ResultActions reserve(LocalDate pickup, LocalDate due) throws Exception {
        return mockMvc.perform(post("/reservation/create/2").session(reader).contentType(MediaType.APPLICATION_JSON)
                .content("{\"pickupTime\":\"" + pickup + "\",\"dueTime\":\"" + due + "\"}"));
    }

    private ResultActions movePickup(long reservationId, LocalDate pickup) throws Exception {
        return mockMvc.perform(put("/reservation/pickup/" + reservationId).session(reader)
                .contentType(MediaType.APPLICATION_JSON).content("{\"pickupTime\":\"" + pickup + "\"}"));
    }

    private long reserved(LocalDate pickup, LocalDate due) throws Exception {
        String body = reserve(pickup, due).andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data")).longValue();
    }

    @Test
    void theDialogsWidestDatesAreAccepted() throws Exception {
        reserve(TODAY, TODAY.plusDays(1)).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void aPickupSevenDaysAheadWithAThirtyDayLoanIsAccepted() throws Exception {
        reserve(TODAY.plusDays(7), TODAY.plusDays(37)).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void aDueDateOnThePickupDayIsRefused() throws Exception {
        reserve(TODAY.plusDays(1), TODAY.plusDays(1))
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Due date must be after pickup date"));
    }

    @Test
    void aLoanLongerThanThirtyDaysIsRefused() throws Exception {
        reserve(TODAY.plusDays(1), TODAY.plusDays(32))
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Reservation period cannot exceed 30 days"));
    }

    @Test
    void aPickupMoreThanSevenDaysAheadIsRefused() throws Exception {
        reserve(TODAY.plusDays(8), TODAY.plusDays(10))
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Pickup date must be within the next 7 days"));
    }

    @Test
    void movingThePickupEarlierCanNotStretchTheLoanPastThirtyDays() throws Exception {
        long id = reserved(TODAY.plusDays(7), TODAY.plusDays(37));

        movePickup(id, TODAY)
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Reservation period cannot exceed 30 days"));
    }

    @Test
    void movingThePickupMoreThanSevenDaysAheadIsRefused() throws Exception {
        long id = reserved(TODAY.plusDays(1), TODAY.plusDays(20));

        movePickup(id, TODAY.plusDays(8))
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.message").value("Pickup date must be within the next 7 days"));
    }

    @Test
    void movingThePickupWithinTheRulesWorks() throws Exception {
        long id = reserved(TODAY.plusDays(1), TODAY.plusDays(20));

        movePickup(id, TODAY.plusDays(5)).andExpect(jsonPath("$.code").value(0));
    }
}
