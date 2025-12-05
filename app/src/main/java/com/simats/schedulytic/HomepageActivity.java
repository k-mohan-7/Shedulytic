package com.simats.schedulytic;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.tabs.TabLayout;
import android.view.View;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class HomepageActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.homepage);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_layout), (v, insets) -> {  // Corrected ID to main_layout
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tabLayout = findViewById(R.id.tabLayout);
        fragmentManager = getSupportFragmentManager();

        // Create fragments for each tab
        final Fragment homeFragment = new HomeFragment();
        final Fragment taskFragment = new TaskFragment();
        final Fragment habitFragment = new HabitFragment();
        final Fragment profileFragment = new ProfileFragment();

        // Initial transaction to set the default fragment (Home)
        FragmentTransaction initialTransaction = fragmentManager.beginTransaction();
        initialTransaction.add(R.id.fragment_container, homeFragment);
        initialTransaction.commit();

        // Set up TabLayout listener for navigation
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                // Hide all fragments first
                if (homeFragment.isAdded()) transaction.hide(homeFragment);
                if (taskFragment.isAdded()) transaction.hide(taskFragment);
                if (habitFragment.isAdded()) transaction.hide(habitFragment);
                if (profileFragment.isAdded()) transaction.hide(profileFragment);

                // Show the selected fragment
                if (tab.getText().equals("Home")) {
                    if (!homeFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, homeFragment);
                    } else {
                        transaction.show(homeFragment);
                    }
                } else if (tab.getText().equals("Task")) {
                    if (!taskFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, taskFragment);
                    } else {
                        transaction.show(taskFragment);
                    }
                } else if (tab.getText().equals("Habit")) {
                    if (!habitFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, habitFragment);
                    } else {
                        transaction.show(habitFragment);
                    }
                } else if (tab.getText().equals("Profile")) {
                    if (!profileFragment.isAdded()) {
                        transaction.add(R.id.fragment_container, profileFragment);
                    } else {
                        transaction.show(profileFragment);
                    }
                }
                transaction.commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Optional
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Optional
            }
        });
    }
}
