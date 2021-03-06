/*
 * Copyright (c) 2016 Ericsson Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.sfc.genius.impl.listeners;

import java.util.concurrent.ExecutorService;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.serviceutils.tools.mdsal.listener.AbstractAsyncDataTreeChangeListener;
import org.opendaylight.sfc.genius.impl.SfcGeniusServiceManager;
import org.opendaylight.sfc.genius.util.SfcGeniusDataUtils;
import org.opendaylight.sfc.genius.util.SfcGeniusRuntimeException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.InterfacesState;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.interfaces.rev140508.interfaces.state.Interface;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Listener for data store changes of interface state data tree. sfc-genius
 * needs to be aware interface state changes that signal a migration of a
 * logical interface from one node/port to another.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7223">
 *     [RFC7223] A YANG Data Model for Interface Management</a>
 * @see "org.opendaylight.genius.interfacemanager"
 */
public class SfcGeniusInterfaceStateListener extends AbstractAsyncDataTreeChangeListener<Interface> {

    private static final Logger LOG = LoggerFactory.getLogger(SfcGeniusInterfaceStateListener.class);
    private final SfcGeniusServiceManager interfaceManger;

    public SfcGeniusInterfaceStateListener(DataBroker dataBroker,
                                           SfcGeniusServiceManager interfaceManager,
                                           ExecutorService executorService) {
        super(dataBroker, LogicalDatastoreType.OPERATIONAL, getWildCardPath(), executorService);
        this.interfaceManger = interfaceManager;
    }

    private static InstanceIdentifier<Interface> getWildCardPath() {
        return InstanceIdentifier.create(InterfacesState.class).child(Interface.class);
    }

    @Override
    public void add(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface newInterface) {
        // VM migration: logical interface state is added once the VM has migrated
        // See org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener#remove
        LOG.debug("Received interface state add event {}", newInterface);
        String interfaceName = newInterface.getName();
        Uint64 dpnId;
        try {
            dpnId = SfcGeniusDataUtils.getDpnIdFromLowerLayerIfList(newInterface.getLowerLayerIf());
        } catch (SfcGeniusRuntimeException e) {
            LOG.debug("Event ignored, could not get underlying dpn id", e);
            return;
        }
        interfaceManger.interfaceStateUp(interfaceName, dpnId);
    }

    @Override
    public void remove(@NonNull InstanceIdentifier<Interface> instanceIdentifier, @NonNull Interface removedInterface) {
        // VM migration: logical interface state is removed while VM migrates to different node/port
        // See org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener#remove
        // This is a NOP, we wait until until the VM has migrated once it's interface registers again
    }

    @Override
    public void update(@NonNull InstanceIdentifier<Interface> instanceIdentifier,
                       @NonNull Interface originalInterface,
                       @NonNull Interface updatedInterface) {
        // NOT VM migration: VM unavailable for any other reason
        // See org.opendaylight.genius.interfacemanager.listeners.InterfaceInventoryStateListener#update
        // Do nothing, should be handled by a failover mechanism
    }
}
