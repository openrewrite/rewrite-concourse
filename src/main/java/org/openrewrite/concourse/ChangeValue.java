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
import org.openrewrite.marker.RecipeSearchResult;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.util.Collections;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeValue extends Recipe {
    private static final String CHANGE_CONCOURSE_PARAMETER = "changeConcourseParameter";

    @Option(displayName = "Key path",
            description = "The key to match and replace.",
            example = "$.resources[?(@.type == 'git')].source.uri")
    String keyPath;

    @Option(displayName = "Old value",
            description = "Only change if the existing value matches.",
            required = false)
    @Nullable
    String oldValue;

    @Option(displayName = "New value",
            description = "New value to replace the old value with.")
    String newValue;

    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/pipeline*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Change Concourse value";
    }

    @Override
    public String getDescription() {
        return "Change every value matching the key pattern.";
    }

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test(
                "oldValue",
                "oldValue must be a compilable regular expression",
                oldValue, ps -> {
                    if (oldValue != null) {
                        try {
                            Pattern.compile(oldValue);
                        } catch (PatternSyntaxException e) {
                            return false;
                        }
                    }
                    return true;
                })
        );
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new YamlVisitor<ExecutionContext>() {
                @Override
                public Yaml visitDocuments(Yaml.Documents documents, ExecutionContext executionContext) {
                    //noinspection ConstantConditions
                    return executionContext.getMessage(CHANGE_CONCOURSE_PARAMETER) != null ?
                            documents.withMarkers(documents.getMarkers().addIfAbsent(new RecipeSearchResult(Tree.randomId(), null, "could have parameter"))) :
                            (Yaml) new HasSourcePath<>(fileMatcher).visitNonNull(documents, executionContext);
                }
            };
        }
        return null;
    }

    @Override
    protected YamlVisitor<ExecutionContext> getVisitor() {
        JsonPathMatcher keyPathMatcher = new JsonPathMatcher(keyPath);
        Pattern oldValuePattern = oldValue == null ? null : Pattern.compile(oldValue);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                for (ChangeParameterValue changeParam : ctx.getMessage(CHANGE_CONCOURSE_PARAMETER, Collections.<ChangeParameterValue>emptySet())) {
                    e = maybeReplaceValue(e, changeParam.getParameter(), changeParam.getValue(), ctx);
                }
                e = maybeReplaceValue(e, keyPathMatcher, newValue, ctx);
                return e;
            }

            private Yaml.Mapping.Entry maybeReplaceValue(Yaml.Mapping.Entry e, JsonPathMatcher matcher, String newValue, ExecutionContext ctx) {
                if (matcher.matches(getCursor()) && e.getValue() instanceof Yaml.Scalar) {
                    if (Parameters.isParameter(e.getValue())) {
                        ctx.computeMessage(CHANGE_CONCOURSE_PARAMETER,
                                new ChangeParameterValue(Parameters.toJsonPath(e.getValue()), newValue),
                                emptySet(),
                                (changeParam, acc) -> Stream.concat(acc.stream(), Stream.of(changeParam)).collect(toSet()));
                    } else if (e.getValue() instanceof Yaml.Scalar) {
                        Yaml.Scalar value = (Yaml.Scalar) e.getValue();
                        if (oldValuePattern == null || oldValuePattern.matcher(value.getValue()).matches()) {
                            e = e.withValue(value.withValue(newValue));
                        }
                    }
                }
                return e;
            }
        };
    }

    @Value
    private static class ChangeParameterValue {
        JsonPathMatcher parameter;
        String value;
    }
}
