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
package org.eclipse.ditto.services.connectivity.mapping;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.ditto.model.base.headers.DittoHeaderDefinition;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.headers.DittoHeadersBuilder;
import org.eclipse.ditto.model.connectivity.MessageMappingFailedException;
import org.eclipse.ditto.protocoladapter.Adaptable;
import org.eclipse.ditto.protocoladapter.ProtocolFactory;
import org.eclipse.ditto.services.models.connectivity.ExternalMessage;
import org.eclipse.ditto.services.models.connectivity.ExternalMessageBuilder;
import org.eclipse.ditto.services.models.connectivity.ExternalMessageFactory;

/**
 * Does wrap any {@link MessageMapper}.
 * <p>
 * adds headers to ExternalMessage and Adaptable in mappings even when the wrapped {@link MessageMapper} does
 * forget to do so by himself.
 * </p>
 */
final class WrappingMessageMapper implements MessageMapper {

    private int limitInboundMessages;
    private int limitOutboundMessages;

    private final MessageMapper delegate;

    private WrappingMessageMapper(final MessageMapper delegate) {
        this.delegate = checkNotNull(delegate);
    }

    /**
     * Enforces content type checking for the mapper
     *
     * @param mapper the mapper
     * @return the wrapped mapper
     */
    public static MessageMapper wrap(final MessageMapper mapper) {
        return new WrappingMessageMapper(mapper);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public Collection<String> getContentTypeBlacklist() {
        return delegate.getContentTypeBlacklist();
    }

    /**
     * @return the MessageMapper delegate this instance wraps.
     */
    public MessageMapper getDelegate() {
        return delegate;
    }

    @Override
    public void configure(final MappingConfig mappingConfig, final MessageMapperConfiguration configuration) {
        final MapperLimitsConfig mapperLimitsConfig = mappingConfig.getMapperLimitsConfig();
        limitInboundMessages = mapperLimitsConfig.getMaxMappedInboundMessages();
        limitOutboundMessages = mapperLimitsConfig.getMaxMappedOutboundMessages();
        delegate.configure(mappingConfig, configuration);
    }

    @Override
    public List<Adaptable> map(final ExternalMessage message) {
        final ExternalMessage enhancedMessage;
        final String correlationId;
        if (!message.getHeaders().containsKey(DittoHeaderDefinition.CORRELATION_ID.getKey())) {
            // if no correlation-id was provided in the ExternalMessage, generate one here:
            correlationId = UUID.randomUUID().toString();
            enhancedMessage = ExternalMessageFactory.newExternalMessageBuilder(message)
                    .withAdditionalHeaders(DittoHeaderDefinition.CORRELATION_ID.getKey(), correlationId)
                    .build();
        } else {
            correlationId = message.getHeaders().get(DittoHeaderDefinition.CORRELATION_ID.getKey());
            enhancedMessage = message;
        }

        final List<Adaptable> mappedAdaptables = delegate.map(enhancedMessage);

        checkMessagesMappingNumber(mappedAdaptables, limitInboundMessages);

        return mappedAdaptables.stream().map(mapped -> {
            final DittoHeadersBuilder headersBuilder = DittoHeaders.newBuilder();
            headersBuilder.correlationId(correlationId);

            Optional.ofNullable(message.getHeaders().get(ExternalMessage.REPLY_TO_HEADER)).ifPresent(replyTo ->
                    headersBuilder.putHeader(ExternalMessage.REPLY_TO_HEADER, replyTo)
            );

            final Optional<DittoHeaders> headersOpt = mapped.getHeaders();
            headersOpt.ifPresent(headersBuilder::putHeaders); // overwrite with mapped headers (if any)

            return ProtocolFactory.newAdaptableBuilder(mapped)
                    .withHeaders(headersBuilder.build())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<ExternalMessage> map(final Adaptable adaptable) {
        final List<ExternalMessage> mappedMessages = delegate.map(adaptable);
        checkMessagesMappingNumber(mappedMessages, limitOutboundMessages);
        return mappedMessages.stream().map(mapped -> {
            final ExternalMessageBuilder messageBuilder = ExternalMessageFactory.newExternalMessageBuilder(mapped);
            messageBuilder.asResponse(adaptable.getPayload().getStatus().isPresent());
            adaptable.getHeaders()
                    .map(h -> h.get(ExternalMessage.REPLY_TO_HEADER))
                    .ifPresent(
                            replyTo -> messageBuilder.withAdditionalHeaders(ExternalMessage.REPLY_TO_HEADER, replyTo));
            return messageBuilder.build();
        }).collect(Collectors.toList());
    }

    private void checkMessagesMappingNumber(final List<?> mappedAdaptables,
            final int messageMappingNumberLimit) {
        if (mappedAdaptables.size() > messageMappingNumberLimit && messageMappingNumberLimit != 0) {
            throw MessageMappingFailedException.newBuilder((String) null)
                    .message("Number of messages from mapping exceeded")
                    .description(
                            "Check invoked messages produced by your payload mapping. Maximum number of invoked messages are "
                                    + messageMappingNumberLimit
                                    + ". Processed mapping invoked "
                                    + mappedAdaptables.size()
                                    + " messages")
                    .build();
        } else if (messageMappingNumberLimit == 0) {
            throw MessageMappingFailedException.newBuilder((String) null)
                    .message("According to configuration no messages can be sent")
                    .description("Check configuration settings to enable sending of messages")
                    .build();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final WrappingMessageMapper that = (WrappingMessageMapper) o;
        return Objects.equals(delegate, that.delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" +
                "delegate=" + delegate +
                "]";
    }

}
