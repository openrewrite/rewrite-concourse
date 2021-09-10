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
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Value
@EqualsAndHashCode(callSuper = true)
@Incubating(since = "0.1.0")
public class UpdateGitResourceURI extends Recipe {
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
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test(
                "oldURIPattern",
                "oldURIPattern must be a compilable regular expression",
                oldURIPattern, ps -> {
                    if (oldURIPattern != null) {
                        try {
                            Pattern.compile(oldURIPattern);
                        } catch (PatternSyntaxException e) {
                            return false;
                        }
                    }
                    return true;
                })
        );
    }

    @Override
    protected YamlVisitor<ExecutionContext> getVisitor() {
        return new UpdateGitURLVisitor();
    }

    private class UpdateGitURLVisitor extends YamlIsoVisitor<ExecutionContext> {
        private final JsonPathMatcher uriPath = new JsonPathMatcher("$.resources[?(@.type == 'git')].source.uri");

        @Override
        public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
            if (uriPath.matches(getCursor()) && entry.getValue() instanceof Yaml.Scalar) {
                if (oldURIPattern == null || Pattern.compile(oldURIPattern).matcher(((Yaml.Scalar) entry.getValue()).getValue()).matches()) {
                    return entry.withValue(((Yaml.Scalar) entry.getValue()).withValue(newURI));
                }
            }
            return super.visitMappingEntry(entry, ctx);
        }

    }


}
