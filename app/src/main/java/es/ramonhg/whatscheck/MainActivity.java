package es.ramonhg.whatscheck;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.tensorflow.lite.examples.textclassification.client.Result;
import org.tensorflow.lite.examples.textclassification.client.TextClassificationClient;

import es.ramonhg.whatscheck.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**This is the home page. This is where the magic happens.*/
public class MainActivity extends AppCompatActivity implements ActivityResultCallback<ActivityResult> {

    /**Launches the {@link CountrySelectorActivity} to pick an isd code.*/
    private final ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), this);
    private Country selectedCountry = null;
    private ActivityMainBinding binding;
    private static final String TAG = "WhatsCheck";
    private TextClassificationClient client;
    private Handler handler;
    private HorizontalBarChart mBarChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.WhatsAppDirect);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);

        // Text analysis
        client = new TextClassificationClient(getApplicationContext());
        handler = new Handler();

        //Dark status bar text during day.
        if (!getResources().getBoolean(R.bool.nightMode)) {
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), root);
            controller.setAppearanceLightStatusBars(true);
        }

        //means default country hasn't been loaded and neither has the user selected a country
        if (selectedCountry == null) getDefaultCountry();
        setClickListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        launcher.unregister();
    }

    @Override
    public void onActivityResult(ActivityResult result) {
        Country country = null;
        if (result.getResultCode() == RESULT_OK && result.getData() != null)
            country = result.getData().getParcelableExtra("country");
        if (country != null) countrySelected(country);
    }

    private void countrySelected(Country selectedCountry) {

        binding.flag.setImageResource(selectedCountry.flagCode);
        binding.isdCode.setText(selectedCountry.isdCode);
        this.selectedCountry = selectedCountry;
    }

    private void setClickListeners() {

        binding.sendBusiness.setOnClickListener(v -> classifyForSendButton((binding.text.getText() == null ? "" : binding.text.getText().toString()).replaceAll("[^a-zA-Z ]", ""), true));
        binding.send.setOnClickListener(v -> classifyForSendButton((binding.text.getText() == null ? "" : binding.text.getText().toString()).replaceAll("[^a-zA-Z ]", ""), false));
        // Analyze button
        binding.analyze.setOnClickListener(v -> classify((binding.text.getText() == null ? "" : binding.text.getText().toString()).replaceAll("[^a-zA-Z ]", "")));

        binding.italic.setOnClickListener(this::formatClicked);
        binding.strike.setOnClickListener(this::formatClicked);
        binding.bold.setOnClickListener(this::formatClicked);
        binding.mono.setOnClickListener(this::formatClicked);

        binding.toggleAbout.setOnClickListener(v
                -> new FragmentAbout().show(getSupportFragmentManager(), "ABOUT"));
    }

    private void getDefaultCountry() {

        int count = 0;
        String deviceCountryCode = Locale.getDefault().getCountry();

        //Search for the entry in list of countries whose iso code matches the device country's iso code.
        for (Country entry: Constants.countries) {
            if(deviceCountryCode.equalsIgnoreCase(entry.isoCode)) {
                countrySelected(entry);
                int finalCount = count;
                binding.selectCountry.setOnClickListener(v -> {
                    Intent intent = new Intent(this, CountrySelectorActivity.class);
                    intent.putExtra("default", entry);
                    intent.putExtra("position", finalCount);
                    launcher.launch(intent);
                });
            }
            ++count;
        }
    }

    /**
     * Using the {@link android.widget.TextView#onKeyDown(int, KeyEvent)} method, it is easy to insert the required characters.
     *
     * Bold is created via asterisk (*).
     * Italics are created using the underscore character.
     * For strikethrough text, you need the tilde symbol.
     * And for mono text, you need three grave characters.*/
    private void formatClicked(View v) {

        int id = v.getId();

        //bold is easy via the asterisk or star symbol.
        if (id == R.id.bold) {
            binding.text.onKeyDown(KeyEvent.KEYCODE_STAR, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_STAR));
            binding.text.onKeyDown(KeyEvent.KEYCODE_STAR, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_STAR));
            binding.text.setSelection(binding.text.getSelectionEnd()-1);
        }

        //The minus keycode with shift on creates underscore
        else if (id == R.id.italic) {
            binding.text.onKeyDown(KeyEvent.KEYCODE_MINUS, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MINUS,0, KeyEvent.META_SHIFT_ON));
            binding.text.onKeyDown(KeyEvent.KEYCODE_MINUS, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MINUS,0, KeyEvent.META_SHIFT_ON));
            binding.text.setSelection(binding.text.getSelectionEnd()-1);
        }

        //The grave character with shift pressed creates tilde.
        else if (id == R.id.strike) {
            binding.text.onKeyDown(KeyEvent.KEYCODE_GRAVE, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GRAVE,0, KeyEvent.META_SHIFT_ON));
            binding.text.onKeyDown(KeyEvent.KEYCODE_GRAVE, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GRAVE,0, KeyEvent.META_SHIFT_ON));
            binding.text.setSelection(binding.text.getSelectionEnd()-1);
        }

        //The grave character allows for mono text but metastate has to be zero.
        else if (id == R.id.mono) {
            binding.text.onKeyDown(KeyEvent.KEYCODE_GRAVE, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GRAVE,0, 0));
            binding.text.onKeyDown(KeyEvent.KEYCODE_GRAVE, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GRAVE,0, 0));
            binding.text.onKeyDown(KeyEvent.KEYCODE_GRAVE, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GRAVE,0, 0));
            binding.text.onKeyDown(KeyEvent.KEYCODE_GRAVE, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GRAVE,0, 0));
            binding.text.onKeyDown(KeyEvent.KEYCODE_GRAVE, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GRAVE,0, 0));
            binding.text.onKeyDown(KeyEvent.KEYCODE_GRAVE, new KeyEvent(System.currentTimeMillis(),System.currentTimeMillis(),KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_GRAVE,0, 0));
            binding.text.setSelection(binding.text.getSelectionEnd() - 3);
        }
        binding.text.requestFocus();
    }

    private void sendClicked(boolean business) {

        if (binding.numberEntry.length() == 0) {
            Toast.makeText(this, getString(R.string.enter_number_warn), Toast.LENGTH_SHORT).show();
            return;
        }

        String phone = binding.numberEntry.getText().toString();
        String code = selectedCountry.isdCode;
        String message = binding.text.getText() == null ? "" : binding.text.getText().toString();

        String total = "https://api.whatsapp.com/send?phone=" + code + phone + "&text=" + message;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(total));

        intent.setPackage(business ? "com.whatsapp.w4b" : "com.whatsapp");
        if (intent.resolveActivity(getPackageManager()) != null) startActivity(intent);
        else Toast.makeText(this, getString(R.string.not_installed), Toast.LENGTH_SHORT).show();
    }

    // Text analysis methods:
    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");
        handler.post(
                () -> {
                    client.load();
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v(TAG, "onStop");
        handler.post(
                () -> {
                    client.unload();
                });
    }
    /** Send input text to TextClassificationClient and get the classify messages. */
    private void classify(final String text) {
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.empty_message), Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Clasifying text: "+text);
        handler.post(
                () -> {
                    // Run text classification with TF Lite.
                    List<Result> results = client.classify(text);

                    // Show classification result on screen
                    showResult(text, results);
                });
    }

    /** The same functionality as classify method but for send buttons */
    private void classifyForSendButton(final String text, boolean business) {
        boolean classifyText = !text.isEmpty();
        if (classifyText)
            Log.d(TAG, "Clasifying text from button: "+text);
        handler.post(
                () -> {
                    if (classifyText) {
                        // Run text classification with TF Lite.
                        List<Result> results = client.classify(text);

                        String mostProbableSentiment = "";
                        float probability = 0;

                        for (Result r : results) {
                            if (r.getConfidence() > probability) {
                                mostProbableSentiment = r.getTitle();
                                probability = r.getConfidence();
                            }
                        }
                        probability = Math.round(100 * probability);

                        if (mostProbableSentiment.equals("Offensive")) {
                            Log.d(TAG, "The text was detected to be offensive");
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);

                            builder.setMessage("Your message was detected as offensive (" + (int) probability + " %).\n" +
                                            "Would you like to continue sending it?")
                                    .setTitle(R.string.alert_before_send_title);

                            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User clicked OK button. The message will be sent
                                    Log.d(TAG, "The user confirmed the request");
                                    sendClicked(business);
                                }
                            });

                            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User cancelled the dialog. Nothing more will be done
                                    Log.d(TAG, "The user canceled the request");
                                }
                            });

                            // Show the dialog
                            AlertDialog dialog = builder.create();
                            dialog.show();
                        } else {
                            // If the message is not detected as offensive, the message is directly sent over WhatsApp
                            sendClicked(business);
                        }
                    } else {
                        // The message is empty. We can directly generate the intent
                        sendClicked(business);
                    }
                });
    }

    public static void barchart(BarChart barChart, ArrayList<BarEntry> arrayList, final ArrayList<String> xAxisValues) {
        barChart.setDrawBarShadow(false);
        barChart.setFitBars(true);
        barChart.setDrawValueAboveBar(true);
        barChart.setMaxVisibleValueCount(25);
        barChart.setPinchZoom(true);

        barChart.setDrawGridBackground(true);
        BarDataSet barDataSet = new BarDataSet(arrayList, "Class");
        if (xAxisValues.get(0).contains("Non-Offensive")) {
            barDataSet.setColors(new int[]{Color.parseColor("#A5D6A7"), Color.parseColor("#EF9A9A")});
        } else {
            barDataSet.setColors(new int[]{Color.parseColor("#EF9A9A"), Color.parseColor("#A5D6A7")});
        }
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.5f);
        barData.setValueTextSize(0.5f);

        barChart.setBackgroundColor(Color.TRANSPARENT);
        barChart.setDrawGridBackground(false);
        barChart.animateY(2000);

        //To set components of x axis
        XAxis xAxis = barChart.getXAxis();
        xAxis.setTextSize(18f);
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xAxisValues));
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setEnabled(true);

        barChart.setData(barData);
    }

    /** Show classification result on the screen. */
    private void showResult(final String inputText, final List<Result> results) {
        // Run on UI thread as we'll updating our app UI
        runOnUiThread(
                () -> {
                    ArrayList<String> labels = new ArrayList<>();
                    ArrayList<String> BarLabel = new ArrayList<>();
                    ArrayList<Float> probability = new ArrayList<>();
                    ArrayList<BarEntry> barEntries = new ArrayList<>();

                    String textToShow = "Input: " + inputText + "\nOutput:\n";
                    for (int i = 0; i < results.size(); i++) {
                        Result result = results.get(i);
                        labels.add(result.getTitle());   // Extract labels
                        probability.add(result.getConfidence());  // Extract confidence
                    }

                    mBarChart = findViewById(R.id.chart);
                    mBarChart.setDrawBarShadow(false);
                    mBarChart.setDrawValueAboveBar(true);
                    mBarChart.getDescription().setEnabled(false);
                    mBarChart.setPinchZoom(false);
                    mBarChart.setDrawGridBackground(false);

                    XAxis xl = mBarChart.getXAxis();
                    xl.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xl.setDrawAxisLine(true);
                    xl.setDrawGridLines(false);
                    xl.setGranularity(1);

                    YAxis yl = mBarChart.getAxisLeft();
                    yl.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
                    yl.setDrawGridLines(false);
                    yl.setEnabled(false);
                    yl.setAxisMinimum(0f);

                    YAxis yr = mBarChart.getAxisRight();
                    yr.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
                    yr.setDrawGridLines(false);
                    yr.setAxisMinimum(0f);

                    // Preparing the array list of bar entries
                    for (int i = 0; i < probability.size(); i++) {
                        barEntries.add(new BarEntry(i, probability.get(i) *100));
                    }

                    for (int i = 0; i < labels.size(); i++) {
                        BarLabel.add(Math.round(probability.get(i) * 1000) / 10.0 + "% " + labels.get(i));
                    }

                    barchart(mBarChart,barEntries,BarLabel);
                });
    }

}