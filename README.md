
# WhatsCheck

## About this application
WhatsCheck allows the users to analyze WhatsApp messages offensiveness before sending them. It also warns the user with a popup if the message to be sent was detected as offensive.

This application was developed using the interface of [Direct to Chat](https://github.com/hafizmdyasir/Direct-To-Chat), an open source application developed by [Mohammad Yasir]("https://github.com/hafizmdyasir) which has the logic to send the messages over WhatsApp using itsAPI through HTTP requests.

For the analysis of the messages, an average word vector TensorFlow Lite model has been trained using an [english offensiveness dataset](https://github.com/NSTiwari/Custom-Text-Classification-on-Android-using-TF-Lite/) provided by [Nitin Tiwari](https://github.com/NSTiwari), as well as some of its examples to power up on-device inference of the model.

## Technical information
This application works on Android 7.1+ (API Level 25) devices. It may not be usable on devices with very small screens or very low DPI.

## TensorFlow Lite model train and evaluation
The file **TrainingAndEvaluationNotebook.ipynb** contains the Jupyter Notebook used to train and to evaluate the different model architectures proposed to be used with this application, including the final average word vector model deployed in the app, with a F1 macro score of 0.96.

## Screenshots

### Non-offensive message analyzed:
<img src="https://github.com/gsi-upm/offensive-tflite-android/raw/main/images/Non-Offensive.jpg" width=500></img>

### Offensive message analyzed:
<img src="https://github.com/gsi-upm/offensive-tflite-android/raw/main/images/Offensive.jpg" width=500></img>

### Popup alerting the user of an offensive message before sending it:
<img src="https://github.com/gsi-upm/offensive-tflite-android/raw/main/images/Popup.jpg" width=500></img>
