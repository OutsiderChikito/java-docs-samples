/*
 * Copyright 2022 Google LLC
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
package bigtable.fraud.beam;

import bigtable.fraud.beam.utils.CustomerProfile;
import bigtable.fraud.beam.utils.TransactionDetails;
import bigtable.fraud.beam.utils.RowDetails;
import bigtable.fraud.beam.utils.WriteCBTHelper;
import com.google.cloud.bigtable.beam.CloudBigtableIO;
import com.google.cloud.bigtable.beam.CloudBigtableTableConfiguration;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.TypeDescriptor;

// Load customer profiles and history into Cloud Bigtable.
public final class LoadDataset {

  /**
   * Hiding the constructor.
   */
  private LoadDataset() {
  }

  /**
   * @param args the input arguments.
   */
  public static void main(final String[] args) throws
      IllegalArgumentException {
    LoadDatasetOptions options =
        PipelineOptionsFactory.fromArgs(args).withValidation()
            .as(LoadDatasetOptions.class);
    options.setJobName("load-customer-profiles-" + options.getRandomUUID());

    CloudBigtableTableConfiguration config =
        new CloudBigtableTableConfiguration.Builder()
            .withProjectId(options.getProjectID())
            .withInstanceId(options.getCBTInstanceId())
            .withTableId(options.getCBTTableId())
            .build();

    // Create a pipeline that reads the GCS customer profile csv file
    // and write it into CBT.
    Pipeline pProfiles = Pipeline.create(options);
    pProfiles
        .apply("ReadGCSFile",
            TextIO.read().from(options.getCustomerProfileInputFile()))
        .apply(
            MapElements.into(TypeDescriptor.of(RowDetails.class))
                .via(CustomerProfile::new))
        .apply("TransformParsingsToBigtable",
            ParDo.of(WriteCBTHelper.MUTATION_TRANSFORM))
        .apply(
            "WriteToBigtable",
            CloudBigtableIO.writeToTable(config));
    PipelineResult pProfilesRun = pProfiles.run();

    // Create a pipeline that reads the GCS history csv file and write
    // it into CBT
    options.setJobName("load-customer-historical-transactions-"
        + options.getRandomUUID());
    Pipeline pHistory = Pipeline.create(options);
    pHistory
        .apply("ReadGCSFile",
            TextIO.read().from(options.getHistoryInputFile()))
        .apply(
            MapElements.into(TypeDescriptor.of(RowDetails.class))
                .via(TransactionDetails::new))
        .apply("TransformParsingsToBigtable",
            ParDo.of(WriteCBTHelper.MUTATION_TRANSFORM))
        .apply(
            "WriteToBigtable",
            CloudBigtableIO.writeToTable(config));
    PipelineResult pHistoryRun = pHistory.run();

    pProfilesRun.waitUntilFinish();
    pHistoryRun.waitUntilFinish();
  }
}
