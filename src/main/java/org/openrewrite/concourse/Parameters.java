/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.concourse;

import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.tree.Yaml;

public class Parameters {
    private Parameters() {
    }

    public static boolean isParameter(Yaml yaml) {
        if (yaml instanceof Yaml.Scalar) {
            String scalar = ((Yaml.Scalar) yaml).getValue();
            return scalar.startsWith("((") && scalar.endsWith("))");
        }
        return false;
    }

    public static JsonPathMatcher toJsonPath(Yaml yaml) {
        if (!isParameter(yaml)) {
            throw new IllegalArgumentException("Yaml element is not a Concourse parameter");
        }

        String scalar = ((Yaml.Scalar) yaml).getValue();
        return new JsonPathMatcher("$." + scalar.substring(2, scalar.length() - 2));
    }
}
