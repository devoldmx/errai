/*
 * Copyright 2012 JBoss, by Red Hat, Inc
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

package org.jboss.errai.bus.server.util;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.jboss.errai.bus.client.api.messaging.Message;
import org.jboss.errai.bus.client.util.BusToolsCli;
import org.jboss.errai.bus.client.util.BusTools;

/**
 * @author Mike Brock
 */
public class ServerBusTools extends BusTools {
  public static ByteArrayInputStream encodeMessageToByteArrayInputStream(Message message) {
    try {
      return new ByteArrayInputStream(BusToolsCli.encodeMessage(message).getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new AssertionError("UTF-8 appears not to be supported by this JRE, but that's impossible");
    }
  }
}
