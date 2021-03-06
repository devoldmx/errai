package org.jboss.errai.aerogear.api.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.json.client.JSONObject;
import org.jboss.errai.enterprise.client.jaxrs.MarshallingWrapper;
import org.jboss.errai.marshalling.client.api.MarshallerFramework;

import java.util.ArrayList;
import java.util.List;

/**
 * @author edewit@redhat.com
 */
public abstract class AbstractAdapter<T> {
  static {
    MarshallerFramework.initializeDefaultSessionProvider();
    enableJacksonMarchalling();
  }

  private native static void enableJacksonMarchalling() /*-{
      $wnd.erraiJaxRsJacksonMarshallingActive = true;
  }-*/;

  protected JavaScriptObject object;
  private final Class<T> type;

  protected AbstractAdapter(Class<T> type) {
    this.type = type;
  }

  protected String toJSON(JavaScriptObject object) {
    return new JSONObject(object).toString();
  }

  protected T fromJSON(String json) {
    return MarshallingWrapper.fromJSON(json, type);
  }

  protected T convertToType(JavaScriptObject object) {
    return fromJSON(toJSON(object));
  }

  protected List<T> convertToType(JsArray jsArray) {
    List<T> result = new ArrayList<T>(jsArray.length());

    for (int i = 0; i < jsArray.length(); i++) {
      result.add(convertToType(jsArray.get(i)));
    }
    return result;
  }
}
