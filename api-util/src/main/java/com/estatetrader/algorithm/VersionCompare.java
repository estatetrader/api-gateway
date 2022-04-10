package com.estatetrader.algorithm;

import java.util.regex.Pattern;

public class VersionCompare {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");

    /**
     * compare two versions
     * @param version1 the first version
     * @param version2 the second version
     * @return return -1 if version1 &lt; version2 or 1 if version1 &gt; version2 otherwise 0
     */
    public static int compare(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        for (int i = 0;; i++) {
            if (i < parts1.length && i < parts2.length) {
                String part1 = parts1[i], part2 = parts2[i];
                if (NUMBER_PATTERN.matcher(part1).matches() && NUMBER_PATTERN.matcher(part2).matches()) {
                    int n1 = Integer.parseInt(part1), n2 = Integer.parseInt(part2);
                    if (n1 < n2) {
                        return -1;
                    } else if (n1 > n2) {
                        return 1;
                    }
                    // continue to compare next parts
                } else {
                    int ret = part1.compareToIgnoreCase(part2);
                    if (ret != 0) {
                        return ret;
                    }
                    // continue
                }
            } else if (i < parts1.length) {
                return 1;
            } else if (i < parts2.length) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
