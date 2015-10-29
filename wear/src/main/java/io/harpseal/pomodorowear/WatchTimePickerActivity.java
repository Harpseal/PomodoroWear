package io.harpseal.pomodorowear;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class WatchTimePickerActivity extends Activity {

    private android.support.wearable.view.CircledImageView mBtnAdd;
    private android.support.wearable.view.CircledImageView mBtnSub;
    private android.support.wearable.view.CircledImageView mBtnOK;
    private android.support.wearable.view.CircledImageView mBtnCancel;

    private TextView mTextMinute;
    private TextView mTextTitle;

    private int mTimeIdx;
    private int mTimeSec;

    private int mBtnPre = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.time_picker_diaglog);

        mTimeIdx = getIntent().getIntExtra("TimeIdx",Integer.MAX_VALUE);
        mTimeSec = getIntent().getIntExtra("TimeSec",0);

        mTextTitle = (TextView) findViewById(R.id.time_picker_title);
        mTextMinute  = (TextView) findViewById(R.id.time_picker_time_text);

        String strTitle = getIntent().getExtras().getString("TimePickerName", "Time Picker");
        mTextTitle.setText(strTitle);
        mTextMinute.setText("" + mTimeSec/60);

        mBtnAdd = (android.support.wearable.view.CircledImageView) findViewById(R.id.time_picker_add_btn);
        mBtnSub = (android.support.wearable.view.CircledImageView) findViewById(R.id.time_picker_sub_btn);
        mBtnOK = (android.support.wearable.view.CircledImageView) findViewById(R.id.time_picker_ok_btn);
        mBtnCancel = (android.support.wearable.view.CircledImageView) findViewById(R.id.time_picker_cancel_btn);

        mBtnAdd.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {

                if (mBtnPre ==0)
                    mBtnPre = 6;
                if (mBtnPre<0)
                    mBtnPre = 1;

                if (mBtnPre>0 && mBtnPre<6)
                {
                    mBtnPre++;
                    mTimeSec+=60;
                }
                else {
                    if (mTimeSec%(5*60)!=0)
                    {
                        mTimeSec-= mTimeSec%(5*60);
                    }
                    mTimeSec += 5 * 60;

                }
                if (mTimeSec<0)
                    mTimeSec = 0;

                //mTimeSec+=5*60;
                mTextMinute.setText("" + mTimeSec/60);
            }
        });

        mBtnSub.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                if (mBtnPre ==0)
                    mBtnPre = -6;
                if (mBtnPre>0)
                    mBtnPre = -1;
                if (mBtnPre<0 && mBtnPre>-6)
                {
                    mBtnPre--;
                    mTimeSec-=60;
                }
                else {
                    if (mTimeSec%(5*60)!=0)
                    {
                        mTimeSec+= mTimeSec%(5*60);
                    }
                    mTimeSec -= 5 * 60;

                }
                if (mTimeSec<0)
                    mTimeSec = 0;


                mTextMinute.setText("" + mTimeSec/60);
            }
        });

        mBtnOK.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                android.content.Intent  resultIntent = new android.content.Intent();
                resultIntent.putExtra("TimeIdx", mTimeIdx);
                resultIntent.putExtra("TimeSec", mTimeSec);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });

        mBtnCancel.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                android.content.Intent  resultIntent = new android.content.Intent();
                resultIntent.putExtra("TimeIdx", mTimeIdx);
                resultIntent.putExtra("TimeSec", mTimeSec);
                setResult(Activity.RESULT_CANCELED, resultIntent);
                finish();
            }
        });
    }
}
