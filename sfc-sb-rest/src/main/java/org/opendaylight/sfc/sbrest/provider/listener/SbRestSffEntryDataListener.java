/*
 * Copyright (c) 2014, 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sfc.sbrest.provider.listener;

import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractSyncDataTreeChangeListener;
import org.opendaylight.sfc.provider.api.SfcInstanceIdentifiers;
import org.opendaylight.sfc.sbrest.provider.task.RestOperation;
import org.opendaylight.sfc.sbrest.provider.task.SbRestSffTask;
import org.opendaylight.yang.gen.v1.urn.cisco.params.xml.ns.yang.sfc.sff.rev140701.service.function.forwarders.ServiceFunctionForwarder;

import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SbRestSffEntryDataListener extends AbstractSyncDataTreeChangeListener<ServiceFunctionForwarder> {

    private static final Logger LOG = LoggerFactory.getLogger(SbRestSffEntryDataListener.class);

    private final ExecutorService executorService;

    @Inject
    public SbRestSffEntryDataListener(DataBroker dataBroker, ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.CONFIGURATION, SfcInstanceIdentifiers.SFF_ENTRY_IID);
        this.executorService = executorService;
    }

    @Override
    public void add(@NonNull InstanceIdentifier<ServiceFunctionForwarder> instanceIdentifier,
                    @NonNull ServiceFunctionForwarder serviceFunctionForwarder) {
        update(instanceIdentifier, serviceFunctionForwarder, serviceFunctionForwarder);
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<ServiceFunctionForwarder> instanceIdentifier,
                       @NonNull ServiceFunctionForwarder serviceFunctionForwarder) {
        LOG.debug("Deleted Service Function Forwarder Name: {}", serviceFunctionForwarder.getName());
        new SbRestSffTask(RestOperation.DELETE, serviceFunctionForwarder, executorService).run();
    }

    @Override
    public void update(@NonNull InstanceIdentifier<ServiceFunctionForwarder> instanceIdentifier,
                       @NonNull ServiceFunctionForwarder originalServiceFunctionForwarder,
                       @NonNull ServiceFunctionForwarder updatedServiceFunctionForwarder) {
        LOG.debug("Updated Service Function Forwarder Name: {}", updatedServiceFunctionForwarder.getName());
        new SbRestSffTask(RestOperation.PUT, updatedServiceFunctionForwarder, executorService).run();
    }
}
