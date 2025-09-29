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

package it.reply.orchestrator.dto.ranker;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRankedCloudService {

  private float classification;

  private float regression;

  @JsonProperty("resource_exactness")
  private float resourceExactness;

  @JsonProperty("bandwidth_in")
  private float bandwidthIn;

  @JsonProperty("bandwidth_out")
  private float bandwidthOut;

  @JsonProperty("exact_flavors")
  private float exactFlavors;

  @JsonProperty("floating_ips_quota")
  private float floatingIpsQuota;

  @JsonProperty("floating_ips_requ")
  private float floatingIpsRequ;

  @JsonProperty("floating_ips_usage")
  private float floatingIpsUsage;

  @JsonProperty("gpus_requ")
  private float gpusRequ;

  @JsonProperty("n_instances_quota")
  private float instancesQuota;

  @JsonProperty("n_instances_requ")
  private float instancesRequ;

  @JsonProperty("n_instances_usage")
  private float instancesUsage;

  @JsonProperty("n_volumes_quota")
  private float volumesQuota;

  @JsonProperty("n_volumes_requ")
  private float volumesRequ;

  @JsonProperty("n_volumes_usage")
  private float volumesUsage;

  @JsonProperty("overbooking_cpu")
  private float overbookingCpu;

  @JsonProperty("overbooking_ram")
  private float overbookingRam;

  @NonNull
  @NotNull
  @JsonProperty("provider_name")
  private String provider;

  @JsonProperty("ram_gb_quota")
  private float ramGbQuota;

  @JsonProperty("ram_gb_requ")
  private float ramGbRequ;

  @JsonProperty("ram_gb_usage")
  private float ramGbUsage;

  @Nullable
  @JsonProperty("region_name")
  private String region;

  @JsonProperty("storage_gb_quota")
  private float storageGbQuota;

  @JsonProperty("storage_gb_requ")
  private float storageGbRequ;

  @JsonProperty("storage_gb_usage")
  private float storageGbUsage;

  @JsonProperty("test_failure_perc_1d")
  private float testFailurePerc1d;

  @JsonProperty("test_failure_perc_30d")
  private float testFailurePerc30d;

  @JsonProperty("test_failure_perc_7d")
  private float testFailurePerc7d;

  @JsonProperty("vcpus_quota")
  private float vcpusQuota;

  @JsonProperty("vcpus_requ")
  private float vcpusRequ;

  @JsonProperty("vcpus_usage")
  private float vcpusUsage;
}


