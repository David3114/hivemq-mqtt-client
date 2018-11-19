/*
 * Copyright 2018 The MQTT Bee project
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
 *
 */

package org.mqttbee.annotations;

import java.lang.annotation.*;

/**
 * Documents that an interface MUST NOT be implemented by the user.
 * <p>
 * The implementation is provided by the library.
 * <p>
 * This allows the library to later add methods to the interface without breaking backwards compatibility with
 * implementing classes.
 *
 * @author Silvio Giebl
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface DoNotImplement {}
