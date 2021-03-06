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
package org.eclipse.ditto.services.thingsearch.persistence.write.streaming;

import java.time.Duration;

import javax.annotation.Nullable;

import org.eclipse.ditto.json.JsonObject;
import org.eclipse.ditto.model.base.json.FieldType;
import org.eclipse.ditto.model.enforcers.AclEnforcer;
import org.eclipse.ditto.model.enforcers.Enforcer;
import org.eclipse.ditto.model.policies.PolicyId;
import org.eclipse.ditto.model.things.AccessControlList;
import org.eclipse.ditto.model.things.Thing;
import org.eclipse.ditto.model.things.ThingId;
import org.eclipse.ditto.model.things.ThingRevision;
import org.eclipse.ditto.services.thingsearch.persistence.write.mapping.EnforcedThingMapper;
import org.eclipse.ditto.services.thingsearch.persistence.write.model.AbstractWriteModel;
import org.eclipse.ditto.services.thingsearch.persistence.write.model.Metadata;
import org.eclipse.ditto.services.thingsearch.persistence.write.model.ThingDeleteModel;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.reactivestreams.client.MongoDatabase;

import akka.NotUsed;
import akka.stream.javadsl.Source;

/**
 * Run parts of the updater stream for unit tests.
 */
public final class TestSearchUpdaterStream {

    private final MongoSearchUpdaterFlow mongoSearchUpdaterFlow;

    private TestSearchUpdaterStream(final MongoSearchUpdaterFlow mongoSearchUpdaterFlow) {
        this.mongoSearchUpdaterFlow = mongoSearchUpdaterFlow;
    }

    /**
     * Create a test stream.
     *
     * @param database the MongoDB database.
     * @return the test stream.
     */
    public static TestSearchUpdaterStream of(final MongoDatabase database) {
        final MongoSearchUpdaterFlow mongoSearchUpdaterFlow = MongoSearchUpdaterFlow.of(database);
        return new TestSearchUpdaterStream(mongoSearchUpdaterFlow);
    }

    /**
     * Write a thing into the updater stream.
     *
     * @param thing the thing
     * @param enforcer the enforcer
     * @param policyRevision the policy revision
     * @return source of write result.
     */
    public Source<BulkWriteResult, NotUsed> write(final Thing thing,
            final Enforcer enforcer,
            final long policyRevision) {

        final JsonObject thingJson = thing.toJson(FieldType.all());
        final AbstractWriteModel writeModel = EnforcedThingMapper.toWriteModel(thingJson, enforcer, policyRevision, -1);

        return Source.single(Source.single(writeModel))
                .via(mongoSearchUpdaterFlow.start(1, 1, Duration.ZERO));
    }

    /**
     * Write a thing with ACL into the updater stream.
     *
     * @param thingWithAcl the thing with ACL.
     * @return source of write result.
     */
    public Source<BulkWriteResult, NotUsed> writeThingWithAcl(final Thing thingWithAcl) {
        final AccessControlList emptyAcl = AccessControlList.newBuilder().build();
        final Enforcer enforcer = AclEnforcer.of(thingWithAcl.getAccessControlList().orElse(emptyAcl));
        final long policyRevision = thingWithAcl.getRevision().orElse(ThingRevision.newInstance(-1L)).toLong();
        return write(thingWithAcl, enforcer, policyRevision);
    }

    /**
     * Deletes a thing from the updater stream.
     * @param thingId the thing id.
     * @param revision the revision.
     * @param policyId the policy id.
     * @param policyRevision the policy revision.
     * @return the write result.
     */
    public Source<BulkWriteResult, NotUsed> delete(final ThingId thingId, final long revision, @Nullable final PolicyId policyId, final long policyRevision) {
        return delete(Metadata.of(thingId, revision, policyId != null ? policyId.toString() : null, policyRevision));
    }

    /**
     * Delete a thing from the updater stream.
     *
     * @param metadata the metadata.
     * @return source of write result.
     */
    private Source<BulkWriteResult, NotUsed> delete(final Metadata metadata) {
        final AbstractWriteModel writeModel = ThingDeleteModel.of(metadata);
        return Source.single(Source.single(writeModel))
                .via(mongoSearchUpdaterFlow.start(1, 1, Duration.ZERO));
    }

}
