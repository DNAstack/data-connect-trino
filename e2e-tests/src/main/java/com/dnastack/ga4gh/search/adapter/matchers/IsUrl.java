package com.dnastack.ga4gh.search.adapter.matchers;

import org.apache.commons.validator.routines.UrlValidator;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsUrl extends TypeSafeMatcher<String> {
    private static UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
    @Override
    public boolean matchesSafely(String url) {
        return urlValidator.isValid(url);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a URL");
    }

    public static Matcher isUrl() {
        return new IsUrl();
    }

    public static void setAllowLocalhost(boolean allowLocalhost) {
        urlValidator = allowLocalhost ? new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS) : UrlValidator.getInstance();
    }

}
