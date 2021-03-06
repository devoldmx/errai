/*
 * Copyright 2010 JBoss, a divison Red Hat, Inc
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

package org.errai.samples.queryservice.server;

import org.errai.samples.queryservice.client.shared.QueryServiceRemote;
import org.jboss.errai.bus.server.annotations.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class QueryService implements QueryServiceRemote {
  private Map<String, String[]> dataMap;

  public QueryService() {
    setupMap();
  }

  private void setupMap() {
    dataMap = new HashMap<String, String[]>();
    dataMap.put("beer", new String[]{"Heineken", "Budweiser", "Hoogaarden"});
    dataMap.put("fruit", new String[]{"Apples", "Oranges", "Grapes"});
    dataMap.put("animals", new String[]{"Monkeys", "Giraffes", "Lions"});
  }

  public String[] query(String queryString) {
    return dataMap.get(queryString);
  }

  public String append(String... arg) {
    StringBuilder builder = new StringBuilder();
    for (String s : arg) {
      builder.append(s);
    }
    return builder.toString();
  }
}

