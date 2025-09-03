# TensorFlow Lite Model

This directory should contain the TensorFlow Lite model file for spam detection.

## Model File
- Name: `spam_model.tflite`
- Location: Place the model file in this directory

## Model Specifications
- Input: Text sequence of maximum length 100
- Output: Single float value between 0 and 1 (probability of spam)
- Architecture: Simple neural network for text classification
- Size: Should be optimized for mobile deployment (< 10MB)

## Training
The model should be trained on a dataset of spam and legitimate SMS messages. The training process should include:
1. Text preprocessing (tokenization, stop word removal)
2. Feature extraction (TF-IDF or word embeddings)
3. Model training (using TensorFlow)
4. Model conversion to TensorFlow Lite format

## Model Conversion
To convert a trained TensorFlow model to TensorFlow Lite format:
```python
import tensorflow as tf

# Load the trained model
model = tf.keras.models.load_model('path_to_trained_model')

# Convert to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save the model
with open('spam_model.tflite', 'wb') as f:
    f.write(tflite_model)
```

## Model Updates
The model can be updated by:
1. Training on new data
2. Converting to TensorFlow Lite format
3. Replacing the model file in this directory
4. Rebuilding the app 