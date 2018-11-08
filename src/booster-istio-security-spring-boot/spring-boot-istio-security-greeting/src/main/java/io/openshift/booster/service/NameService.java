/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.openshift.booster.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NameService {

    private final RestTemplate restTemplate;

    private final NameServiceProperties properties;

    public NameService(RestTemplate restTemplate, NameServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public String getName() {
        return restTemplate.getForObject(properties.getUrl(), String.class);
    }

}