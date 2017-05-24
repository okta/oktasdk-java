/*
 * Copyright 2014 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.sdk.impl.resource;

import com.okta.sdk.impl.query.Operator;
import com.okta.sdk.impl.query.SimpleExpression;

/**
 * @since 0.5.0
 */
public abstract class NonStringProperty<T> extends Property<T> {

    protected NonStringProperty(String name, Class<T> type) {
        super(name, type);
    }

    /**
     * Returns a new equals expression reflecting the property name and the specified value.
     *
     * @param value the value that should equal the property value.
     * @return a new equals expression reflecting the property name and the specified value.
     */
    public SimpleExpression eq(Object value) {
        return new SimpleExpression(this.name, value, Operator.EQUALS);
    }
}
