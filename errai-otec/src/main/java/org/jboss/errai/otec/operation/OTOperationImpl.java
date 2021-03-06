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

package org.jboss.errai.otec.operation;

import org.jboss.errai.otec.OTEngine;
import org.jboss.errai.otec.OTEntity;
import org.jboss.errai.otec.mutation.Mutation;

import java.util.Arrays;
import java.util.List;

/**
 * @author Christian Sadilek
 * @author Mike Brock
 */
public class OTOperationImpl implements OTOperation {
  private final OTEngine engine;
  private final List<Mutation> mutations;
  private final Integer entityId;
  private final Integer revision;
  private final boolean propagate;

  private OTOperationImpl(OTEngine engine, final List<Mutation> mutationList,
                          final Integer entityId,
                          final Integer revision,
                          final boolean propagate) {
    this.engine = engine;

    this.mutations = mutationList;
    this.entityId = entityId;
    this.revision = revision;
    this.propagate = propagate;
  }

  public static OTOperation createOperation(final OTEngine engine,
                                            final List<Mutation> mutationList,
                                            final Integer entityId,
                                            final Integer revision) {

    return new OTOperationImpl(engine, mutationList, entityId, revision, true);
  }

  public static OTOperation createLocalOnlyOperation(final OTEngine engine,
                                                     final List<Mutation> mutationList,
                                                     final Integer entityId,
                                                     final Integer revision) {

    return new OTOperationImpl(engine, mutationList, entityId, revision, false);
  }

  public static OTOperation createLocalOnlyOperation(final OTEngine engine, final OTOperation operation) {
    return new OTOperationImpl(engine, operation.getMutations(), operation.getEntityId(), operation.getRevision(), false);
  }


  @Override
  public List<Mutation> getMutations() {
    return mutations;
  }

  @Override
  public Integer getEntityId() {
    return entityId;
  }

  @Override
  public Integer getRevision() {
    return revision;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean apply(final OTEntity entity) {
    for (final Mutation mutation : mutations) {
      mutation.apply(entity.getState());
    }

    System.out.println("APPLY: " + toString() + "; on=" + engine);

    entity.incrementRevision();
    return shouldPropagate();
  }

  @Override
  public boolean shouldPropagate() {
    return propagate;
  }

  @Override
  public OTEngine getEngine() {
    return engine;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof OTOperationImpl)) return false;

    final OTOperationImpl that = (OTOperationImpl) o;

    return !(entityId != null ? !entityId.equals(that.entityId) : that.entityId != null)
        && !(mutations != null ? !mutations.equals(that.mutations) : that.mutations != null)
        && !(revision != null ? !revision.equals(that.revision) : that.revision != null);
  }

  @Override
  public int hashCode() {
    int result = mutations != null ? mutations.hashCode() : 0;
    result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
    result = 31 * result + (revision != null ? revision.hashCode() : 0);
    return result;
  }

  public String toString() {
    return Arrays.toString(mutations.toArray());
  }
}
