/*
 * Copyright 2020 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.textclassification.client;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.task.text.nlclassifier.NLClassifier;
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier;
import org.tensorflow.lite.task.core.BaseOptions;

/** Load TfLite model and provide predictions with task api. */
public class TextClassificationClient {
  private static final String TAG = "TaskApi";
  private static final String MODEL_PATH = "model_average_word_vec_en.tflite"; //"model_spanish_dataset_simetrico(average_word_vec 300 epochs).tflite";//"model_spanish_mobile_bert(hasta 256 palabras con tabs)_17-02.tflite";
  private static final String currentModel = "word_vec"; // word_vec o mobile_bert

  private final Context context;

  NLClassifier classifier;
  BertNLClassifier bertClassifier;

  public TextClassificationClient(Context context) {
    this.context = context;
  }

  public void load() {
    try {
      Log.w(TAG, "Leyendo archivo "+MODEL_PATH);
      if (currentModel.equals("mobile_bert")) {
        BertNLClassifier.BertNLClassifierOptions options =
                BertNLClassifier.BertNLClassifierOptions.builder()
                        //.setMaxSeqLen(256)
                        .build();
        bertClassifier = BertNLClassifier.createFromFileAndOptions(context, MODEL_PATH, options);
      } else if (currentModel.equals("word_vec"))
        classifier = NLClassifier.createFromFile(context, MODEL_PATH);
      Log.w(TAG, "Archivo "+MODEL_PATH+" leído correctamente");
    } catch (IOException e) {
      Log.e(TAG, e.getMessage());
    }
  }

  public void unload() {
    if (currentModel.equals("mobile_bert")) {
      bertClassifier.close();
      bertClassifier = null;
    } else if (currentModel.equals("word_vec")) {
      classifier.close();
      classifier = null;
    }
  }

  public List<Result> classify(String text) {
    List<Category> apiResults = new ArrayList<>();
    if (currentModel.equals("mobile_bert")) {
      apiResults = bertClassifier.classify(text);
    } else if (currentModel.equals("word_vec")) {
      apiResults = classifier.classify(text);
    }
    List<Result> results = new ArrayList<>(apiResults.size());
    for (int i = 0; i < apiResults.size(); i++) {
      Category category = apiResults.get(i);
      results.add(new Result("" + i, category.getLabel(), category.getScore()));
    }
    Collections.sort(results);
    return results;
  }
}
