/*
 * Copyright 2024 Google LLC
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

package aiplatform;

// [START aiplatform_sdk_text_image_embedding]

import com.google.cloud.aiplatform.v1beta1.EndpointName;
import com.google.cloud.aiplatform.v1beta1.PredictResponse;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceClient;
import com.google.cloud.aiplatform.v1beta1.PredictionServiceSettings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Value;
import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PredictImageFromTextSample {

  public static void main(String[] args) throws IOException {
    // TODO(developer): Replace this variable before running the sample.
    String project = "YOUR_PROJECT_ID";
    String textPrompt = "YOUR_TEXT_PROMPT";

    // Learn how to generate images from text prompts:
    // https://cloud.google.com/vertex-ai/docs/generative-ai/image/generate-images
    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("sampleCount", 1);

    String location = "us-central1";
    String publisher = "google";
    String model = "multimodalembedding@001";

    predictImageFromText(project, location, publisher, model, textPrompt, parameters);
  }

  // Generate images using text prompts
  public static void predictImageFromText(
      String project,
      String location,
      String publisher,
      String model,
      String textPrompt,
      Map<String, Object> parameters)
      throws IOException {
    final String endpoint = String.format("%s-aiplatform.googleapis.com:443", location);
    final PredictionServiceSettings predictionServiceSettings =
        PredictionServiceSettings.newBuilder().setEndpoint(endpoint).build();

    // Initialize client that will be used to send requests. This client only needs to be created
    // once, and can be reused for multiple requests.
    try (PredictionServiceClient predictionServiceClient =
        PredictionServiceClient.create(predictionServiceSettings)) {
      final EndpointName endpointName =
          EndpointName.ofProjectLocationPublisherModelName(project, location, publisher, model);

      JsonObject jsonInstance = new JsonObject();
      jsonInstance.addProperty("text", textPrompt);
      Value instanceValue = stringToValue(jsonInstance.toString());
      List<Value> instances = new ArrayList<>();
      instances.add(instanceValue);

      Gson gson = new Gson();
      String gsonString = gson.toJson(parameters);
      Value parameterValue = stringToValue(gsonString);

      PredictResponse predictResponse =
          predictionServiceClient.predict(endpointName, instances, parameterValue);
      System.out.println("Predict Response");
      System.out.println(predictResponse);
      for (Value prediction : predictResponse.getPredictionsList()) {
        System.out.format("\tPrediction: %s\n", prediction);
      }
    }
  }

  // Convert a Json string to a protobuf.Value
  static Value stringToValue(String value) throws InvalidProtocolBufferException {
    Value.Builder builder = Value.newBuilder();
    JsonFormat.parser().merge(value, builder);
    return builder.build();
  }
}
// [END aiplatform_sdk_text_image_embedding]
