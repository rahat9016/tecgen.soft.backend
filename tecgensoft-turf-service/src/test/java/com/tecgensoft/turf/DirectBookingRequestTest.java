package com.tecgensoft.turf;

import com.tecgensoft.turf.http.dtos.TurfRequests.BookingRequest;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DirectBookingRequestTest {
  @Test void publicBookingNeedsPhoneButNoCustomerAccountOrName() {
    var request=new BookingRequest(UUID.randomUUID(),LocalDate.of(2026,11,20),
        LocalTime.of(18,0),LocalTime.of(19,0),"01700000000",null,"Evening football");
    var command=request.toEntity();
    assertEquals("01700000000",command.customerPhone());
    assertNull(command.customerName());
    assertEquals(18,command.slot().startHour());
    assertEquals(1,command.slot().durationHours());
  }

  @Test void bookingRequiresWholeHoursAndIncreasingTime() {
    var request=new BookingRequest(UUID.randomUUID(),LocalDate.now(),LocalTime.of(18,30),
        LocalTime.of(19,30),"01700000000",null,null);
    assertThrows(IllegalArgumentException.class,request::toEntity);
  }
}
