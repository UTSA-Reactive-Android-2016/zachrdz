package com.example.kbaldor.runningsum;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.ExpandedMenuView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

import nz.sodium.Cell;
import nz.sodium.CellLoop;
import nz.sodium.CellSink;
import nz.sodium.Handler;
import nz.sodium.Lambda2;
import nz.sodium.Lambda3;
import nz.sodium.Stream;
import nz.sodium.StreamSink;
import nz.sodium.Transaction;
import nz.sodium.Unit;

public class MainActivity extends AppCompatActivity {

    Random myRandom = new Random();

    StreamSink<Integer> nextRandom     = new StreamSink<>();

    StreamSink<Unit>    incrementEvent = new StreamSink<>();
    StreamSink<Unit>    decrementEvent = new StreamSink<>();

    CellSink<Integer> max = new CellSink<>(10);
    CellSink<Integer> min = new CellSink<>(3);

    CellSink<Integer> sumMax = new CellSink<>(Integer.MAX_VALUE);
    CellSink<Integer> sumMin = new CellSink<>(0);

    // I normally wouldn't put these here, but I wanted to provide a hint
    CellLoop<Integer>            N;
    CellLoop<ArrayList<Integer>> lastNValues;
    CellLoop<Integer>            sum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button minusButton = (Button)findViewById(R.id.Minus);
        Button plusButton  = (Button)findViewById(R.id.Plus);
        Button sendNumberButton = (Button)findViewById(R.id.SendNumber);

        final TextView NView = (TextView)findViewById(R.id.N);
        final TextView NumbersView = (TextView)findViewById(R.id.LastNumbers);
        final TextView sumView = (TextView)findViewById(R.id.Sum);


        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                decrementEvent.send(Unit.UNIT);
            }
        });
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                incrementEvent.send(Unit.UNIT);
            }
        });
        sendNumberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextRandom.send(myRandom.nextInt(9)+1);
            }
        });

        // You need a transaction for closing loops
        Transaction.runVoid(new Runnable() {
            @Override
            public void run() {

                // define your reactive network here
                N = new CellLoop<>();
                lastNValues = new CellLoop<>();
                sum = new CellLoop<>();

                /************************* N Increase and Decrease ****************************/
                Stream<Integer> incrementValues = incrementEvent.snapshot(N, new Lambda2<Unit, Integer, Integer>() {
                    @Override
                    public Integer apply(Unit unit, Integer old_value) {
                        return old_value+1;
                    }
                });

                Stream<Integer> decrementValues = decrementEvent.snapshot(N, new Lambda2<Unit, Integer, Integer>() {
                    @Override
                    public Integer apply(Unit unit, Integer old_value) {
                        return old_value-1;
                    }
                });

                Stream<Integer> changeValues = incrementValues.merge(decrementValues,
                        new Lambda2<Integer, Integer, Integer>() {
                            @Override
                            public Integer apply(Integer inc, Integer dec) {
                                return (inc+dec)/2;
                            }
                        });

                Cell<Integer> candidateValues = changeValues.hold(10);
                final ArrayList<Integer> loopN = new ArrayList<>();
                final ArrayList<Integer> currN = new ArrayList<>();

                Cell<Integer> legalValues =
                        candidateValues.lift(min, max, new Lambda3<Integer, Integer, Integer, Integer>() {
                            @Override
                            public Integer apply(Integer change, Integer min, Integer max) {
                                if(change > max) return max;
                                if(change < min) return min;
                                currN.clear();
                                currN.add(change);

                                if(loopN.size() >= currN.get(0)){
                                    for(int i = loopN.size() - 1; i > currN.get(0) - 1; i--){
                                        loopN.remove(i);
                                    }
                                    NumbersView.setText(loopN.toString().replace("[", "").replace("]", ""));
                                    Integer tot = 0;
                                    for (Integer i : loopN) {
                                        tot += i;
                                    }
                                    sumView.setText(tot.toString());
                                }
                                return change;
                            }
                        });

                N.loop(legalValues);

                /************************* sum Increase and Decrease ****************************/
                Stream<Integer> increaseSum = nextRandom.snapshot(sum, new Lambda2<Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer value, Integer old_value) {
                        if(loopN.size() < currN.get(0)) {
                            loopN.add(value);
                            NumbersView.setText(loopN.toString().replace("[", "").replace("]", ""));
                            int tot = 0;
                            for (Integer i : loopN) {
                                tot += i;
                            }
                            return tot;
                        }
                        return old_value;
                    }
                });

                Cell<Integer> sumCandidateValues = increaseSum.hold(0);

                Cell<Integer> sumLegalValues =
                        sumCandidateValues.lift(sumMin, sumMax, new Lambda3<Integer, Integer, Integer, Integer>() {
                            @Override
                            public Integer apply(Integer change, Integer min, Integer max) {
                                if(change > max) return max;
                                if(change < min) return min;
                                return change;
                            }
                        });

                sum.loop(sumLegalValues);
            }
        });

        N.listen(new Handler<Integer>() {
            @Override
            public void run(Integer value) {
                NView.setText(value.toString());
            }
        });

        sum.listen(new Handler<Integer>() {
            @Override
            public void run(Integer value) {
                sumView.setText(value.toString());
            }
        });

    }

    public void sendNumber(View view){
        nextRandom.send(myRandom.nextInt(9)+1);
    }

    public void decN(View view){
        decrementEvent.send(Unit.UNIT);
    }

    public void incN(View view){
        incrementEvent.send(Unit.UNIT);
    }

}
