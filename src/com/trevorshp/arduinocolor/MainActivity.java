package com.trevorshp.arduinocolor;


import java.io.IOException;

import android.content.Context;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;


 
public class MainActivity extends FragmentActivity implements OnTouchListener, OnSeekBarChangeListener {
	public final static String TAG = "AndroidColor";
	public ColorPickerView colorPicker;
	private TextView text1;
	private static final int blueStart = 100;
	
	private UsbManager usbManager;
	private UsbSerialDriver device;
  
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LinearLayout layout = (LinearLayout) findViewById(R.id.color_picker_layout);
        final int width = layout.getWidth();
        //get the display density
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        colorPicker = new ColorPickerView(this,blueStart,metrics.densityDpi);
        layout.setMinimumHeight(width);
        layout.addView(colorPicker);
        layout.setOnTouchListener(this);
        
        text1 = (TextView) findViewById(R.id.result1_textview);
	    text1.setText("Tap a color!");
                
		SeekBar seek = (SeekBar) findViewById(R.id.seekBar1);
		seek.setProgress(blueStart);
		seek.setMax(255);
		seek.setOnSeekBarChangeListener(this);
		
		// Get UsbManager from Android.
		usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    } 
	
	 @Override
    protected void onPause() {
        super.onPause();
        //check if the device is already closed
        if (device != null) {
            try {
                device.close();
            } catch (IOException e) {
                //we couldn't close the device, but there's nothing we can do about it!
            }
            //remove the reference to the device
            device = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //get a USB to Serial device object
        device = UsbSerialProber.acquire(usbManager);
        if (device == null) {
        	//there is no device connected!
            Log.d(TAG, "No USB serial device connected.");
        } else {
            try {
            	//open the device
                device.open();
                //set the communication speed
                device.setBaudRate(115200); //make sure this matches your device's setting!
            } catch (IOException err) {
                Log.e(TAG, "Error setting up USB device: " + err.getMessage(), err);
                try {
                	//something failed, so try closing the device
                	device.close();
                } catch (IOException err2) {
                    //couldn't close, but there's nothing more to do!
                }
                device = null;
                return;
            }
        }
    }
	    
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	// sends color data to a Serial device as {R, G, B, 0x0A}
	private void sendToArduino(int color){
		byte[] dataToSend = {(byte)Color.red(color),(byte)Color.green(color),(byte)Color.blue(color), 0x0A};
		//remove spurious line endings from color bytes so the serial device doesn't get confused
		for (int i=0; i<dataToSend.length-1; i++){
			if (dataToSend[i] == 0x0A){
				dataToSend[i] = 0x0B;
			}
		}
		//send the color to the serial device
		if (device != null){
			try{
				device.write(dataToSend, 500);
			}
			catch (IOException e){
				Log.e(TAG, "couldn't write color bytes to serial device");
			}
		}
	}
	
    // sets the text boxes' text and color background.
	private void updateTextAreas(int col) {
		int[] colBits = {Color.red(col),Color.green(col),Color.blue(col)};
		//set the text & color backgrounds
		text1.setText("You picked #" + String.format("%02X", Color.red(col)) + String.format("%02X", Color.green(col)) + String.format("%02X", Color.blue(col)));
		text1.setBackgroundColor(col);
		
		if (isDarkColor(colBits)) {
			text1.setTextColor(Color.WHITE);
		} else {
			text1.setTextColor(Color.BLACK);
		}
	}
	
	// returns true if the color is dark.  useful for picking a font color.
    public boolean isDarkColor(int[] color) {
    	if (color[0]*.3 + color[1]*.59 + color[2]*.11 > 150) return false;
    	return true;
    }
    
    @Override
    //called when the user touches the color palette
	public boolean onTouch(View view, MotionEvent event) {
    	int color = 0;
		color = colorPicker.getColor(event.getX(),event.getY(),true);
		colorPicker.invalidate();
		//re-draw the selected colors text
		updateTextAreas(color);
		//send data to arduino
		sendToArduino(color);
		return true;
	}
	
    @Override
	public void onProgressChanged(SeekBar seek, int progress, boolean fromUser) {
		int amt = seek.getProgress();
		int col = colorPicker.updateShade(amt);
		updateTextAreas(col);
		sendToArduino(col);
		colorPicker.invalidate();
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		
	}
	
	// generate a random hex color & display it
	public void randomColor(View v) {
    	int z = (int) (Math.random()*255);
    	int x = (int) (Math.random()*255);
    	int y = (int) (Math.random()*255);
    	colorPicker.setColor(x,y,z);
		SeekBar seek = (SeekBar) findViewById(R.id.seekBar1);
		seek.setProgress(z);
	}
}
