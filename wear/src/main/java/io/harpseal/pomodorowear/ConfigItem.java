package io.harpseal.pomodorowear;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Created by Harpseal on 15/10/15.
 */
public class ConfigItem extends LinearLayout implements
        WearableListView.OnCenterProximityListener {
    /** The duration of the expand/shrink animation. */
    private static final int ANIMATION_DURATION_MS = 150;
    /** The ratio for the size of a circle in shrink state. */
    private static final float SHRINK_CIRCLE_RATIO = .75f;

    private static final float SHRINK_LABEL_ALPHA = .5f;
    private static final float EXPAND_LABEL_ALPHA = 1f;

    private final TextView mLabel;
    private final TextView mSubLabel;
    private final CircledImageView mHeaderImage;

    private float mExpandCircleRadius;
    private float mShrinkCircleRadius;

    private ObjectAnimator mExpandCircleAnimator;
    private ObjectAnimator mExpandCircleAlphaAnimator;
    private ObjectAnimator mExpandLabelAnimator;
    private ObjectAnimator mExpandSubLabelAnimator;
    private AnimatorSet mExpandAnimator;

    private ObjectAnimator mShrinkCircleAnimator;
    private ObjectAnimator mShrinkCircleAlphaAnimator;
    private ObjectAnimator mShrinkLabelAnimator;
    private ObjectAnimator mShrinkSubLabelAnimator;
    private AnimatorSet mShrinkAnimator;

    public ConfigItem(Context context) {
        super(context);
        View.inflate(context, R.layout.item_config, this);

        mLabel = (TextView) findViewById(R.id.item_label);
        mSubLabel = (TextView) findViewById(R.id.item_sublabel);
        mHeaderImage = (CircledImageView) findViewById(R.id.item_header_image);

        mSubLabel.setVisibility(GONE);
        init(mHeaderImage.getCircleRadius());

    }

    public ConfigItem(Context context,float radius) {
        super(context);
        View.inflate(context, R.layout.item_config, this);

        mLabel = (TextView) findViewById(R.id.item_label);
        mSubLabel = (TextView) findViewById(R.id.item_sublabel);
        mHeaderImage = (CircledImageView) findViewById(R.id.item_header_image);


        mSubLabel.setVisibility(GONE);
        mHeaderImage.setCircleRadius(radius);
        mHeaderImage.setCircleRadiusPressed(radius+2);
        mHeaderImage.setLayoutParams(new android.widget.LinearLayout.LayoutParams((int) radius * 2 + 4, LayoutParams.MATCH_PARENT));
        init(radius);

    }

    private void init(float radius)
    {
        mExpandCircleRadius = radius;
        mShrinkCircleRadius = mExpandCircleRadius * SHRINK_CIRCLE_RATIO;

        mShrinkCircleAnimator = ObjectAnimator.ofFloat(mHeaderImage, "circleRadius",
                mExpandCircleRadius, mShrinkCircleRadius);


        mShrinkCircleAlphaAnimator = ObjectAnimator.ofFloat(mHeaderImage, "alpha",
                EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);


        mShrinkLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
                EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);
        mShrinkSubLabelAnimator = ObjectAnimator.ofFloat(mSubLabel, "alpha",
                EXPAND_LABEL_ALPHA, SHRINK_LABEL_ALPHA);
        mShrinkAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
        mShrinkAnimator.playTogether(mShrinkCircleAnimator,mShrinkCircleAlphaAnimator, mShrinkLabelAnimator,mShrinkSubLabelAnimator);

        mExpandCircleAnimator = ObjectAnimator.ofFloat(mHeaderImage, "circleRadius",
                mShrinkCircleRadius, mExpandCircleRadius);
        mExpandCircleAlphaAnimator = ObjectAnimator.ofFloat(mHeaderImage, "alpha",
                SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);


        mExpandLabelAnimator = ObjectAnimator.ofFloat(mLabel, "alpha",
                SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);
        mExpandSubLabelAnimator = ObjectAnimator.ofFloat(mSubLabel, "alpha",
                SHRINK_LABEL_ALPHA, EXPAND_LABEL_ALPHA);
        mExpandAnimator = new AnimatorSet().setDuration(ANIMATION_DURATION_MS);
        mExpandAnimator.playTogether(mExpandCircleAnimator, mExpandCircleAlphaAnimator, mExpandLabelAnimator, mExpandSubLabelAnimator);
    }

    @Override
    public void onCenterPosition(boolean animate) {
        if (animate) {
            mShrinkAnimator.cancel();
            if (!mExpandAnimator.isRunning()) {
                mExpandCircleAnimator.setFloatValues(mHeaderImage.getCircleRadius(), mExpandCircleRadius);
                mExpandCircleAlphaAnimator.setFloatValues(mLabel.getAlpha(), EXPAND_LABEL_ALPHA);
                mExpandLabelAnimator.setFloatValues(mLabel.getAlpha(), EXPAND_LABEL_ALPHA);
                mExpandSubLabelAnimator.setFloatValues(mSubLabel.getAlpha(), EXPAND_LABEL_ALPHA);
                mExpandAnimator.start();
            }
        } else {
            mExpandAnimator.cancel();
            mHeaderImage.setCircleRadius(mExpandCircleRadius);
            mHeaderImage.setAlpha(EXPAND_LABEL_ALPHA);
            mLabel.setAlpha(EXPAND_LABEL_ALPHA);
            mSubLabel.setAlpha(EXPAND_LABEL_ALPHA);
        }
    }

    @Override
    public void onNonCenterPosition(boolean animate) {
        if (animate) {
            mExpandAnimator.cancel();
            if (!mShrinkAnimator.isRunning()) {
                mShrinkCircleAnimator.setFloatValues(mHeaderImage.getCircleRadius(), mShrinkCircleRadius);
                mShrinkCircleAlphaAnimator.setFloatValues(mLabel.getAlpha(), SHRINK_LABEL_ALPHA);
                mShrinkLabelAnimator.setFloatValues(mLabel.getAlpha(), SHRINK_LABEL_ALPHA);
                mShrinkSubLabelAnimator.setFloatValues(mSubLabel.getAlpha(), SHRINK_LABEL_ALPHA);
                mShrinkAnimator.start();
            }
        } else {
            mShrinkAnimator.cancel();
            mHeaderImage.setCircleRadius(mShrinkCircleRadius);
            mHeaderImage.setAlpha(SHRINK_LABEL_ALPHA);
            mLabel.setAlpha(SHRINK_LABEL_ALPHA);
            mSubLabel.setAlpha(SHRINK_LABEL_ALPHA);

        }
    }

    public void setRadius(float radius)
    {
        mHeaderImage.setCircleRadius(radius);
    }

    public float getRadius()
    {
        return mHeaderImage.getCircleRadius();
    }

    public void setColor(String colorName) {
        mHeaderImage.setCircleColor(Color.parseColor(colorName));
    }

    public void setColor(int color) {
        mHeaderImage.setCircleColor(color);
    }

    public void setCircleBorderColor(int color) {
        mHeaderImage.setCircleBorderColor(color);
    }

    public int getColor() {
        return mHeaderImage.getDefaultCircleColor();
    }

    public enum ImageType{
        IT_Calendar,
        IT_Tomato_white,
        IT_Tomato_color,
        IT_Alarm_clock,
        IT_Factory,
        IT_Time,
        IT_Work,
        IT_Reflash,
        IT_Memory

    }

    public void setImage(ImageType type)
    {
        switch (type)
        {
            case IT_Calendar:
                mHeaderImage.setImageResource(R.drawable.ic_event_note_white_48px);
                mHeaderImage.setCircleBorderColor(0);
                break;
            case IT_Tomato_white:
                mHeaderImage.setImageResource(R.drawable.icon_tomato_100);
                mHeaderImage.setCircleBorderColor(0);
                break;
            case IT_Tomato_color:
                mHeaderImage.setImageResource(R.drawable.icon_tomato_color_100);
                mHeaderImage.setCircleBorderColor(0);
                break;
            case IT_Alarm_clock:
                mHeaderImage.setImageResource(R.drawable.ic_alarm_white_48px);
                mHeaderImage.setCircleBorderColor(0);
                break;
            case IT_Factory:
                mHeaderImage.setImageResource(R.drawable.icon_factory_100);
                mHeaderImage.setCircleBorderColor(0);
                break;
            case IT_Time:
                mHeaderImage.setImageResource(R.drawable.icon_time_100);
                mHeaderImage.setCircleBorderColor(0);
                break;
            case IT_Work:
                mHeaderImage.setImageResource(R.drawable.icon_work_100);
                mHeaderImage.setCircleBorderColor(0);
                break;
            case IT_Reflash:
                mHeaderImage.setImageResource(R.drawable.ic_refresh_white_48px);
                mHeaderImage.setCircleBorderColor(0);
                break;
            case IT_Memory:
                mHeaderImage.setImageResource(R.drawable.ic_memory_48px);
                mHeaderImage.setCircleBorderColor(0);
                break;
        }
    }

    public void setItemName(String itemName){
        mLabel.setText(itemName);
        String strCal = getResources().getString(R.string.config_item_lv1_calendar);
        String strTomato = getResources().getString(R.string.config_item_lv1_tomato);
        String strTimer = getResources().getString(R.string.config_item_lv1_timer);
        String strEventQueue = getResources().getString(R.string.config_item_lv1_clear_event_queue);
        String strBuildInfo = getResources().getString(R.string.config_item_lv1_build_info);
        if (itemName.equals(strCal))
            setImage(ImageType.IT_Calendar);
        else if (itemName.equals(strTomato))
            setImage(ImageType.IT_Tomato_color);
        else if (itemName.equals(strTimer))
            setImage(ImageType.IT_Alarm_clock);
        else if (itemName.equals(strEventQueue))
            setImage(ImageType.IT_Reflash);
        else if (itemName.equals(strBuildInfo))
            setImage(ImageType.IT_Memory);
    }
    public String getLebelName() {return mLabel.getText().toString();}

    public void setSubLebelName(String itemName){
        mSubLabel.setText(itemName);
        mSubLabel.setVisibility(VISIBLE);
    }

    public String getSubLebelName(){
        return mSubLabel.getText().toString();
    }
}
