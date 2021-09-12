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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        JsonPathMatcher keyPathMatcher = new JsonPathMatcher(keyPath);
        Pattern oldValuePattern = oldValue == null ? null : Pattern.compile(oldValue);
        List<JsonPathMatcher> parametersToChange = new ArrayList<>();

        YamlVisitor<ExecutionContext> findParametersToChange = new YamlVisitor<ExecutionContext>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (keyPathMatcher.matches(getCursor()) && entry.getValue() instanceof Yaml.Scalar &&
                        Parameters.isParameter(entry.getValue())) {
                    parametersToChange.add(Parameters.toJsonPath(entry.getValue()));
                }
                return super.visitMappingEntry(entry, ctx);
            }
        };

        for (SourceFile sourceFile : before) {
            findParametersToChange.visit(sourceFile, ctx);
        }

        return ListUtils.map(before, sourceFile -> {
            if (!(sourceFile instanceof Yaml.Documents)) {
                return sourceFile;
            }

            boolean matchesFile = fileMatcher == null;
            if (!matchesFile) {
                Path sourcePath = sourceFile.getSourcePath();
                PathMatcher pathMatcher = sourcePath.getFileSystem().getPathMatcher("glob:" + fileMatcher);
                matchesFile = pathMatcher.matches(sourcePath);
            }

            if (matchesFile) {
                return (SourceFile) new YamlIsoVisitor<ExecutionContext>() {
                    @Override
                    public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                        Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                        for (JsonPathMatcher changeParam : parametersToChange) {
                            e = maybeReplaceValue(e, changeParam);
                        }
                        e = maybeReplaceValue(e, keyPathMatcher);
                        return e;
                    }

                    private Yaml.Mapping.Entry maybeReplaceValue(Yaml.Mapping.Entry e, JsonPathMatcher matcher) {
                        if (matcher.matches(getCursor()) && e.getValue() instanceof Yaml.Scalar) {
                            if (e.getValue() instanceof Yaml.Scalar) {
                                Yaml.Scalar value = (Yaml.Scalar) e.getValue();
                                if (oldValuePattern == null || oldValuePattern.matcher(value.getValue()).matches()) {
                                    e = e.withValue(value.withValue(newValue));
                                }
                            }
                        }
                        return e;
                    }
                }.visitNonNull(sourceFile, ctx);
            }

            return sourceFile;
        });
    }
}
