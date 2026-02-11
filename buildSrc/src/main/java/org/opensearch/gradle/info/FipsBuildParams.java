/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.gradle.info;

import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

public class FipsBuildParams {

    private static final Logger LOGGER = Logger.getLogger(FipsBuildParams.class.getName());
    private static final Set<String> VALID_MODES = Set.of("FIPS-140-3", "any-supported", "none");

    @Deprecated
    public static final String FIPS_BUILD_PARAM_FOR_TESTS = "tests.fips.enabled";
    public static final String FIPS_BUILD_PARAM = "crypto.standard";
    public static final String DEFAULT_FIPS_MODE = "FIPS-140-3";

    private static String fipsMode;

    public static void init(Function<String, Object> fipsValue) {
        var fipsBuildParamForTests = Boolean.parseBoolean((String) fipsValue.apply(FIPS_BUILD_PARAM_FOR_TESTS));
        var fipsBuildParam = (String) fipsValue.apply(FIPS_BUILD_PARAM);

        if (fipsBuildParam != null && !VALID_MODES.contains(fipsBuildParam)) {
            LOGGER.warning("Unrecognized crypto.standard value '" + fipsBuildParam + "'. Valid values: " + VALID_MODES);
        }

        if (fipsBuildParamForTests) {
            fipsMode = DEFAULT_FIPS_MODE;
        } else if ("any-supported".equals(fipsBuildParam) || "none".equals(fipsBuildParam)) {
            fipsMode = "any-supported";
        } else {
            fipsMode = DEFAULT_FIPS_MODE;
        }
    }

    private FipsBuildParams() {}

    public static boolean isInFipsMode() {
        return DEFAULT_FIPS_MODE.equals(fipsMode);
    }

    public static String getFipsMode() {
        return fipsMode;
    }

}
