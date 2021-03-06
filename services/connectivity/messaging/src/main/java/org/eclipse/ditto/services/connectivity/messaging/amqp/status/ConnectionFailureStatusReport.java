/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.services.connectivity.messaging.amqp.status;

import org.eclipse.ditto.services.connectivity.messaging.internal.ConnectionFailure;

/**
 * Reports a connections failure.
 */
public final class ConnectionFailureStatusReport {

    private final ConnectionFailure failure;

    private ConnectionFailureStatusReport(final ConnectionFailure failure) {
        this.failure = failure;
    }

    public static ConnectionFailureStatusReport get(final ConnectionFailure failure) {
        return new ConnectionFailureStatusReport(failure);
    }

    public ConnectionFailure getFailure() {
        return failure;
    }
}
