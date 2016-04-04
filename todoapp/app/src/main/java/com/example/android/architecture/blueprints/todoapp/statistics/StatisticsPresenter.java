/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.statistics;

import android.support.annotation.NonNull;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository;
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource;

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Listens to user actions from the UI ({@link StatisticsFragment}), retrieves the data and updates
 * the UI as required.
 */
public class StatisticsPresenter implements StatisticsContract.Presenter {

    private final TasksRepository mTasksRepository;

    private final StatisticsContract.View mStatisticsView;
    private int mCompletedTasks;
    private int mActiveTasks;

    public StatisticsPresenter(@NonNull TasksRepository tasksRepository,
                               @NonNull StatisticsContract.View statisticsView) {
        mTasksRepository = checkNotNull(tasksRepository, "tasksRepository cannot be null");
        mStatisticsView = checkNotNull(statisticsView, "StatisticsView cannot be null!");

        mStatisticsView.setPresenter(this);
    }

    @Override
    public void subscribe() {}

    @Override
    public void unsubscribe() { }

    private void loadStatistics() {
        mStatisticsView.setProgressIndicator(true);

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment(); // App is busy until further notice

        mTasksRepository.getTasks()
                .flatMap(new Func1<List<Task>, Observable<Task>>() {
                    @Override
                    public Observable<Task> call(List<Task> tasks) {
                        mCompletedTasks = 0;
                        mActiveTasks = 0;
                        return Observable.from(tasks);
                    }
                });
        // TODO Use count() ?
/*
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Task>() {
                    @Override
                    public void onCompleted() {
                        if (!mStatisticsView.isActive()) {
                            return;
                        }

                        mStatisticsView.setProgressIndicator(false);

                        mStatisticsView.showStatistics(mActiveTasks, mCompletedTasks);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Task task) {
                        if (task.isCompleted()) {
                            mCompletedTasks += 1;
                        } else {
                            mActiveTasks += 1;
                        }
                    }
                });
*/

    }

}
