package com.estatetrader.apigw.core.features;

import com.estatetrader.apigw.core.extensions.Extension;
import com.estatetrader.apigw.core.models.ApiContext;
import com.estatetrader.apigw.core.phases.executing.request.RequestStarted;

import java.util.HashMap;
import java.util.Map;

public interface UtmInfoFeature {
    @Extension
    class RequestParserImpl implements RequestStarted.RequestParser {
        @Override
        public void parse(ApiContext context) {
            Map<String, String> utm = new HashMap<>(4);
            collectUtmPart("source", context, utm);
            collectUtmPart("medium", context, utm);
            collectUtmPart("campaign", context, utm);
            collectUtmPart("content", context, utm);
            collectUtmPart("term", context, utm);
            if (!utm.isEmpty()) {
                context.utm = utm;
            }
        }

        private void collectUtmPart(String key, ApiContext context, Map<String, String> result) {
            String v = getUtmPart(key, context);
            if (v != null && !v.isEmpty()) {
                result.put(key, v);
            }
        }

        private String getUtmPart(String key, ApiContext context) {
            String name = "utm_" + key;

            String pv = context.request.getParameter(name);
            if (pv != null) {
                return pv;
            }

            String cv = context.getCookie(name);
            if (cv != null) {
                return cv;
            }

            return context.getCookie("_" + name);
        }
    }
}
