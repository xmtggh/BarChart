package com.scoller.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.scoller.R;
import com.scoller.chart.ScrollBar;

public class ScrollBarActivity extends AppCompatActivity implements View.OnClickListener {

    private List<Float> verticalList;
    private List<String> horizontalList;
    private ScrollBar barChart;
    private Random random;

    private int count = 30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scroll);


        Button more = (Button) findViewById(R.id.more);
        Button small = (Button) findViewById(R.id.small);

        barChart = (ScrollBar) findViewById(R.id.barchart);


        more.setOnClickListener(this);
        small.setOnClickListener(this);
        verticalList = new ArrayList<>();
        horizontalList = new ArrayList<>();
        random = new Random();

        horizontalList.clear();
        verticalList.clear();
        for (int i = 0; i < count; i++) {
            horizontalList.add("" + i);
        }

        while (verticalList.size() < count) {
            int randomInt = random.nextInt(1000);
            verticalList.add((float) randomInt);
        }
        barChart.scollTo();
        barChart.setVerticalList(verticalList);


    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.more:
                horizontalList.clear();
                verticalList.clear();
                for (int i = 0; i < count; i++) {
                    horizontalList.add("" + i);
                }

                while (verticalList.size() < count) {
                    int randomInt = random.nextInt(1000);
                    verticalList.add((float) randomInt);
                }
                barChart.scollTo();
                barChart.setVerticalList(verticalList);

                break;
            case R.id.small:

                horizontalList.clear();
                verticalList.clear();
                for (int i = 0; i < 2; i++) {
                    horizontalList.add("" + i);
                }

                while (verticalList.size() < 2) {
                    int randomInt = random.nextInt(1000);
                    verticalList.add((float) randomInt);
                }

                barChart.setVerticalList(verticalList);
                break;
        }
    }
}
