package younan.george.littlepaycodingchallenge.service;

import org.springframework.stereotype.Service;
import younan.george.littlepaycodingchallenge.dto.TapDetail;
import younan.george.littlepaycodingchallenge.dto.TravelPrice;
import younan.george.littlepaycodingchallenge.dto.TravelPriceId;
import younan.george.littlepaycodingchallenge.dto.TripResult;
import younan.george.littlepaycodingchallenge.enums.StopId;
import younan.george.littlepaycodingchallenge.enums.TapType;
import younan.george.littlepaycodingchallenge.enums.TripStatus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class TravelCostService {
    private HashMap<TravelPriceId, BigDecimal> travelPrices;

    public TravelCostService() {
        travelPrices = new HashMap<>();
        travelPrices.put(new TravelPriceId(StopId.STOP_1, StopId.STOP_2), new BigDecimal("3.25"));
        travelPrices.put(new TravelPriceId(StopId.STOP_2, StopId.STOP_3), new BigDecimal("5.50"));
        travelPrices.put(new TravelPriceId(StopId.STOP_1, StopId.STOP_3), new BigDecimal("7.30"));
    }

    public TripResult calculateCost(TapDetail currentTap, TapDetail nextTap) {
        if (currentTap == null) {
            throw new IllegalArgumentException("currentTap must be non null!");
        }
        if (isIncomplete(currentTap, nextTap)) {
            return calculateCostForIncompleteTrip(currentTap);
        }

        return calculateCostForCompletedTrip(currentTap, nextTap);
    }

    private TripResult calculateCostForCompletedTrip(TapDetail currentTap, TapDetail nextTap) {
        BigDecimal chargeAmount = travelPrices.get(new TravelPriceId(currentTap.getStopId(), nextTap.getStopId()));
        if (chargeAmount == null) {
            throw new IllegalArgumentException("Unknown travel cost between stops " + currentTap.getStopId() + ", " + nextTap.getStopId());
        }

        return new TripResult(
                currentTap.getDateTimeUTC(),
                nextTap.getDateTimeUTC(),
                nextTap.getDateTimeUTC().toEpochSecond() - currentTap.getDateTimeUTC().toEpochSecond(),
                currentTap.getStopId(),
                nextTap.getStopId(),
                chargeAmount,
                currentTap.getCompanyId(),
                currentTap.getBusId(),
                currentTap.getPan(),
                TripStatus.COMPLETED
        );
    }

    private TripResult calculateCostForIncompleteTrip(TapDetail currentTap) {
        TravelPrice maxCostForStop = getMaxCostForStop(currentTap.getStopId());

        StopId[] stops = convertStopsToArray(maxCostForStop.getTravelPriceId().getStops());
        StopId nextStop = currentTap.getStopId() == stops[0] ? stops[1] : stops[0];
        return new TripResult(
                currentTap.getDateTimeUTC(),
                currentTap.getDateTimeUTC(),
                0,
                currentTap.getStopId(),
                nextStop,
                maxCostForStop.getCost(),
                currentTap.getCompanyId(),
                currentTap.getBusId(),
                currentTap.getPan(),
                TripStatus.INCOMPLETE
        );
    }

    boolean isIncomplete(TapDetail currentTap, TapDetail nextTap) {
        return nextTap == null || (currentTap.getTapType() == TapType.ON && currentTap.getTapType() == nextTap.getTapType());
    }

    TravelPrice getMaxCostForStop(StopId stopId) {
        return travelPrices.entrySet().stream()
                .filter(travelPrice -> travelPrice.getKey().getStops().contains(stopId))
                .max(Map.Entry.comparingByValue())
                .map(entry -> new TravelPrice(entry.getKey(), entry.getValue()))
                .orElseThrow();
    }

    private static StopId[] convertStopsToArray(Set<StopId> stopSet) {
        StopId[] stops = new StopId[2];
        stopSet.toArray(stops);
        return stops;
    }

}
