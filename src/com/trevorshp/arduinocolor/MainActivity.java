package com.trevorshp.arduinocolor;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class MainActivity extends FragmentActivity implements OnTouchListener, OnSeekBarChangeListener {

    public final static String TAG = "AndroidColor";
	public ColorPickerView colorPicker;
	private TextView text1;
	private static final int blueStart = 100;
    private final int REQUEST_CONNECT_DEVICE = 1;
    private final int REQUEST_ENABLE_BT = 9;
    private BluetoothAdapter mBluetoothAdapter = null;

    private BluetoothSocket mmSocket = null;
    private BluetoothDevice mmDevice = null;
    private InputStream mmInStream = null;
    private OutputStream mmOutStream = null;

    private UUID myUUID = null;

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

        //Importante! Voce deve usar o UUID correto para o SPP - Serial Port Profile
        myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Estranho. Mas esse dispositivo nao suporta bluetooth :S", Toast.LENGTH_LONG).show();
        }   else    {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }   else    {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(this, DeviceListActivity.class);
                this.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);

            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)      {
        switch (requestCode)    {
            case REQUEST_CONNECT_DEVICE:
                if (resultCode== Activity.RESULT_OK)    {
                    mBluetoothAdapter.cancelDiscovery();

                    String endereco = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Log.i("ConexaoBluetooth", "Endereco do Bluetooth: " + endereco);
                    mmDevice = mBluetoothAdapter.getRemoteDevice(endereco);

                    //send the color to the serial device
                    if (mmDevice != null) {
                        try {

                            System.out.println(mmDevice.getName());
                            // Cria o socket utilizando o UUID
                            mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(myUUID);

                            // Conecta ao dispositivo escolhido
                            mmSocket.connect();

                            // Obtem os fluxos de entrada e saida que lidam com transmissões através do socket
                            mmInStream = mmSocket.getInputStream();
                            mmOutStream = mmSocket.getOutputStream();

                        }   catch (Exception ex)    {
                            Log.e("ConexaoBluetooth", ex.getLocalizedMessage(), ex);
                            Toast.makeText(this, "Ocorreu um erro no envio da mensagem!" + ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            break;
        }
    }
	
	 @Override
    protected void onPause() {
        super.onPause();
        //check if the device is already closed
        if (mmSocket != null) {
            try {
                mmSocket.close();
            } catch (IOException e) {
                //we couldn't close the device, but there's nothing we can do about it!
            }
            //remove the reference to the device
            mmSocket = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

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

        if (mmOutStream!=null) {
            try {
                // Saida:
                // Envio de uma mensagem pelo .write
                mmOutStream.write(dataToSend);

                Log.i("sendToArduino", String.valueOf(dataToSend));

                /*
                // Entrada:
                // bytes returnados da read()
                //int bytes;
                // buffer de memória para o fluxo
                //byte[] read = new byte[1024];

                // Continuar ouvindo o InputStream enquanto conectado
                // O loop principal é dedicado a leitura do InputStream

                while (true) {
                    try {
                        // Read from the InputStream
                        bytes = mmInStream.read(read);

                        String readMessage = new String(read);
                        Toast.makeText(this, readMessage, Toast.LENGTH_LONG).show();

                    } catch (IOException e) {
                        Toast.makeText(this, "Ocorreu um erro no recebimento da mensagem!", Toast.LENGTH_LONG).show();
                    }
                }
                */

            } catch (IOException e) {
                Toast.makeText(this, "Ocorreu um erro!", Toast.LENGTH_LONG).show();
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
