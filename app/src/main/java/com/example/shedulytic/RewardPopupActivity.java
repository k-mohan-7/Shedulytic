package com.example.shedulytic;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

/**
 * Reward/Penalty Popup Activity
 * Shows animated XP gain/loss after task completion
 */
public class RewardPopupActivity extends AppCompatActivity {
    
    private CardView popupCard;
    private ImageView iconImage;
    private TextView titleText;
    private TextView xpText;
    private TextView messageText;
    private View confettiView1, confettiView2, confettiView3, confettiView4;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward_popup);
        
        // Initialize views
        popupCard = findViewById(R.id.popup_card);
        iconImage = findViewById(R.id.icon_image);
        titleText = findViewById(R.id.title_text);
        xpText = findViewById(R.id.xp_text);
        messageText = findViewById(R.id.message_text);
        confettiView1 = findViewById(R.id.confetti_1);
        confettiView2 = findViewById(R.id.confetti_2);
        confettiView3 = findViewById(R.id.confetti_3);
        confettiView4 = findViewById(R.id.confetti_4);
        
        // Get intent data
        double xpChange = getIntent().getDoubleExtra("xp_change", 0);
        String taskTitle = getIntent().getStringExtra("task_title");
        boolean isReward = getIntent().getBooleanExtra("is_reward", true);
        boolean wasExtended = getIntent().getBooleanExtra("was_extended", false);
        
        setupPopup(xpChange, taskTitle, isReward, wasExtended);
        startAnimations(isReward && !wasExtended);
        
        // Auto close after 3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(this::closePopup, 3000);
        
        // Allow tap to dismiss
        popupCard.setOnClickListener(v -> closePopup());
        findViewById(R.id.root_layout).setOnClickListener(v -> closePopup());
    }
    
    private void setupPopup(double xpChange, String taskTitle, boolean isReward, boolean wasExtended) {
        if (wasExtended) {
            // Extended task completed - no reward
            iconImage.setImageResource(R.drawable.ic_check);
            iconImage.setColorFilter(getResources().getColor(R.color.primary_color));
            titleText.setText("✅ Task Completed");
            titleText.setTextColor(getResources().getColor(R.color.primary_color));
            
            xpText.setText("No XP (Extended)");
            xpText.setTextColor(getResources().getColor(R.color.darkGray));
            
            messageText.setText("Task completed after extending. Complete on time next time for XP!");
            
            // Hide confetti for extended completion
            hideConfetti();
        } else if (isReward) {
            // Reward styling
            iconImage.setImageResource(R.drawable.ic_star);
            iconImage.setColorFilter(getResources().getColor(R.color.gold));
            titleText.setText("🎉 Task Completed!");
            titleText.setTextColor(getResources().getColor(R.color.darkGreen));
            
            String xpString = String.format("+%.1f XP", xpChange);
            xpText.setText(xpString);
            xpText.setTextColor(getResources().getColor(R.color.success_green));
            
            messageText.setText("Great job completing \"" + taskTitle + "\" on time!");
            
            // Show confetti
            showConfetti();
        } else {
            // Penalty styling
            iconImage.setImageResource(R.drawable.ic_close);
            iconImage.setColorFilter(getResources().getColor(R.color.error_red));
            titleText.setText("😔 Task Skipped");
            titleText.setTextColor(getResources().getColor(R.color.error_red));
            
            String xpString = String.format("%.1f XP", xpChange);
            xpText.setText(xpString);
            xpText.setTextColor(getResources().getColor(R.color.error_red));
            
            messageText.setText("You couldn't complete \"" + taskTitle + "\". Try again next time!");
            
            // Hide confetti for penalty
            hideConfetti();
        }
    }
    
    private void startAnimations(boolean isReward) {
        // Initial state
        popupCard.setScaleX(0f);
        popupCard.setScaleY(0f);
        popupCard.setAlpha(0f);
        
        // Pop-in animation for card
        AnimatorSet cardAnimation = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(popupCard, "scaleX", 0f, 1.1f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(popupCard, "scaleY", 0f, 1.1f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(popupCard, "alpha", 0f, 1f);
        
        scaleX.setDuration(500);
        scaleY.setDuration(500);
        alpha.setDuration(300);
        
        scaleX.setInterpolator(new OvershootInterpolator(1.5f));
        scaleY.setInterpolator(new OvershootInterpolator(1.5f));
        
        cardAnimation.playTogether(scaleX, scaleY, alpha);
        cardAnimation.start();
        
        // Icon bounce animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator iconBounce = ObjectAnimator.ofFloat(iconImage, "translationY", 0f, -30f, 0f);
            iconBounce.setDuration(600);
            iconBounce.setInterpolator(new BounceInterpolator());
            iconBounce.start();
            
            // Rotate icon
            ObjectAnimator iconRotate = ObjectAnimator.ofFloat(iconImage, "rotation", 0f, 360f);
            iconRotate.setDuration(800);
            iconRotate.start();
        }, 300);
        
        // XP text counting animation
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            double xpChange = getIntent().getDoubleExtra("xp_change", 0);
            animateXPCounter(xpChange, isReward);
        }, 500);
        
        // Pulse animation for XP
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            ObjectAnimator pulseX = ObjectAnimator.ofFloat(xpText, "scaleX", 1f, 1.3f, 1f);
            ObjectAnimator pulseY = ObjectAnimator.ofFloat(xpText, "scaleY", 1f, 1.3f, 1f);
            pulseX.setDuration(400);
            pulseY.setDuration(400);
            pulseX.setRepeatCount(2);
            pulseY.setRepeatCount(2);
            
            AnimatorSet pulseSet = new AnimatorSet();
            pulseSet.playTogether(pulseX, pulseY);
            pulseSet.start();
        }, 800);
    }
    
    private void animateXPCounter(double finalValue, boolean isReward) {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, (float) Math.abs(finalValue));
        animator.setDuration(800);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            String prefix = isReward ? "+" : "";
            xpText.setText(String.format("%s%.1f XP", prefix, isReward ? value : -value));
        });
        
        animator.start();
    }
    
    private void showConfetti() {
        if (confettiView1 == null) return;
        
        View[] confettiViews = {confettiView1, confettiView2, confettiView3, confettiView4};
        int[] colors = {0xFFFFD700, 0xFF4CAF50, 0xFF2196F3, 0xFFE91E63};
        
        for (int i = 0; i < confettiViews.length; i++) {
            View confetti = confettiViews[i];
            if (confetti == null) continue;
            
            confetti.setVisibility(View.VISIBLE);
            confetti.setBackgroundColor(colors[i % colors.length]);
            
            // Random starting position
            float startX = (float) (Math.random() * 400 - 200);
            float startY = -100f;
            
            confetti.setTranslationX(startX);
            confetti.setTranslationY(startY);
            
            // Fall animation
            ObjectAnimator fallY = ObjectAnimator.ofFloat(confetti, "translationY", startY, 800f);
            ObjectAnimator fallX = ObjectAnimator.ofFloat(confetti, "translationX", startX, startX + (float)(Math.random() * 200 - 100));
            ObjectAnimator rotate = ObjectAnimator.ofFloat(confetti, "rotation", 0f, 720f);
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(confetti, "alpha", 1f, 0f);
            
            fallY.setDuration(2000 + (int)(Math.random() * 500));
            fallX.setDuration(2000);
            rotate.setDuration(2000);
            fadeOut.setDuration(2000);
            fadeOut.setStartDelay(1500);
            
            AnimatorSet confettiSet = new AnimatorSet();
            confettiSet.playTogether(fallY, fallX, rotate, fadeOut);
            confettiSet.setStartDelay(i * 100);
            confettiSet.start();
        }
    }
    
    private void hideConfetti() {
        if (confettiView1 != null) confettiView1.setVisibility(View.GONE);
        if (confettiView2 != null) confettiView2.setVisibility(View.GONE);
        if (confettiView3 != null) confettiView3.setVisibility(View.GONE);
        if (confettiView4 != null) confettiView4.setVisibility(View.GONE);
    }
    
    private void closePopup() {
        // Fade out animation
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(popupCard, "alpha", 1f, 0f);
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(popupCard, "scaleX", 1f, 0.8f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(popupCard, "scaleY", 1f, 0.8f);
        
        AnimatorSet closeAnimation = new AnimatorSet();
        closeAnimation.playTogether(fadeOut, scaleX, scaleY);
        closeAnimation.setDuration(200);
        closeAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finish();
                overridePendingTransition(0, 0);
            }
        });
        closeAnimation.start();
    }
    
    @Override
    public void onBackPressed() {
        closePopup();
    }
}
