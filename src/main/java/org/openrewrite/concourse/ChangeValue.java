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

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeValue extends ScanningRecipe<List<JsonPathMatcher>> {
    @Option(displayName = "Key path",
            description = "The key to match and replace.",
            example = "$.resources[?(@.type == 'git')].source.uri")
    String keyPath;

    @Option(displayName = "Old value",
            description = "Only change if the existing value matches.",
            required = false)
    @Nullable
    String oldValue;

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

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
    public List<JsonPathMatcher> getInitialValue(ExecutionContext ctx) {
        return new ArrayList<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(List<JsonPathMatcher> parametersToChange) {
        JsonPathMatcher keyPathMatcher = new JsonPathMatcher(keyPath);
        return new YamlVisitor<ExecutionContext>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                if (keyPathMatcher.matches(getCursor()) && entry.getValue() instanceof Yaml.Scalar &&
                        Parameters.isParameter(entry.getValue())) {
                    parametersToChange.add(Parameters.toJsonPath(entry.getValue()));
                }
                return super.visitMappingEntry(entry, ctx);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(List<JsonPathMatcher> parametersToChange) {
        JsonPathMatcher keyPathMatcher = new JsonPathMatcher(keyPath);
        Pattern oldValuePattern = oldValue == null ? null : Pattern.compile(oldValue);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable Yaml visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof Yaml.Documents) {
                    Yaml.Documents sourceFile = (Yaml.Documents) tree;
                    boolean matchesFile = fileMatcher == null;
                    if (!matchesFile) {
                        Path sourcePath = sourceFile.getSourcePath();
                        PathMatcher pathMatcher = sourcePath.getFileSystem().getPathMatcher("glob:" + fileMatcher);
                        matchesFile = pathMatcher.matches(sourcePath);
                    }
                    if (!matchesFile) {
                        return sourceFile;
                    }
                    Yaml t = super.visit(tree, ctx);
                    String asKeyPath = getCursor().pollMessage("asKeyPath");
                    while (asKeyPath != null) {
                        t = (Yaml) new org.openrewrite.yaml.ChangeValue(asKeyPath, newValue).getVisitor()
                                .visitNonNull(t, ctx);
                        asKeyPath = getCursor().pollMessage("asKeyPath");
                    }
                    return t;
                } else {
                    return super.visit(tree, ctx);
                }
            }

            @Override
            public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext ctx) {
                Yaml.Mapping.Entry e = super.visitMappingEntry(entry, ctx);
                for (JsonPathMatcher changeParam : parametersToChange) {
                    e = maybeReplaceValue(e, changeParam);
                }
                e = maybeReplaceValue(e, keyPathMatcher);
                return e;
            }

            private Yaml.Mapping.Entry maybeReplaceValue(Yaml.Mapping.Entry entry, JsonPathMatcher matcher) {
                if (matcher.matches(getCursor()) && entry.getValue() instanceof Yaml.Scalar) {
                    Yaml.Scalar scalar = (Yaml.Scalar) entry.getValue();
                    // do not replace the original value if it is parameterized.
                    if (Parameters.isParameter(scalar)) {
                        // if we're on a redirected parameter, recurse on the newly-parameterized value
                        if (!keyPathMatcher.matches(getCursor())) {
                            String value = scalar.getValue();
                            String asKeyPath = "$." + scalar.getValue().substring(2, value.length() - 2);
                            getCursor().getRoot().putMessage("asKeyPath", asKeyPath);
                        }
                        return entry;
                    }

                    if (oldValuePattern == null || oldValuePattern.matcher(scalar.getValue()).matches()) {
                        entry = entry.withValue(scalar.withValue(newValue));
                    }
                }
                return entry;
            }
        };
    }
}
