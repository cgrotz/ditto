/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.services.gateway.streaming;

import java.time.Instant;
import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.ditto.services.gateway.streaming.actors.EventAndResponsePublisher;
import org.eclipse.ditto.services.gateway.streaming.actors.StreamingActor;

import akka.actor.ActorRef;

/**
 * Message to be sent in order to establish a new "streaming" connection via {@link StreamingActor}.
 */
public final class Connect {

    private final ActorRef eventAndResponsePublisher;
    private final String connectionCorrelationId;
    private final String type;
    @Nullable private final Instant sessionExpirationTime;

    /**
     * Constructs a new {@link Connect} instance.
     *
     * @param eventAndResponsePublisher the ActorRef to the correlating {@link EventAndResponsePublisher}.
     * @param connectionCorrelationId the correlationId of the connection/session.
     * @param type the type of the "streaming" connection to establish.
     */
    public Connect(final ActorRef eventAndResponsePublisher, final String connectionCorrelationId,
            final String type, @Nullable final Instant sessionExpirationTime) {
        this.eventAndResponsePublisher = eventAndResponsePublisher;
        this.connectionCorrelationId = connectionCorrelationId;
        this.type = type;
        this.sessionExpirationTime = sessionExpirationTime;
    }

    public ActorRef getEventAndResponsePublisher() {
        return eventAndResponsePublisher;
    }

    public String getConnectionCorrelationId() {
        return connectionCorrelationId;
    }

    public String getType() {
        return type;
    }

    @Nullable
    public Instant getSessionExpirationTime() { return sessionExpirationTime; }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Connect connect = (Connect) o;
        return Objects.equals(eventAndResponsePublisher, connect.eventAndResponsePublisher) &&
                Objects.equals(connectionCorrelationId, connect.connectionCorrelationId) &&
                Objects.equals(type, connect.type) &&
                Objects.equals(sessionExpirationTime, connect.sessionExpirationTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventAndResponsePublisher, connectionCorrelationId, type, sessionExpirationTime);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" +
                "eventAndResponsePublisher=" + eventAndResponsePublisher +
                ", connectionCorrelationId=" + connectionCorrelationId +
                ", type=" + type +
                ", sessionExpirationTime=" + sessionExpirationTime +
                "]";
    }
}
