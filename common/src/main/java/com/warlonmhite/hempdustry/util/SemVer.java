package com.warlonmhite.hempdustry.util;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * A version number, read and ordered the way Fabric Loader's {@code SemanticVersion} reads and
 * orders it, so the update checker says the same thing on every loader.
 *
 * <p>That is SemVer with Fabric's leniencies, kept on purpose rather than tidied up: the core may
 * have any number of components and a missing one counts as 0 ({@code 1.0} is {@code 1.0.0}); an
 * empty pre-release ({@code 2.0.0-}) is a pre-release; numeric pre-release parts are ordered by
 * length first; and build metadata ({@code +1.21.11}) is never read. A game test holds this to
 * Fabric's own parser on every version the mod has published and every malformed case it knows.
 */
public final class SemVer implements Comparable<SemVer> {
    private static final Pattern DOT_SEPARATED_ID = Pattern.compile("|[-0-9A-Za-z]+(\\.[-0-9A-Za-z]+)*");
    private static final Pattern UNSIGNED_INTEGER = Pattern.compile("0|[1-9][0-9]*");

    private final int[] components;
    private final @Nullable String prerelease;

    private SemVer(int[] components, @Nullable String prerelease) {
        this.components = components;
        this.prerelease = prerelease;
    }

    /** {@code version} read as a version, or null when it is not one. */
    public static @Nullable SemVer parse(@Nullable String version) {
        if (version == null || version.isEmpty()) {
            return null;
        }
        int build = version.indexOf('+');
        if (build >= 0) {
            version = version.substring(0, build);
        }
        String prerelease = null;
        int dash = version.indexOf('-');
        if (dash >= 0) {
            prerelease = version.substring(dash + 1);
            version = version.substring(0, dash);
            if (!DOT_SEPARATED_ID.matcher(prerelease).matches()) {
                return null;
            }
        }
        if (version.endsWith(".") || version.startsWith(".")) {
            return null;
        }
        String[] parts = version.split("\\.");
        int[] components = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].trim().isEmpty()) {
                return null;
            }
            try {
                components[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
            if (components[i] < 0) {
                return null;
            }
        }
        return new SemVer(components, prerelease);
    }

    private int component(int index) {
        return index < components.length ? components[index] : 0;
    }

    @Override
    public int compareTo(SemVer other) {
        for (int i = 0; i < Math.max(components.length, other.components.length); i++) {
            int compare = Integer.compare(component(i), other.component(i));
            if (compare != 0) {
                return compare;
            }
        }
        if (prerelease == null || other.prerelease == null) {
            // A release sorts after any pre-release of the same core.
            return prerelease == null ? (other.prerelease == null ? 0 : 1) : -1;
        }
        String[] a = prerelease.isEmpty() ? new String[0] : prerelease.split("\\.");
        String[] b = other.prerelease.isEmpty() ? new String[0] : other.prerelease.split("\\.");
        for (int i = 0; i < a.length; i++) {
            if (i >= b.length) {
                return 1;
            }
            boolean numericA = UNSIGNED_INTEGER.matcher(a[i]).matches();
            boolean numericB = UNSIGNED_INTEGER.matcher(b[i]).matches();
            if (numericA != numericB) {
                return numericA ? -1 : 1;
            }
            if (numericA) {
                int compare = Integer.compare(a[i].length(), b[i].length());
                if (compare != 0) {
                    return compare;
                }
            }
            int compare = a[i].compareTo(b[i]);
            if (compare != 0) {
                return compare;
            }
        }
        return b.length > a.length ? -1 : 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SemVer other && compareTo(other) == 0;
    }

    @Override
    public int hashCode() {
        int last = components.length;
        while (last > 0 && components[last - 1] == 0) {
            last--;
        }
        return 31 * Arrays.hashCode(Arrays.copyOf(components, last)) + (prerelease == null ? 0 : prerelease.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < components.length; i++) {
            text.append(i == 0 ? "" : ".").append(components[i]);
        }
        return prerelease == null ? text.toString() : text.append('-').append(prerelease).toString();
    }
}
