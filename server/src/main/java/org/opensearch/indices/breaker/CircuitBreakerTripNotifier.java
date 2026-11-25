/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.indices.breaker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.annotation.ExperimentalApi;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Node-local notifications of memory circuit breaker trips.
 *
 * Listeners run synchronously on the thread that trips the breaker. They must not block,
 * perform breaker-accounted operations, or rely on request context being available.
 *
 * @opensearch.experimental
 */
@ExperimentalApi
public final class CircuitBreakerTripNotifier {
    private static final Logger logger = LogManager.getLogger(CircuitBreakerTripNotifier.class);
    private final CopyOnWriteArrayList<Consumer<Trip>> listeners = new CopyOnWriteArrayList<>();

    /**
     * A single node-local breaker trip. The label is provided by the caller performing the allocation.
     */
    @ExperimentalApi
    public record Trip(String breaker, String label, long bytesWanted, long bytesLimit, String durability) {
    }

    public void addListener(Consumer<Trip> listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener));
    }

    public void removeListener(Consumer<Trip> listener) {
        listeners.remove(listener);
    }

    public void notifyTrip(String breaker, String label, long bytesWanted, long bytesLimit, String durability) {
        if (listeners.isEmpty()) {
            return;
        }
        Trip trip = new Trip(breaker, label, bytesWanted, bytesLimit, durability);
        for (Consumer<Trip> listener : listeners) {
            try {
                listener.accept(trip);
            } catch (RuntimeException e) {
                logger.warn("Circuit breaker trip listener failed", e);
            }
        }
    }
}
