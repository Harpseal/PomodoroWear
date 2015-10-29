package io.harpseal.pomodorowear;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

/**
 * Created by Harpseal on 15/10/18.
 */
public final class AnimationUtil {

    private static void aniFade(final Activity mainActiviy, final View view,long duration)
    {
        final boolean isFadeIn = view.getVisibility() != View.VISIBLE;
        Animation fadeInAnimation = AnimationUtils.loadAnimation(mainActiviy, isFadeIn ? R.anim.fade_in_anim : R.anim.fade_out_anim);

        if (duration>0)
            fadeInAnimation.setDuration(duration);
        fadeInAnimation.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation arg0) {
                if (isFadeIn)
                    view.setVisibility(View.VISIBLE);
            }
            @Override
            public void onAnimationRepeat(Animation arg0) {
            }
            @Override
            public void onAnimationEnd(Animation arg0) {
                if (!isFadeIn)
                    view.setVisibility(View.INVISIBLE);

            }
        });
        view.startAnimation(fadeInAnimation);
    }

    public static void expand(final View v,long duration) {
        v.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);
        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                v.getLayoutParams().height = interpolatedTime == 1
                        ? ViewGroup.LayoutParams.WRAP_CONTENT
                        : (int)(targetHeight * interpolatedTime);
                v.requestLayout();
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };


        if (duration <=0)
            a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));// 1dp/ms
        else
            a.setDuration(duration);
        v.startAnimation(a);
    }

    public static void collapse(final View v,long duration) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation()
        {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if(interpolatedTime == 1){
                    v.setVisibility(View.GONE);
                }else{
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };


        if (duration <=0)
            a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));// 1dp/ms
        else
            a.setDuration(duration);
        v.startAnimation(a);
    }

}
