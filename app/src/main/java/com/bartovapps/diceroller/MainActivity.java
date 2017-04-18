package com.bartovapps.diceroller;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.databinding.ObservableInt;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.transition.Fade;
import android.support.transition.Transition;
import android.support.transition.TransitionManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;

import rx.Observable;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import utils.AudioHelper;
import utils.Utils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int DEFAULT_ANIMATION_DURATION = 1000;
    public static final int ANIMATION_REPEAT = 3;
    int mScore;
    Random random;
    int mDie1Score;
    int mDie2Score;
    int mDie3Score;

    List<Integer> mDiceScores;
    ImageView ivDie1;
    ImageView ivDie2;
    ImageView ivDie3;

    List<ImageView> imageViewList;

    TextView tvScore;

    ValueAnimator diceAnimation;
    ViewGroup rootView;

    Observable<List<Integer>> mDiceObservable;
    Observable<Integer> mScoreObservable;
    Subscription mSubscription;

    ObservableInt scoreObservable;

    AudioHelper audioHelper;
    int mScreenWidthPx;
    int mScreenWidthDp;

    float mMargin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        mScreenWidthPx = displayMetrics.widthPixels;
        mScreenWidthDp = Utils.pixelToDp(mScreenWidthPx, this);

        Log.i(TAG, "Screen Width: " + mScreenWidthPx + "px");
        Log.i(TAG, "Screen Width: " + mScreenWidthDp + "dp");

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        random = new Random();
        mDiceScores = new ArrayList<>();
        imageViewList = new ArrayList<>();

        audioHelper = new AudioHelper(this);
        audioHelper.prepareSoundPool();

        setViews();
        setAnimation();

        scoreObservable = new ObservableInt();
        scoreObservable.addOnPropertyChangedCallback(new android.databinding.Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(android.databinding.Observable observable, int i) {
                Log.i(TAG, "onPropertyChanged: " + scoreObservable.get());
            }
        });

        mDiceObservable = getObservable(mDiceScores);
        mScoreObservable = Observable.just(mScore);
    }


    private void setAnimation() {

        diceAnimation = ValueAnimator.ofFloat(0, 360);
        diceAnimation.setDuration(DEFAULT_ANIMATION_DURATION);
        diceAnimation.setRepeatCount(ANIMATION_REPEAT);
        diceAnimation.setInterpolator(new LinearInterpolator());

        diceAnimation.addUpdateListener(animation -> {
            float value1 = (float) animation.getAnimatedValue();
            ivDie1.setRotation(value1);
            ivDie1.setRotationX(value1);
            ivDie1.setRotationY(value1);
            ivDie2.setRotation(value1);
            ivDie2.setRotationX(value1);
            ivDie2.setRotationY(value1);
            ivDie3.setRotation(value1);
            ivDie3.setRotationX(value1);
            ivDie3.setRotationY(value1);
        });

        diceAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                audioHelper.stopDiceAudio();
                calculateScore();
                tvScore.setText(String.format(Locale.getDefault(), "Score: %d", scoreObservable.get()));
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                rollDice();
            }
        });

    }

    private void updateDiceImages() {
        for (int i = 0; i < imageViewList.size(); i++) {

            loadImageToView(imageViewList.get(i), "die_" + mDiceScores.get(i) + ".png");
        }


    }

    private void setViews() {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            audioHelper.playRollDice();
            diceAnimation.start();
            rollDice();
        });


        ivDie1 = (ImageView) findViewById(R.id.die1Image);
        loadImageToView(ivDie1, "die_1.png");
        ivDie2 = (ImageView) findViewById(R.id.die2Image);
        loadImageToView(ivDie2, "die_1.png");
        ivDie3 = (ImageView) findViewById(R.id.die3Image);
        loadImageToView(ivDie3, "die_1.png");

        imageViewList.add(ivDie1);
        imageViewList.add(ivDie2);
        imageViewList.add(ivDie3);

        mMargin = getResources().getDimension(R.dimen.die_layout_margin);
        Log.i(TAG, "setViews margin: " + mMargin + "dp");
        int marginPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, mMargin, getResources().getDisplayMetrics());
        Log.i(TAG, "setViews, marginPx: " + marginPx);
        float totalMargin = 2*(marginPx * (imageViewList.size()+1));
        Log.i(TAG, "setViews, totalMargin: " + totalMargin);

        float dieSizePx = (mScreenWidthPx - totalMargin)/3;
        Log.i(TAG, "setViews, dieSizePx: " + dieSizePx);

        for(ImageView iv: imageViewList){

            iv.getLayoutParams().height = (int)dieSizePx;
            iv.getLayoutParams().width = (int)dieSizePx;

        }

        tvScore = (TextView) findViewById(R.id.tvScore);

        rootView = (ViewGroup) findViewById(R.id.rootView);
        Transition mFadeTransition = new Fade();
        TransitionManager.beginDelayedTransition(rootView, mFadeTransition);

    }

    private void rollDice() {
        mDie1Score = random.nextInt(6) + 1;
        mDie2Score = random.nextInt(6) + 1;
        mDie3Score = random.nextInt(6) + 1;

        mDiceScores.clear();
        mDiceScores.add(mDie1Score);
        mDiceScores.add(mDie2Score);
        mDiceScores.add(mDie3Score);

        updateDiceImages();

    }

    private void calculateScore() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ArrayList<Integer> repeats = mDiceScores.stream().filter(i -> Collections.frequency(mDiceScores, i) > 1)
                    .collect(Collectors.toCollection(ArrayList::new));
            Log.i(TAG, "rollDice: got " + repeats.size() + " doubles");

            switch (repeats.size()) {
                case 2:
                    scoreObservable.set(scoreObservable.get() + 50);
                    mScore += 50;
                    break;
                case 3:
                    scoreObservable.set(scoreObservable.get() + repeats.get(0) * 100);
                    mScore += 100 * repeats.get(0);
                    break;
                default:
                    break;
            }
        } else {
            if (mDie1Score == mDie2Score && mDie1Score == mDie3Score) {
                scoreObservable.set(scoreObservable.get() + mDie1Score * 100);
            } else if (mDie1Score == mDie2Score || mDie2Score == mDie3Score || mDie1Score == mDie3Score) {
                scoreObservable.set(scoreObservable.get() + 50);
            }
        }

    }


    void loadImageToView(ImageView v, String image) {
        Glide.with(this).load("file:///android_asset/" + image).
                diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(v);

    }


    Observable<List<Integer>> getObservable(List<Integer> list) {
        return Observable.just(list);
    }

}
