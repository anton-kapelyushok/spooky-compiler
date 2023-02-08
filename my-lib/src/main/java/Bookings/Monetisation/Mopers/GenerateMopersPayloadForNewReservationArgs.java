package Bookings.Monetisation.Mopers;

import perl.ArrayRef;
import perl.PerlDto;

public record GenerateMopersPayloadForNewReservationArgs(
        ArrayRef<RoomReservation> room_reservations,
        Integer price_mode,
        String travel_purpose,
        String booker_cc1,
        Long hotel_id,
        String checkin_date,
        String checkout_date
) implements PerlDto {
}
