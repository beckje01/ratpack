/*
 * Copyright 2014 the original author or authors.
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

package ratpack.session.clientside.internal;

import com.google.common.collect.Iterables;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.Cookie;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.http.ResponseMetaData;
import ratpack.session.clientside.SessionService;
import ratpack.session.store.internal.ChangeTrackingSessionStorage;
import ratpack.stream.Streams;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class CookieBasedSessionStorageBindingHandler implements Handler {

  private final SessionService sessionService;

  private final String sessionName;

  public CookieBasedSessionStorageBindingHandler(SessionService sessionService, String sessionName) {
    this.sessionService = sessionService;
    this.sessionName = sessionName;
  }

  public void handle(final Context context) {
    context.getRequest().addLazy(ChangeTrackingSessionStorage.class, () -> {
      Cookie sessionCookie = Iterables.find(context.getRequest().getCookies(), c -> sessionName.equals(c.name()), null);
      ConcurrentMap<String, Object> sessionMap = sessionService.deserializeSession(sessionCookie);
      return new ChangeTrackingSessionStorage(sessionMap, context);
    });

    context.getResponse().beforeSend(responseMetaData -> {
      Optional<ChangeTrackingSessionStorage> storageOptional = context.getRequest().maybeGet(ChangeTrackingSessionStorage.class);
      if (storageOptional.isPresent()) {
        ChangeTrackingSessionStorage storage = storageOptional.get();
        if (storage.hasChanged()) {
          Set<Map.Entry<String, Object>> entries = new HashSet<Map.Entry<String, Object>>();

          storage.getKeys().then((keys) -> {
            context.stream(Streams.publish(keys))
              .flatMap((key) -> storage.get(key, Object.class).map((value) -> {
                if (value.isPresent()) {
                  return new AbstractMap.SimpleImmutableEntry<String, Object>(key, value.get());
                } else {
                  return null;
                }
              }))
              .toList().then((entryList) -> {
              for (Map.Entry<String, Object> entry : entryList) {
                if (entry != null) {
                  entries.add(entry);
                }
              }

              if (entries.isEmpty()) {
                invalidateSession(responseMetaData);
              } else {
                ByteBufAllocator bufferAllocator = context.get(ByteBufAllocator.class);
                String cookieValue = sessionService.serializeSession(bufferAllocator, entries);
                responseMetaData.cookie(sessionName, cookieValue);
              }
            });
          });
        }
      }
    });

    context.next();
  }

  private void invalidateSession(ResponseMetaData responseMetaData) {
    responseMetaData.expireCookie(sessionName);
  }

}
