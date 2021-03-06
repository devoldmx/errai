/*
* Copyright 2013 JBoss, by Red Hat, Inc
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.jboss.errai.otec;

import org.jboss.errai.otec.mutation.CharacterData;
import org.jboss.errai.otec.mutation.Data;
import org.jboss.errai.otec.mutation.IndexPosition;
import org.jboss.errai.otec.mutation.Mutation;
import org.jboss.errai.otec.mutation.MutationType;
import org.jboss.errai.otec.mutation.StringMutation;
import org.jboss.errai.otec.operation.OTOperation;
import org.jboss.errai.otec.operation.OTOperationImpl;
import org.jboss.errai.otec.operation.OTOperationsFactory;
import org.jboss.errai.otec.operation.OTOperationsListBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Christian Sadilek <csadilek@redhat.com>
 * @author Mike Brock
 */
public class OTEngineImpl implements OTEngine {
  private final String engineId;

  private final PeerState peerState;
  private final OTEntityState entityState = new OTEntityStateImpl();
  private OTEngineMode mode = OTEngineMode.Online;
  private String name;

  private OTEngineImpl(final PeerState peerState, final String name) {
    engineId = GUIDUtil.createGUID();
    if (name == null) {
      this.name = engineId;
    }
    else {
      this.name = name;
    }

    this.peerState = peerState;
  }

  @SuppressWarnings("UnusedDeclaration")
  public static OTEngine createEngineWithSinglePeer() {
    return createEngineWithSinglePeer(null);
  }

  @SuppressWarnings("UnusedDeclaration")
  public static OTEngine createEngineWithMultiplePeers() {
    return createEngineWithMultiplePeers(null);
  }

  public static OTEngine createEngineWithSinglePeer(final String name) {
    return new OTEngineImpl(new SinglePeerState(), name);
  }

  public static OTEngine createEngineWithMultiplePeers(final String name) {
    return new OTEngineImpl(new MultiplePeerState(), name);
  }


  @Override
  public ReceiveHandler getReceiveHandler(final String peerId, final Integer entityId) {
    final OTEntity entity = entityState.getEntity(entityId);

    if (entity == null) {
      throw new OTException("could not find entity for reference: " + entityId);
    }

    final OTPeer peer = peerState.getPeer(peerId);

    return new ReceiveHandler() {
      @Override
      public void receive(final OTOperation operation) {
        final List<OTOperation> transformedOps;

        if (peerState.hasConflictResolutionPrecedence()) {
          transformedOps = Transformer.createTransformerLocalPrecedence(OTEngineImpl.this, entity, peer, operation).transform();
        }
        else {
          transformedOps = Transformer.createTransformerRemotePrecedence(OTEngineImpl.this, entity, peer, operation).transform();
        }

        // broadcast to all other peers subscribed to this entity
        final Set<OTPeer> peers = getPeerState().getPeersFor(entity);
        for (final OTPeer otPeer : peers) {
          for (OTOperation op : transformedOps) {
            if (otPeer != peer) {
              otPeer.send(entityId, op);
            }
//            else {
//              System.out.println(getEngineName() + " DID NOT PROPAGATE:" + op + "; to peer: " + otPeer);
//            }
          }
        }
      }
    };
  }

  @Override
  public InitialStateReceiveHandler getInitialStateReceiveHandler(final String peerId, final Integer entityId) {
    final OTPeer peer = getPeerState().getPeer(peerId);
    assertPeerNotNull(peer);

    return new InitialStateReceiveHandler() {
      @SuppressWarnings("unchecked")
      @Override
      public void receive(final State obj) {
        final OTEntity newEntity = new OTEntityImpl(entityId, obj);
        entityState.addEntity(newEntity);
        getPeerState().associateEntity(peer, newEntity);
      }
    };
  }

  @SuppressWarnings("unchecked")
  @Override
  public void syncRemoteEntity(final String peerId, final Integer entityId, final EntitySyncCompletionCallback callback) {
    final OTPeer peer = getPeerState().getPeer(peerId);
    assertPeerNotNull(peer);

    peer.beginSyncRemoteEntity(peerId, entityId, callback);
  }

  @Override
  public void notifyOperation(final OTOperation operation) {
    final OTEntity entity = getEntityStateSpace().getEntity(operation.getEntityId());
    final boolean propagate = operation.apply(entity);
    // System.out.println("APPLY " + operation + "; on=" + getEngineName());
    entity.getTransactionLog().appendLog(operation);

    if (propagate && mode == OTEngineMode.Online) {
      final Collection<OTPeer> peersFor = getPeerState().getPeersFor(entity);
      for (final OTPeer peer : peersFor) {
        peer.send(entity.getId(), operation);
      }
    }
  }

  @Override
  public OTOperationsFactory getOperationsFactory() {
    return new OTOperationsFactory() {
      @Override
      public OTOperationsListBuilder createOperation(final OTEntity entity) {
        return new OTOperationsListBuilder() {
          List<Mutation> operationList = new ArrayList<Mutation>();

          @Override
          public OTOperationsListBuilder add(final MutationType type, final Position position, final Data data) {
            operationList.add(
                new StringMutation(type, (IndexPosition) position, (CharacterData) data)
            );
            return this;
          }

          @Override
          public OTOperation build() {
            return OTOperationImpl.createOperation(OTEngineImpl.this, operationList, entity.getId(), entity.getRevision());
          }

          @Override
          public OTOperationsListBuilder add(final MutationType type, final Position position) {
            operationList.add(new StringMutation(type, (IndexPosition) position, null));
            return this;
          }
        };
      }
    };
  }

  private static void assertPeerNotNull(final OTPeer peer) {
    if (peer == null) {
      throw new OTException("could not find peer for id: " + peer);
    }
  }

  @Override
  public OTEntityState getEntityStateSpace() {
    return entityState;
  }

  private PeerState getPeerState() {
    return peerState;
  }

  @Override
  public void associateEntity(final String peerId, final Integer entityId) {
    final OTPeer peer = getPeerState().getPeer(peerId);
    if (peer == null) {
      throw new OTException("no peer for id: " + peerId);
    }

    final OTEntity entity = getEntityStateSpace().getEntity(entityId);
    if (entity == null) {
      throw new OTException("no entity for id: " + entityId);
    }

    getPeerState().associateEntity(peer, entity);
  }

  @Override
  public void disassociateEntity(final String peerId, final Integer entityId) {
    final OTPeer peer = getPeerState().getPeer(peerId);
    if (peer == null) {
      throw new OTException("no peer for id: " + peerId);
    }

    final OTEntity entity = getEntityStateSpace().getEntity(entityId);
    if (entity == null) {
      throw new OTException("not entity for id: " + entityId);
    }


    getPeerState().disassociateEntity(peer, entity);
  }

  @Override
  public void registerPeer(final OTPeer peer) {
    getPeerState().registerPeer(peer);
  }

  @Override
  public void setEngineMode(final OTEngineMode mode) {
    if (this.mode == OTEngineMode.Offline && mode == OTEngineMode.Online) {
      transmitDeferredTransactions();
    }
    this.mode = mode;
  }

  private void transmitDeferredTransactions() {
    final Map<OTEntity, Set<OTPeer>> entityPeerRelationshipMap = getPeerState().getEntityPeerRelationshipMap();
    for (final Map.Entry<OTEntity, Set<OTPeer>> entry : entityPeerRelationshipMap.entrySet()) {

      for (final OTPeer peer : entry.getValue()) {
        final Collection<OTOperation> log
            = entry.getKey().getTransactionLog().getLogFromId(peer.getLastTransmittedSequence(entry.getKey()));

        for (final OTOperation op : log) {
          if (getPeerState().shouldForwardOperation(op)) {
            peer.send(entry.getKey().getId(), op);
          }
        }
      }
    }
  }

  @Override
  public String getId() {
    return engineId;
  }

  public String toString() {
    return getEngineName();
  }

  @Override
  public String getEngineName() {
    return name;
  }
}
