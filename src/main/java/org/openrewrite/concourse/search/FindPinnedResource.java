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
package org.openrewrite.concourse.search;

import lombok.Getter;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.search.FindKey;

public class FindPinnedResource extends Recipe {
    @Option(displayName = "Resource type",
            description = "The resource type to search for. Leave empty to find all pins.",
            example = "git",
            required = false)
    @Nullable
    @Getter
    private final String resourceType;

    @Override
    public String getDisplayName() {
        return "Find pinned resources by type";
    }

    @Override
    public String getDescription() {
        return "Find resources of a particular type that have pinned versions.";
    }

    public FindPinnedResource(@Nullable String resourceType) {
        this.resourceType = resourceType;
        String search = "$.resources[" +
                (resourceType == null ? "*" : "?(@.type == '" + resourceType + "')") +
                "].version";
        doNext(new FindKey(search));
    }
}
