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

package com.example.android.architecture.blueprints.todoapp.data;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource;
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksDbHelper;
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksLocalDataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.List;

import rx.functions.Action0;
import rx.functions.Action1;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Integration test for the {@link TasksDataSource}, which uses the {@link TasksDbHelper}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class TasksLocalDataSourceTest {

    private final static String TITLE = "title";

    private final static String TITLE2 = "title2";

    private final static String TITLE3 = "title3";

    private TasksLocalDataSource mLocalDataSource;

    @Before
    public void setup() {
        mLocalDataSource = TasksLocalDataSource.getInstance(
                InstrumentationRegistry.getTargetContext());
    }

    @After
    public void cleanUp() {
        mLocalDataSource.deleteAllTasks();
    }

    @Test
    public void testPreConditions() {
        assertNotNull(mLocalDataSource);
    }

    @Test
    public void saveTask_retrievesTask() {
        // Given a new task
        final Task newTask = new Task(TITLE, "");

        // When saved into the persistent repository
        mLocalDataSource.saveTask(newTask);

        // Then the task can be retrieved from the persistent repository
        mLocalDataSource.getTask(newTask.getId())
                .subscribe(new Action1<Task>() {
                    @Override
                    public void call(Task task) {
                        assertThat(task, is(newTask));
                    }
                });
    }

    @Test
    public void completeTask_retrievedTaskIsComplete() {
        // Given a new task in the persistent repository
        final Task newTask = new Task(TITLE, "");
        mLocalDataSource.saveTask(newTask);

        // When completed in the persistent repository
        mLocalDataSource.completeTask(newTask);

        // Then the task can be retrieved from the persistent repository and is complete
        mLocalDataSource.getTask(newTask.getId())
                .subscribe(new Action1<Task>() {
                    @Override
                    public void call(Task task) {
                        assertThat(task, is(newTask));
                        assertThat(task.isCompleted(), is(true));

                    }
                });
    }

    @Test
    public void activateTask_retrievedTaskIsActive() {

        // Given a new completed task in the persistent repository
        final Task newTask = new Task(TITLE, "");
        mLocalDataSource.saveTask(newTask);
        mLocalDataSource.completeTask(newTask);

        // When activated in the persistent repository
        mLocalDataSource.activateTask(newTask);

        // Then the task can be retrieved from the persistent repository and is active
        Action1 mockedAction = mock(Action1.class);
        mLocalDataSource.getTask(newTask.getId()).subscribe(mockedAction);

        verify(mockedAction).call(newTask);
        assertThat(newTask.isCompleted(), is(false));
    }

    @Test
    public void clearCompletedTask_taskNotRetrievable() {
        // Given 2 new completed tasks and 1 active task in the persistent repository
        final Task newTask1 = new Task(TITLE, "");
        mLocalDataSource.saveTask(newTask1);
        mLocalDataSource.completeTask(newTask1);
        final Task newTask2 = new Task(TITLE2, "");
        mLocalDataSource.saveTask(newTask2);
        mLocalDataSource.completeTask(newTask2);
        final Task newTask3 = new Task(TITLE3, "");
        mLocalDataSource.saveTask(newTask3);

        // When completed tasks are cleared in the repository
        mLocalDataSource.clearCompletedTasks();

        Action1 mockedOnNext = mock(Action1.class);
        Action0 mockedOnCompleted = mock(Action0.class);
        Action1 mockedOnError = mock(Action1.class);
        // Then the completed tasks cannot be retrieved and the active one can
        mLocalDataSource.getTask(newTask1.getId()).subscribe(mockedOnNext, mockedOnError, mockedOnCompleted);

        verify(mockedOnCompleted).call();
        verify(mockedOnNext, never()).call(newTask1);

        mockedOnNext = mock(Action1.class);
        mockedOnCompleted = mock(Action0.class);
        mockedOnError = mock(Action1.class);
        mLocalDataSource.getTask(newTask2.getId()).subscribe(mockedOnNext, mockedOnError, mockedOnCompleted);

        verify(mockedOnCompleted).call();
        verify(mockedOnNext, never()).call(newTask2);

        mockedOnNext = mock(Action1.class);
        mockedOnCompleted = mock(Action0.class);
        mockedOnError = mock(Action1.class);
        mLocalDataSource.getTask(newTask3.getId()).subscribe(mockedOnNext, mockedOnError, mockedOnCompleted);

        verify(mockedOnCompleted).call();
        verify(mockedOnNext, never()).call(newTask3);
    }

    @Test
    public void deleteAllTasks_emptyListOfRetrievedTask() {
        // Given a new task in the persistent repository and a mocked callback
        Task newTask = new Task(TITLE, "");
        mLocalDataSource.saveTask(newTask);

        // When all tasks are deleted
        mLocalDataSource.deleteAllTasks();

        Action1 mockedOnNext = mock(Action1.class);
        Action0 mockedOnCompleted = mock(Action0.class);
        Action1 mockedOnError = mock(Action1.class);
        // Then the retrieved tasks is an empty list
        mLocalDataSource.getTasks().subscribe(mockedOnNext, mockedOnError, mockedOnCompleted);

        verify(mockedOnCompleted).call();
        verify(mockedOnNext, never()).call(anyList());
        verify(mockedOnError, never()).call(anyList());
    }

    @Test
    public void getTasks_retrieveSavedTasks() {
        // Given 2 new tasks in the persistent repository
        final Task newTask1 = new Task(TITLE, "");
        mLocalDataSource.saveTask(newTask1);
        final Task newTask2 = new Task(TITLE, "");
        mLocalDataSource.saveTask(newTask2);

        // Then the tasks can be retrieved from the persistent repository
        mLocalDataSource.getTasks().subscribe(new Action1<List<Task>>() {
            @Override
            public void call(List<Task> tasks) {
                assertNotNull(tasks);
                assertTrue(tasks.size() >= 2);

                boolean newTask1IdFound = false;
                boolean newTask2IdFound = false;
                for (Task task : tasks) {
                    if (task.getId().equals(newTask1.getId())) {
                        newTask1IdFound = true;
                    }
                    if (task.getId().equals(newTask2.getId())) {
                        newTask2IdFound = true;
                    }
                }
                assertTrue(newTask1IdFound);
                assertTrue(newTask2IdFound);
            }
        });
    }
}
