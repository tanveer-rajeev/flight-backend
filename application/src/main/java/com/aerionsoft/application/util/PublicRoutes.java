package com.aerionsoft.application.util;

import java.util.List;

public class PublicRoutes {

    public static final List<String> EXACT_MATCH_PATHS = List.of(
            "/api/public/**",
            "/api/admin/airport-airline/airport/list",
            "/api/admin/airport-airline/airport-search",
            "/api/admin/airport-airline/airline/list",
            "/api/admin/airport-airline/airline-search",
            "/api/auth/**",
            "/api/oauth/access-token"
    );


    public static final List<String> ANT_STYLE_PATTERNS = List.of(
            "/api/flight/common/group-tickets",
            "/api/flight/common/currencies",
            "/api/flight/common/common-currencies"

    );

}
