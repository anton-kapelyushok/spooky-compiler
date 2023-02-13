package Booking.Monetisation.Mopers;

import Booking.Monetisation.Mopers.Bookings.Monetisation.Mopers.Core.Momo;
import Bookings.Monetisation.Mopers.MomoCharge;
import Bookings.Monetisation.Mopers.RawMomo;
import perl.ArrayRef;
import perl.PerlDto;

public interface Codec {
    Momo create(CreatePayload payload);

    RawMomo encode(Momo momo);

    Momo decode(RawMomo rawMomo);

    record CreatePayload(
            ArrayRef<MomoCharge> charges
    ) implements PerlDto {
    }
}
