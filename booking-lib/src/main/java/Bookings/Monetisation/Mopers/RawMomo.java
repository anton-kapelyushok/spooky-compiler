package Bookings.Monetisation.Mopers;

import perl.PerlDto;

public record RawMomo(
        Integer major_version,
        Integer minor_version,
        Integer patch_version,

        String payload
) implements PerlDto {
}
