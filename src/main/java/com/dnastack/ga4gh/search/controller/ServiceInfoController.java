package com.dnastack.ga4gh.search.controller;

import com.dnastack.ga4gh.search.ServiceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implementation of service-info from GA4GH specification
 * @see <a href="https://github.com/ga4gh-discovery/service-info">GA4GH service-info specification</a>
 */
@RestController
public class ServiceInfoController {

    private ServiceInfo serviceInfo;

    @Autowired
    public ServiceInfoController(ServiceInfo serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    @GetMapping(value = "/service-info", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity getServiceInfo() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(serviceInfo);
    }

}
