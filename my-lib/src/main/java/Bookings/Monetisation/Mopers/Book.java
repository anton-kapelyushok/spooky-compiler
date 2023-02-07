package Bookings.Monetisation.Mopers;

import Bookings.Monetisation.Mopers.Api.MopersClient;
import Bookings.Monetisation.Mopers.Common.MoperChargesFromBreakdown;
import Bookings.Payment.Util.Pricing;
import Bookings.PriceDetail.Context;
import Bookings.Tools.TravelPurpose;
import perl.ArrayRef;
import perl.PerlDto;
import perl.PerlModule;

@lombok.RequiredArgsConstructor
public class Book implements PerlModule {

    private final Pricing pricingModule;
    private final TravelPurpose travelPurposeModule;
    private final Context priceDetailContextModule;
    private final MoperChargesFromBreakdown moperChargesFromBreakdownModule;
    private final MopersClient mopersClientModule;

    public ArrayRef<RawMomo> generate_mopers_payload_for_new_reservation(GenerateMopersPayloadForNewReservationArgs args) {

        var momo_requests = args.room_reservations.map(rr -> {
            var price = pricingModule._summary_for_stay(rr.stay_info, rr.nr_guests);
            var travel_purpose_constant = travelPurposeModule.string_to_constant(args.travel_purpose);

            var price_context = priceDetailContextModule.__new(new Context.NewContextArgs(
                    price, args.price_mode, travel_purpose_constant, args.booker_cc1
            ));

            var breakdown = price_context.breakdown();

            var charges = moperChargesFromBreakdownModule.parse_breakdown_to_moper_charges(new MoperChargesFromBreakdown.ParseBreakdownToMoperChargesArgs(
                    breakdown,
                    rr.currencycode,
                    args.hotel_id,
                    args.checkin_date,
                    args.checkout_date
            ));

            return new MopersClient.MomoRequest(
                    new ArrayRef<>(),
                    new MopersClient.MomoRequest.Payload(charges)
            );
        });

        var momo_responses = mopersClientModule.createMomo(momo_requests);
        return momo_responses.map(it -> it.momo());
    }

    public void testMethod(GenerateMopersPayloadForNewReservationArgs args) {
        var poupa = args.room_reservations.get(0).currencycode;
    }

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

    public record RoomReservation(
            Object stay_info,
            Integer nr_guests,
            String currencycode
    ) implements PerlDto {
    }
}
