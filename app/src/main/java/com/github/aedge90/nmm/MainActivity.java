package com.github.aedge90.nmm;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;

public class MainActivity extends Activity {

    private final static int RUN_GAME = 67;
    private final static int SET_OPTIONS = 100;
    
    private final MainActivity THIS = this;
    
    private Options options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        options = new Options();

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        startSetOptionsIntent();

    }

    private void startSetOptionsIntent () {
        Intent setOptionsIntent = new Intent(this, OptionsActivity.class);
        setOptionsIntent.putExtra("own.projects.lemiroapp.Options", options);
        startActivityForResult(setOptionsIntent, SET_OPTIONS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == SET_OPTIONS) {
            if (resultCode == RESULT_OK) {
                
                options = data.getParcelableExtra("own.projects.lemiroapp.Options");

                Intent gameModeIntent = new Intent(THIS, GameModeActivity.class);
                gameModeIntent.setExtrasClassLoader(Options.class.getClassLoader());
                gameModeIntent.putExtra("own.projects.lemiroapp.Options", options);
                startActivityForResult(gameModeIntent, RUN_GAME);
                
            }else {
                finish();
            }
        }else if(requestCode == RUN_GAME) {
            if(resultCode == RESULT_CANCELED){
                finish();
            }else{
                startSetOptionsIntent();
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
