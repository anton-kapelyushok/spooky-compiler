package Bookings.Monetisation.Mopers.Api;

import Bookings.Monetisation.Mopers.MomoCharge;
import Bookings.Monetisation.Mopers.RawMomo;
import perl.ArrayRef;
import perl.PerlDto;
import perl.PerlModule;

public interface MopersClient extends PerlModule {

    ArrayRef<MomoResponse> createMomo(ArrayRef<MomoRequest> requests);

    record MomoRequest(
            ArrayRef<String> perspectives,
            Payload payload
    ) implements PerlDto {

        public record Payload(
                ArrayRef<MomoCharge> charges
        ) implements PerlDto {
        }
    }

    record MomoResponse(
            RawMomo momo
    ) implements PerlDto {
    }
}
