package Bookings.Monetisation.Mopers;

import perl.PerlDto;

public record RoomReservation(
        Object stay_info,
        Integer nr_guests,
        String currencycode
) implements PerlDto {
}
