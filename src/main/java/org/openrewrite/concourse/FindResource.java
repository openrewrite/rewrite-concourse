package org.openrewrite.concourse;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.yaml.JsonPathMatcher;
import org.openrewrite.yaml.YamlVisitor;
import org.openrewrite.yaml.search.YamlSearchResult;
import org.openrewrite.yaml.tree.Yaml;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindResource extends Recipe {
    @Option(displayName = "Type",
        description = "Resource type",
        example = "git")
    String type;

    @Override
    public String getDisplayName() {
        return "Find resource";
    }

    @Override
    public String getDescription() {
        return "Find a Concourse resource by name";
    }

    @Override
    protected YamlVisitor<ExecutionContext> getVisitor() {
        JsonPathMatcher resource = new JsonPathMatcher("$.resources[*].type[@ == ' + " + type + "']");
        return new YamlVisitor<ExecutionContext>() {
            @Override
            public Yaml visitMappingEntry(Yaml.Mapping.Entry entry, ExecutionContext executionContext) {
                if(resource.matches(getCursor())) {
                    return entry.withMarkers(entry.getMarkers().addIfAbsent(new YamlSearchResult(Tree.randomId(), FindResource.this, null)));
                }
                return super.visitMappingEntry(entry, executionContext);
            }
        };
    }
}
