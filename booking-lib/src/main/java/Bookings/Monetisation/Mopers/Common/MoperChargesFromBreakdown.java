package Bookings.Monetisation.Mopers.Common;

import Bookings.Monetisation.Mopers.MomoCharge;
import Bookings.PriceDetail.Breakdown;
import perl.ArrayRef;
import perl.PerlDto;
import perl.PerlModule;

public interface MoperChargesFromBreakdown extends PerlModule {
    ArrayRef<MomoCharge> parse_breakdown_to_moper_charges(ParseBreakdownToMoperChargesArgs args);

    record ParseBreakdownToMoperChargesArgs(
            Breakdown breakdown,
            String currencycode,
            Long hotel_id,
            String checkin,
            String checkout
    ) implements PerlDto {
    }
}
