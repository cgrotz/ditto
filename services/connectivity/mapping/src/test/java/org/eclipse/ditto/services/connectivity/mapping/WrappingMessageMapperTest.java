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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.connectivity.MessageMappingFailedException;
import org.eclipse.ditto.protocoladapter.Adaptable;
import org.eclipse.ditto.protocoladapter.ProtocolFactory;
import org.eclipse.ditto.services.models.connectivity.ExternalMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.internal.verification.VerificationModeFactory;

import com.typesafe.config.ConfigFactory;

/**
 * Tests for {@link WrappingMessageMapper}.
 */
public class WrappingMessageMapperTest {

    private MessageMapper mockMapper;
    private MessageMapper underTest;
    private MessageMapperConfiguration mockConfiguration;
    private ExternalMessage mockMessage;
    private Adaptable mockAdaptable;

    private MappingConfig mapperLimitsConfig;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        mockMapper = mock(MessageMapper.class);
        mockConfiguration = mock(MessageMapperConfiguration.class);
        mockMessage = mock(ExternalMessage.class);
        mockAdaptable = mock(Adaptable.class);

        when(mockMapper.map(any(ExternalMessage.class))).thenReturn(singletonList(mockAdaptable));
        when(mockMapper.map(mockAdaptable)).thenReturn(singletonList(mockMessage));
        when(mockMapper.getId()).thenReturn("mockMapper");
        when(mockAdaptable.getTopicPath()).thenReturn(ProtocolFactory.emptyTopicPath());
        when(mockAdaptable.getHeaders()).thenReturn(Optional.of(DittoHeaders.empty()));
        when(mockAdaptable.getPayload()).thenReturn(ProtocolFactory.newPayload("{\"path\":\"/\"}"));
        mapperLimitsConfig = DefaultMappingConfig.of(ConfigFactory.load("mapper-limits-test"));
        underTest = WrappingMessageMapper.wrap(mockMapper);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void configure() {
        when(mockConfiguration.getContentTypeBlacklist()).thenReturn(Collections.singletonList("blacklistedContentType"));

        underTest.configure(mapperLimitsConfig, mockConfiguration);
        verify(mockMapper).configure(mapperLimitsConfig, mockConfiguration);
    }

    @Test
    public void mapMessage() {
        underTest.configure(mapperLimitsConfig, mockConfiguration);
        final List<Adaptable> adaptables = underTest.map(mockMessage);
        verify(mockMapper).map(any(ExternalMessage.class));
        assertThat(adaptables).allSatisfy(a -> assertThat(a.getDittoHeaders().getMapper()).contains("mockMapper"));
    }

    @Test
    public void mapAdaptable() {
        final DittoHeaders headers = DittoHeaders.of(Collections.singletonMap(ExternalMessage.CONTENT_TYPE_HEADER, "contentType"));
        when(mockAdaptable.getHeaders()).thenReturn(Optional.of(headers));

        underTest.configure(mapperLimitsConfig, mockConfiguration);
        underTest.map(mockAdaptable);
        verify(mockAdaptable, VerificationModeFactory.atLeastOnce()).getHeaders();
        verify(mockMapper).map(mockAdaptable);
    }

    @Test
    public void mapMessageWithInvalidNumberOfMessages() {
        exception.expect(MessageMappingFailedException.class);
        List<Adaptable> listOfMockAdaptable = listWithInvalideNumberOfElements(mockAdaptable,
                mapperLimitsConfig.getMapperLimitsConfig().getMaxMappedInboundMessages());
        when(mockMapper.map(any(ExternalMessage.class))).thenReturn(listOfMockAdaptable);

        underTest.configure(mapperLimitsConfig, mockConfiguration);
        underTest.map(mockMessage);
        verify(mockMapper).map(any(ExternalMessage.class));
    }

    @Test
    public void mapAdaptableWithInvalidNumberOfMessages() {
        exception.expect(MessageMappingFailedException.class);
        List<ExternalMessage> listOfMockAdaptable =
                listWithInvalideNumberOfElements(mockMessage,
                        mapperLimitsConfig.getMapperLimitsConfig().getMaxMappedOutboundMessages());
        when(mockMapper.map(any(Adaptable.class))).thenReturn(listOfMockAdaptable);

        underTest.configure(mapperLimitsConfig, mockConfiguration);
        underTest.map(mockAdaptable);
        verify(mockAdaptable, VerificationModeFactory.atLeastOnce()).getHeaders();
        verify(mockMapper).map(mockAdaptable);
    }

    private <T> List<T> listWithInvalideNumberOfElements(T elementInList, final int invalidLimitNumber) {
        List<T> listOfMockAdaptable = new ArrayList<>();
        for (int i = 0; i < invalidLimitNumber + 1; i++) {
            listOfMockAdaptable.add(elementInList);
        }
        return listOfMockAdaptable;
    }
}
