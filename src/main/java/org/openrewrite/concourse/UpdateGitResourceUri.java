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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Incubating;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
@Incubating(since = "0.1.0")
public class UpdateGitResourceUri extends Recipe {
    @Option(displayName = "Optional old URI matcher",
            description = "The old URI value to replace. This can be a regex pattern. If left empty, replace all occurrences.",
            required = false,
            example = "https://github.com/openrewrite/rewrite")
    @Nullable
    String oldURIPattern;

    @Option(displayName = "New URI",
            description = "New URI value to replace the old URI value with.",
            example = "git@gitlab.com:openrewrite/rewrite.git")
    String newURI;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pipeline*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Update git resource `source.uri` references";
    }

    @Override
    public String getDescription() {
        return "Update git resource `source.uri` URI values to point to a new URI value.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public List<Recipe> getRecipeList() {
        return singletonList(new ChangeValue("$.resources[?(@.type == 'git')].source.uri",
                oldURIPattern, newURI, fileMatcher));
    }
}
