package cz.cvut.fit.acb.harness;

import cz.cvut.fit.acb.TripletCoding;

import java.util.Map;

/**
 * The slowest each coder may be, in megabytes per second over the harness's generated inputs, at
 * the harness's settings. Set to about 60% of what the development machine measured, so noise does
 * not trip it, and raised whenever a phase makes the coders faster.
 */
record PerformanceBudget(Map<TripletCoding, Floor> floors) {

    record Floor(double compress, double decompress) {
    }

    PerformanceBudget {
        floors = Map.copyOf(floors);
    }

    /** No floor for any coder: everything passes. */
    static PerformanceBudget none() {
        return new PerformanceBudget(Map.of());
    }

    static PerformanceBudget current() {
        return new PerformanceBudget(Map.of(
                TripletCoding.SIMPLE, new Floor(0.75, 1.0),
                TripletCoding.SALOMON, new Floor(0.7, 1.0),
                TripletCoding.SALOMON2, new Floor(0.8, 1.1),
                TripletCoding.VALACH, new Floor(0.8, 1.1)));
    }

    Floor floorFor(TripletCoding coder) {
        return this.floors.getOrDefault(coder, new Floor(0, 0));
    }
}
