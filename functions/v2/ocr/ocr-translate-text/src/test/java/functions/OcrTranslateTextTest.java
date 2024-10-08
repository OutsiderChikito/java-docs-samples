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

package functions;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.testing.TestLogHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import functions.eventpojos.Message;
import functions.eventpojos.MessagePublishedData;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OcrTranslateTextTest {
  private static final Logger logger = Logger.getLogger(OcrTranslateText.class.getName());

  private static final TestLogHandler LOG_HANDLER = new TestLogHandler();

  // Create custom serializer to handle timestamps in event data
  class DateSerializer implements JsonSerializer<OffsetDateTime> {
    @Override
    public JsonElement serialize(
        OffsetDateTime time, Type typeOfSrc, JsonSerializationContext context)
        throws JsonParseException {
      return new JsonPrimitive(time.toString());
    }
  }

  private final Gson gson =
      new GsonBuilder().registerTypeAdapter(OffsetDateTime.class, new DateSerializer()).create();

  private static OcrTranslateText sampleUnderTest;

  @BeforeClass
  public static void setUpClass() throws IOException {
    assertThat(System.getenv("RESULT_TOPIC")).isNotNull();
    sampleUnderTest = new OcrTranslateText();
    logger.addHandler(LOG_HANDLER);
  }

  @After
  public void afterTest() {
    LOG_HANDLER.clear();
  }

  @Test(expected = IllegalArgumentException.class)
  public void functionsOcrTranslate_shouldValidateParams()
      throws IOException, URISyntaxException, InterruptedException {
    MessagePublishedData data = new MessagePublishedData();
    Message message = new Message();
    message.setData(new String(Base64.getEncoder().encode("{}".getBytes())));
    data.setMessage(message);

    CloudEvent event =
        CloudEventBuilder.v1()
            .withId("000")
            .withType("google.cloud.pubsub.topic.v1.messagePublished")
            .withSource(new URI("curl-command"))
            .withData("application/json", gson.toJson(data).getBytes())
            .build();
    sampleUnderTest.accept(event);
  }

  @Test
  public void functionsOcrTranslate_shouldTranslateText()
      throws IOException, URISyntaxException, InterruptedException {
    String text = "Wake up human!";
    String filename = "wakeupcat.jpg";
    String lang = "es";

    JsonObject dataJson = new JsonObject();
    dataJson.addProperty("text", text);
    dataJson.addProperty("filename", filename);
    dataJson.addProperty("lang", lang);

    MessagePublishedData data = new MessagePublishedData();
    Message message = new Message();
    message.setData(new String(Base64.getEncoder().encode(gson.toJson(dataJson).getBytes())));
    data.setMessage(message);
    CloudEvent event =
        CloudEventBuilder.v1()
            .withId("000")
            .withType("google.cloud.pubsub.topic.v1.messagePublished")
            .withSource(new URI("curl-command"))
            .withData("application/json", gson.toJson(data).getBytes())
            .build();

    sampleUnderTest.accept(event);

    List<LogRecord> logs = LOG_HANDLER.getStoredLogRecords();
    assertThat(logs.get(1).getMessage()).contains("¡Despierta humano!");
    assertThat(logs.get(2).getMessage()).isEqualTo("Text translated to es");
  }
}
