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
