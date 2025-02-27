/*
 * Copyright © 2015-2021 I.N.F.N.
 * Copyright © 2015-2020 Santer Reply S.p.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.reply.orchestrator.dto.cmdb;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum CloudServiceType {

  @JsonProperty("compute") COMPUTE,
  @JsonProperty("storage") STORAGE,
  @JsonProperty("block-storage") BLOCK_STORAGE,
  @JsonProperty("object-store") OBJECT_STORE,
  @JsonProperty("network") NETWORK,
  @JsonEnumDefaultValue @JsonProperty("unknown") UNKNOWN;

}
