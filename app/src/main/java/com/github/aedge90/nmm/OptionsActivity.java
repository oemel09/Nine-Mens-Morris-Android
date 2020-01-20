/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.aedge90.nmm;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;

public class OptionsActivity extends android.support.v4.app.FragmentActivity{

    private static final String MILL_VARIANT = "MILL_VARIANT";
    private static final String WHO_STARTS = "WHO_STARTS";
    private SharedPreferences prefs;
    private Options options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.options_layout);
        this.options = getIntent().getParcelableExtra("own.projects.lemiroapp.Options");

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

        setMillVariant();
        setPlayerDifficultyFor(options.playerWhite);
        setPlayerDifficultyFor(options.playerBlack);
        setWhoStarts();

        Button buttonOK = (Button) findViewById(R.id.buttonOK);
        buttonOK.setOnClickListener(new OnClickListener(){

            @Override
            public void onClick(View v) {

                Intent i = new Intent();
                setResult(RESULT_OK, i.putExtra("own.projects.lemiroapp.Options", options));
                finish();

            }

        });

    }

    private void setMillVariant() {

        final ItemsAdapter items = new ItemsAdapter(this, R.layout.spinner_item);
        items.add(getResources().getString(R.string.five_mens_morris), R.drawable.gameboard5);
        items.add(getResources().getString(R.string.seven_mens_morris), R.drawable.gameboard7);
        items.add(getResources().getString(R.string.nine_mens_morris), R.drawable.gameboard9);

        Spinner millModeSpinner = (Spinner) findViewById(R.id.millModeSpinner);

        millModeSpinner.setAdapter(items);
        //set current selection to previously chosen value (or the initial value)
        int defaultMillVariant = Options.MillVariant.valueOf(options.millVariant.name()).ordinal();
        millModeSpinner.setSelection(prefs.getInt(MILL_VARIANT, defaultMillVariant));
        millModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                options.millVariant = Options.MillVariant.values()[pos];
                saveOption(MILL_VARIANT, Options.MillVariant.valueOf(options.millVariant.name()).ordinal());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}

        });

    }

    private void setPlayerDifficultyFor(final Player player) {

        final ArrayAdapter<String> items = new ArrayAdapter<String>(this, R.layout.spinner_item);
        items.add("Bot (" + getString(getResources().getIdentifier(Options.Difficulties.EASIER.name(), "string", getPackageName())) + ")");
        items.add("Bot (" + getString(getResources().getIdentifier(Options.Difficulties.EASY.name(), "string", getPackageName())) + ")");
        items.add("Bot (" + getString(getResources().getIdentifier(Options.Difficulties.NORMAL.name(), "string", getPackageName())) + ")");
        items.add("Bot (" + getString(getResources().getIdentifier(Options.Difficulties.ADVANCED.name(), "string", getPackageName())) + ")");
        items.add("Bot (" + getString(getResources().getIdentifier(Options.Difficulties.HARD.name(), "string", getPackageName())) + ")");
        items.add("Bot (" + getString(getResources().getIdentifier(Options.Difficulties.HARDER.name(), "string", getPackageName())) + ")");
        items.add("Bot (" + getString(getResources().getIdentifier(Options.Difficulties.HARDEST.name(), "string", getPackageName())) + ")");
        items.add(getResources().getString(R.string.human_player));

        Spinner spinner = null;
        if(player.getColor().equals(Options.Color.WHITE)) {
            Spinner playerWhiteSpinner = (Spinner) findViewById(R.id.playerWhiteSpinner);
            spinner = playerWhiteSpinner;
        }else {
            Spinner playerBlackSpinner = (Spinner) findViewById(R.id.playerBlackSpinner);
            spinner = playerBlackSpinner;
        }

        spinner.setAdapter(items);
        //set current selection to previously chosen value (or the initial value)
        final int defaultDifficulty = Options.Difficulties.values().length;
        if(player.getDifficulty() == null){
            spinner.setSelection(prefs.getInt(player.getColor().name(), defaultDifficulty));
        }else {
            spinner.setSelection(Options.Difficulties.valueOf(player.getDifficulty().name()).ordinal());
        }
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                if(pos == defaultDifficulty){
                    // no difficulty for human player. Set difficulty to null if human is reselected
                    player.setDifficulty(null);
                    saveOption(player.getColor().name(), defaultDifficulty);
                }else {
                    player.setDifficulty(Options.Difficulties.values()[pos]);
                    saveOption(player.getColor().name(), Options.Difficulties.valueOf(player.getDifficulty().name()).ordinal());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });

    }

    private void setWhoStarts() {

        final ArrayAdapter<String> items = new ArrayAdapter<String>(this, R.layout.spinner_item);
        items.add(getResources().getString(R.string.white));
        items.add(getResources().getString(R.string.black));

        Spinner whoStartsSpinner = (Spinner) findViewById(R.id.whoStartsSpinner);

        whoStartsSpinner.setAdapter(items);
        //set current selection to previously chosen value (or the initial value)
        final int defaultBeginner = Options.Color.valueOf(options.whoStarts.name()).ordinal();
        whoStartsSpinner.setSelection(prefs.getInt(WHO_STARTS, defaultBeginner));
        whoStartsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                options.whoStarts = Options.Color.values()[pos];
                saveOption(WHO_STARTS, Options.Color.valueOf(options.whoStarts.name()).ordinal());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

        });

    }

    private void saveOption(String key, int value) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private class ItemsAdapter extends ArrayAdapter<String> {

        private ArrayList<String> str;
        private Context context;
        private ArrayList<Integer> imageIds;
        private int screenWidth;

        public ItemsAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
            this.context = context;
            this.str = new ArrayList<String>();
            this.imageIds = new ArrayList<Integer>();
        }

        public void add(String object, int id) {
            super.add(object);
            str.add(object);
            imageIds.add(id);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            String item = str.get(position);

            LinearLayout layout = new LinearLayout(context);
            layout.setGravity(Gravity.CENTER_VERTICAL);
            layout.setOrientation(LinearLayout.HORIZONTAL);

            int padding = (int) getResources().getDimension(R.dimen.padding_spinner_item);
            layout.setPadding(padding, padding, padding, padding);

            LinearLayout.LayoutParams margin = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            margin.setMargins(0, 0, 0, 0);

            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            screenWidth = size.x;

            ImageView image = new ImageView(context);
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), imageIds.get(position));
            bmp = Bitmap.createScaledBitmap(bmp, screenWidth / 6, screenWidth / 6, false);
            image.setImageBitmap(bmp);
            layout.addView(image, margin);

            TextView mTitle = new TextView(context);
            mTitle.setText(item);
            //This is correct. Size in pixels is needed as output type
            mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_spinner_item));
            layout.addView(mTitle, margin);

            return layout;
        }
    }

}
