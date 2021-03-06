/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.tomakehurst.wiremock.http;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

@JsonSerialize(using = HttpHeadersJsonSerializer.class)
@JsonDeserialize(using = HttpHeadersJsonDeserializer.class)
public class HttpHeaders {

    private final Multimap<CaseInsensitiveKey, String> headers;

    public HttpHeaders() {
        headers = ImmutableMultimap.of();
    }

    public HttpHeaders(HttpHeader... headers) {
        this(ImmutableList.copyOf(headers));
    }

    public HttpHeaders(Iterable<HttpHeader> headers) {
        ImmutableMultimap.Builder<CaseInsensitiveKey, String> builder = ImmutableMultimap.builder();
        for (HttpHeader header: headers) {
            builder.putAll(caseInsensitive(header.key()), header.values());
        }

        this.headers = builder.build();
    }

    public HttpHeaders(HttpHeaders headers) {
        this(headers.all());
    }

    public static HttpHeaders noHeaders() {
        return new HttpHeaders();
    }

    public HttpHeaders without(String key) {
      ArrayList<HttpHeader> httpHeaders = new ArrayList<HttpHeader>();
      for(Map.Entry<CaseInsensitiveKey, String> x : headers.entries()) {
        if (x.getKey().key.equals(key)) continue;
        httpHeaders.add(new HttpHeader(x.getKey().key, x.getValue()));
      }
      return new HttpHeaders(httpHeaders);
    }

    public HttpHeader getHeader(String key) {
        if (!headers.containsKey(caseInsensitive(key))) {
            return HttpHeader.absent(key);
        }

        Collection<String> values = headers.get(caseInsensitive(key));
        return new HttpHeader(key, values);
    }

    public ContentTypeHeader getContentTypeHeader() {
        HttpHeader header = getHeader(ContentTypeHeader.KEY);
        if (header.isPresent()) {
            return new ContentTypeHeader(getHeader(ContentTypeHeader.KEY).firstValue());
        }

        return ContentTypeHeader.absent();
    }

    public Collection<HttpHeader> all() {
        List<HttpHeader> httpHeaderList = newArrayList();
        for (CaseInsensitiveKey key: headers.keySet()) {
            httpHeaderList.add(new HttpHeader(key.key, headers.get(key)));
        }

        return httpHeaderList;
    }

    public Set<String> keys() {
        return newHashSet(transform(headers.keySet(), new Function<CaseInsensitiveKey, String>() {
            public String apply(CaseInsensitiveKey input) {
                return input.key;
            }
        }));
    }

    public static HttpHeaders copyOf(HttpHeaders source) {
        return new HttpHeaders(source);
    }

    public int size() {
        return headers.asMap().size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HttpHeaders that = (HttpHeaders) o;

//        if (headers != null ? !headers.equals(that.headers) : that.headers != null) return false;
        if (headers != null ?
            !ImmutableSetMultimap.copyOf(headers).equals(ImmutableSetMultimap.copyOf(that.headers))
            : that.headers != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        return result;
    }

    private CaseInsensitiveKey caseInsensitive(String key) {
        return new CaseInsensitiveKey(key);
    }

    private static class CaseInsensitiveKey {
        final String key;

        CaseInsensitiveKey(String key) {
            this.key = key;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CaseInsensitiveKey that = (CaseInsensitiveKey) o;

            if (key != null ? !key.toLowerCase().equals(that.key.toLowerCase()) : that.key != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return key != null ? key.toLowerCase().hashCode() : 0;
        }
    }
}
