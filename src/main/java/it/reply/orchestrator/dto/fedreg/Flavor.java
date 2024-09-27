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

package it.reply.orchestrator.dto.fedreg;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Flavor {

  @JsonProperty("description")
  private String description;

  @JsonProperty("name")
  @NotNull
  private String name;

  @JsonProperty("uuid")
  @NotNull
  private String uuid;

  @JsonProperty("disk")
  private Integer disk;

  @JsonProperty("is_public")
  private Boolean isPublic;

  @JsonProperty("ram")
  private Integer ram;

  @JsonProperty("vcpus")
  private Integer vcpus;

  @JsonProperty("swap")
  private Integer swap;

  @JsonProperty("ephemeral")
  private Integer ephemeral;

  @JsonProperty("infiniband")
  private Boolean infiniband;

  @JsonProperty("gpus")
  private Integer gpus;

  @JsonProperty("gpu_model")
  private String gpuModel;

  @JsonProperty("gpu_vendor")
  private String gpuVendor;

  @JsonProperty("local_storage")
  private String localStorage;

  @JsonProperty("uid")
  @NotNull
  private String uid;

}
